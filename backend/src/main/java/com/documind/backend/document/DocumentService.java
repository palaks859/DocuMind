package com.documind.backend.document;

import com.documind.backend.audit.AuditLogService;
import com.documind.backend.common.exception.ResourceNotFoundException;
import com.documind.backend.document.dto.DocumentChunkResponse;
import com.documind.backend.document.dto.DocumentResponse;
import com.documind.backend.storage.StorageService;
import com.documind.backend.storage.StoredFileResult;
import com.documind.backend.user.User;
import com.documind.backend.workspace.WorkspaceAuthorizationService;
import com.documind.backend.workspace.WorkspaceMember;
import com.documind.backend.workspace.WorkspaceRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentChunkVectorRepository chunkVectorRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final StorageService storageService;
    private final AiServiceClient aiServiceClient;
    private final AuditLogService auditLogService;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentChunkRepository chunkRepository,
                           DocumentChunkVectorRepository chunkVectorRepository,
                           WorkspaceAuthorizationService authorizationService,
                           StorageService storageService,
                           AiServiceClient aiServiceClient,
                           AuditLogService auditLogService) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.chunkVectorRepository = chunkVectorRepository;
        this.authorizationService = authorizationService;
        this.storageService = storageService;
        this.aiServiceClient = aiServiceClient;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public DocumentResponse upload(UUID workspaceId, User uploader, MultipartFile file) {
        WorkspaceMember membership = authorizationService.requireRole(
                workspaceId, uploader.getId(), WorkspaceRole.REVIEWER);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file must not be empty.");
        }

        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType + ". Allowed: PDF, DOCX, TXT.");
        }

        String originalFilename = file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()
                ? file.getOriginalFilename().trim()
                : "untitled";

        UUID documentId = UUID.randomUUID();
        StoredFileResult stored = storageService.store(workspaceId, documentId, file);

        Document document = new Document(
                documentId,
                membership.getWorkspace(),
                uploader,
                originalFilename,
                contentType,
                stored.sizeBytes(),
                stored.storageKey(),
                stored.sha256Checksum()
        );
        document.setStatus(DocumentStatus.PROCESSING);
        document = documentRepository.save(document);

        auditLogService.record(workspaceId, uploader.getId(), "DOCUMENT_UPLOADED",
                "DOCUMENT", document.getId(),
                Map.of("filename", document.getFilename(), "fileSize", document.getFileSize(), "contentType", contentType));

        try {
            AiServiceClient.IngestResult ingested = aiServiceClient.ingestFromStorage(
                    stored.storageKey(), originalFilename, contentType);

            if (ingested != null && ingested.chunks() != null) {
                for (AiServiceClient.ChunkItem c : ingested.chunks()) {
                    chunkVectorRepository.insertChunkWithEmbedding(
                            UUID.randomUUID(),
                            documentId,
                            workspaceId,
                            c.chunk_index(),
                            c.content(),
                            c.token_count(),
                            c.page_number(),
                            c.page_start(),
                            c.page_end(),
                            c.start_offset(),
                            c.end_offset(),
                            c.embedding(),
                            ingested.embedding_model(),
                            ingested.embedding_model_revision()
                    );
                }
                document.setPageCount(ingested.page_count());
            }
            document.setStatus(DocumentStatus.READY);
            document = documentRepository.save(document);

            auditLogService.record(workspaceId, uploader.getId(), "DOCUMENT_INGESTED",
                    "DOCUMENT", document.getId(),
                    Map.of("pageCount", document.getPageCount() != null ? document.getPageCount() : 0,
                           "chunkCount", ingested != null ? ingested.total_chunks() : 0,
                           "embeddingModel", ingested != null && ingested.embedding_model() != null ? ingested.embedding_model() : ""));
        } catch (Exception ex) {
            log.warn("Document ingestion failed for document {}: {}", documentId, ex.getMessage());
            document.setStatus(DocumentStatus.FAILED);
            document.setFailureMessage(ex.getMessage());
            document = documentRepository.save(document);
        }

        return DocumentResponse.from(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listDocuments(UUID workspaceId, UUID userId) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.VIEWER);
        return documentRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentChunkResponse> listDocumentChunks(UUID workspaceId, UUID documentId, UUID userId) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.VIEWER);
        if (documentRepository.findByIdAndWorkspaceId(documentId, workspaceId).isEmpty()) {
            throw new ResourceNotFoundException("Document not found in workspace.");
        }
        return chunkRepository.findAllByDocumentIdAndWorkspaceIdOrderByChunkIndexAsc(documentId, workspaceId).stream()
                .map(DocumentChunkResponse::from)
                .toList();
    }

    @Transactional
    public DocumentResponse getDocument(UUID workspaceId, UUID documentId, UUID userId) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.VIEWER);
        Document document = documentRepository.findByIdAndWorkspaceId(documentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found in workspace."));

        auditLogService.record(workspaceId, userId, "DOCUMENT_VIEWED",
                "DOCUMENT", document.getId(), Map.of("filename", document.getFilename()));

        return DocumentResponse.from(document);
    }

    @Transactional
    public DownloadedDocument downloadDocument(UUID workspaceId, UUID documentId, UUID userId) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.VIEWER);
        Document document = documentRepository.findByIdAndWorkspaceId(documentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found in workspace."));

        Resource resource = storageService.loadAsResource(document.getStorageKey());

        auditLogService.record(workspaceId, userId, "DOCUMENT_DOWNLOADED",
                "DOCUMENT", document.getId(), Map.of("filename", document.getFilename()));

        return new DownloadedDocument(document, resource);
    }

    @Transactional
    public void deleteDocument(UUID workspaceId, UUID documentId, UUID actorUserId) {
        authorizationService.requireRole(workspaceId, actorUserId, WorkspaceRole.REVIEWER);
        Document document = documentRepository.findByIdAndWorkspaceId(documentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found in workspace."));

        String storageKey = document.getStorageKey();
        String filename = document.getFilename();

        documentRepository.delete(document);
        storageService.delete(storageKey);

        auditLogService.record(workspaceId, actorUserId, "DOCUMENT_DELETED",
                "DOCUMENT", documentId, Map.of("filename", filename));
    }

    public record DownloadedDocument(Document document, Resource resource) {}
}
