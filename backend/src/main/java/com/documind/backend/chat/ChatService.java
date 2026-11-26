package com.documind.backend.chat;

import com.documind.backend.audit.AuditLogService;
import com.documind.backend.chat.dto.*;
import com.documind.backend.common.exception.ResourceNotFoundException;
import com.documind.backend.document.AiServiceClient;
import com.documind.backend.document.Document;
import com.documind.backend.document.DocumentChunkVectorRepository;
import com.documind.backend.document.DocumentRepository;
import com.documind.backend.workspace.WorkspaceAuthorizationService;
import com.documind.backend.workspace.WorkspaceRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final MessageCitationRepository citationRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkVectorRepository vectorRepository;
    private final AiServiceClient aiServiceClient;
    private final WorkspaceAuthorizationService authorizationService;
    private final AuditLogService auditLogService;

    public ChatService(ChatSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       MessageCitationRepository citationRepository,
                       DocumentRepository documentRepository,
                       DocumentChunkVectorRepository vectorRepository,
                       AiServiceClient aiServiceClient,
                       WorkspaceAuthorizationService authorizationService,
                       AuditLogService auditLogService) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.citationRepository = citationRepository;
        this.documentRepository = documentRepository;
        this.vectorRepository = vectorRepository;
        this.aiServiceClient = aiServiceClient;
        this.authorizationService = authorizationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ChatSessionResponse createSession(UUID workspaceId, UUID userId, CreateSessionRequest request) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.VIEWER);
        String title = (request != null && request.title() != null && !request.title().isBlank())
                ? request.title().trim()
                : "New Document Q&A";
        ChatSession session = sessionRepository.save(new ChatSession(workspaceId, userId, title));
        return ChatSessionResponse.from(session);
    }

    @Transactional(readOnly = true)
    public List<ChatSessionResponse> listSessions(UUID workspaceId, UUID userId) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.VIEWER);
        return sessionRepository.findAllByWorkspaceIdAndUserIdOrderByUpdatedAtDesc(workspaceId, userId).stream()
                .map(ChatSessionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> listMessages(UUID workspaceId, UUID sessionId, UUID userId) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.VIEWER);
        if (sessionRepository.findByIdAndWorkspaceId(sessionId, workspaceId).isEmpty()) {
            throw new ResourceNotFoundException("Chat session not found in workspace.");
        }

        List<ChatMessage> messages = messageRepository.findAllBySessionIdAndWorkspaceIdOrderByCreatedAtAsc(sessionId, workspaceId);
        if (messages.isEmpty()) {
            return List.of();
        }

        List<UUID> msgIds = messages.stream().map(ChatMessage::getId).toList();
        List<MessageCitation> allCitations = citationRepository.findAllByMessageIdInOrderByRelevanceScoreDesc(msgIds);
        Map<UUID, String> docNames = documentRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
                .collect(Collectors.toMap(Document::getId, Document::getFilename, (a, b) -> a));

        Map<UUID, List<CitationResponse>> citationsByMsg = new HashMap<>();
        for (MessageCitation c : allCitations) {
            citationsByMsg.computeIfAbsent(c.getMessageId(), k -> new ArrayList<>())
                    .add(CitationResponse.from(c, docNames.getOrDefault(c.getDocumentId(), "Document")));
        }

        return messages.stream()
                .map(m -> ChatMessageResponse.from(m, citationsByMsg.getOrDefault(m.getId(), List.of())))
                .toList();
    }

    @Transactional
    public ChatTurnResponse askQuestion(UUID workspaceId, UUID sessionId, UUID userId, AskQuestionRequest request) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.VIEWER);
        ChatSession session = sessionRepository.findByIdAndWorkspaceId(sessionId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found in workspace."));

        List<ChatMessage> priorMessages = messageRepository
                .findAllBySessionIdAndWorkspaceIdOrderByCreatedAtAsc(sessionId, workspaceId);

        // Auto-title session from first user question if still default
        if (priorMessages.isEmpty() && "New Document Q&A".equals(session.getTitle())) {
            String autoTitle = request.question().trim();
            session.setTitle(autoTitle.length() > 80 ? autoTitle.substring(0, 77) + "..." : autoTitle);
        }

        ChatMessage userMsg = messageRepository.save(new ChatMessage(
                sessionId, workspaceId, "USER", request.question().trim(),
                true, null, null, null
        ));

        // 1. Embed query and retrieve top-K chunks via pgvector
        List<Double> queryVec = aiServiceClient.embedQuery(request.question());
        List<DocumentChunkVectorRepository.SemanticChunkHit> hits = vectorRepository.searchSimilarChunks(
                workspaceId, queryVec, request.resolvedTopK(), request.documentIds()
        );

        List<AiServiceClient.RagContextChunkPayload> contextChunks = hits.stream()
                .map(h -> new AiServiceClient.RagContextChunkPayload(
                        h.chunkId().toString(),
                        h.documentId().toString(),
                        h.documentFilename(),
                        h.chunkIndex(),
                        h.pageNumber(),
                        h.pageStart(),
                        h.pageEnd(),
                        h.content(),
                        h.similarityScore()
                ))
                .toList();

        List<AiServiceClient.RagHistoryTurnPayload> historyPayload = priorMessages.stream()
                .skip(Math.max(0, priorMessages.size() - 4))
                .map(m -> new AiServiceClient.RagHistoryTurnPayload(m.getRole(), m.getContent()))
                .toList();

        // 2. Generate grounded answer & citations from ai-service
        AiServiceClient.RagAnswerResult ragResult = aiServiceClient.generateGroundedAnswer(
                request.question(), contextChunks, historyPayload
        );

        ChatMessage assistantMsg = messageRepository.save(new ChatMessage(
                sessionId,
                workspaceId,
                "ASSISTANT",
                ragResult.answer(),
                ragResult.found_answer(),
                ragResult.prompt_version(),
                ragResult.model_provider(),
                ragResult.model_name()
        ));

        List<CitationResponse> savedCitations = new ArrayList<>();
        if (ragResult.citations() != null) {
            for (AiServiceClient.RagCitationItem item : ragResult.citations()) {
                UUID docId = UUID.fromString(item.document_id());
                UUID chunkId = item.chunk_id() != null ? UUID.fromString(item.chunk_id()) : null;
                BigDecimal score = BigDecimal.valueOf(Math.max(0.0, Math.min(1.0, item.relevance_score())))
                        .setScale(5, RoundingMode.HALF_UP);

                MessageCitation entity = citationRepository.save(new MessageCitation(
                        assistantMsg.getId(),
                        workspaceId,
                        docId,
                        chunkId,
                        item.page_number(),
                        item.page_start(),
                        item.page_end(),
                        item.passage_reference(),
                        item.quoted_text(),
                        score
                ));
                savedCitations.add(CitationResponse.from(entity, item.document_filename()));
            }
        }

        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);

        auditLogService.record(workspaceId, userId, "CHAT_QUESTION_ASKED",
                "CHAT_SESSION", sessionId,
                Map.of("foundAnswer", ragResult.found_answer(), "citationsCount", savedCitations.size()));

        return new ChatTurnResponse(
                ChatMessageResponse.from(userMsg, List.of()),
                ChatMessageResponse.from(assistantMsg, savedCitations)
        );
    }

    @Transactional
    public ChatMessageResponse submitFeedback(UUID workspaceId, UUID messageId, UUID userId, MessageFeedbackRequest request) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.VIEWER);
        ChatMessage msg = messageRepository.findByIdAndWorkspaceId(messageId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat message not found in workspace."));
        if (!"ASSISTANT".equals(msg.getRole())) {
            throw new IllegalArgumentException("Feedback can only be recorded on ASSISTANT messages.");
        }
        msg.setFeedback(request.feedback());
        msg = messageRepository.save(msg);

        Map<UUID, String> docNames = documentRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
                .collect(Collectors.toMap(Document::getId, Document::getFilename, (a, b) -> a));
        List<CitationResponse> citations = citationRepository.findAllByMessageIdOrderByRelevanceScoreDesc(messageId).stream()
                .map(c -> CitationResponse.from(c, docNames.getOrDefault(c.getDocumentId(), "Document")))
                .toList();

        return ChatMessageResponse.from(msg, citations);
    }
}
