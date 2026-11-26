package com.documind.backend.compliance;

import com.documind.backend.audit.AuditLogService;
import com.documind.backend.common.exception.ResourceNotFoundException;
import com.documind.backend.compliance.dto.ComplianceDtos.*;
import com.documind.backend.document.*;
import com.documind.backend.workspace.WorkspaceAuthorizationService;
import com.documind.backend.workspace.WorkspaceRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ComplianceService {

    private final ComplianceChecklistRepository checklistRepository;
    private final ChecklistItemRepository itemRepository;
    private final ComplianceRunRepository runRepository;
    private final ComplianceResultRepository resultRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final AiServiceClient aiServiceClient;
    private final WorkspaceAuthorizationService authorizationService;
    private final AuditLogService auditLogService;

    public ComplianceService(ComplianceChecklistRepository checklistRepository,
                             ChecklistItemRepository itemRepository,
                             ComplianceRunRepository runRepository,
                             ComplianceResultRepository resultRepository,
                             DocumentRepository documentRepository,
                             DocumentChunkRepository chunkRepository,
                             AiServiceClient aiServiceClient,
                             WorkspaceAuthorizationService authorizationService,
                             AuditLogService auditLogService) {
        this.checklistRepository = checklistRepository;
        this.itemRepository = itemRepository;
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.aiServiceClient = aiServiceClient;
        this.authorizationService = authorizationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ChecklistResponse createChecklist(UUID workspaceId, UUID userId, CreateChecklistRequest request) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.REVIEWER);
        ComplianceChecklist checklist = checklistRepository.save(
                new ComplianceChecklist(workspaceId, request.name().trim(), request.description(), userId)
        );

        List<ChecklistItemResponse> savedItems = new ArrayList<>();
        for (int i = 0; i < request.items().size(); i++) {
            CreateChecklistItemRequest itemReq = request.items().get(i);
            ChecklistItem item = itemRepository.save(new ChecklistItem(
                    checklist.getId(), workspaceId, itemReq.criterion().trim(), itemReq.description(), i
            ));
            savedItems.add(ChecklistItemResponse.from(item));
        }

        auditLogService.record(workspaceId, userId, "CHECKLIST_CREATED",
                "CHECKLIST", checklist.getId(), Map.of("name", checklist.getName(), "itemsCount", savedItems.size()));

        return ChecklistResponse.from(checklist, savedItems);
    }

    @Transactional(readOnly = true)
    public List<ChecklistResponse> listChecklists(UUID workspaceId, UUID userId) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.VIEWER);
        List<ComplianceChecklist> lists = checklistRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        if (lists.isEmpty()) return List.of();

        List<UUID> ids = lists.stream().map(ComplianceChecklist::getId).toList();
        Map<UUID, List<ChecklistItemResponse>> itemsByList = new HashMap<>();
        for (ChecklistItem item : itemRepository.findAllByChecklistIdInOrderByItemOrderAsc(ids)) {
            itemsByList.computeIfAbsent(item.getChecklistId(), k -> new ArrayList<>())
                    .add(ChecklistItemResponse.from(item));
        }

        return lists.stream()
                .map(c -> ChecklistResponse.from(c, itemsByList.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    @Transactional
    public ComplianceRunResponse runComplianceEvaluation(UUID workspaceId, UUID documentId, UUID userId, RunComplianceRequest request) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.REVIEWER);
        if (request == null || request.checklistId() == null) {
            throw new IllegalArgumentException("checklistId is required.");
        }

        Document doc = documentRepository.findByIdAndWorkspaceId(documentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found in workspace."));
        ComplianceChecklist checklist = checklistRepository.findByIdAndWorkspaceId(request.checklistId(), workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance checklist not found in workspace."));

        List<ChecklistItem> items = itemRepository.findAllByChecklistIdOrderByItemOrderAsc(checklist.getId());
        if (items.isEmpty()) {
            throw new IllegalStateException("Checklist has no criteria items.");
        }

        List<DocumentChunk> chunks = chunkRepository.findAllByDocumentIdAndWorkspaceIdOrderByChunkIndexAsc(documentId, workspaceId);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("Document has no ingested chunks.");
        }

        ComplianceRun run = runRepository.save(new ComplianceRun(documentId, checklist.getId(), workspaceId, userId));

        List<AiServiceClient.ChecklistCriterionPayload> criteriaPayloads = items.stream()
                .map(i -> new AiServiceClient.ChecklistCriterionPayload(i.getId().toString(), i.getCriterion(), i.getDescription()))
                .toList();

        List<AiServiceClient.DocChunkPayload> chunkPayloads = chunks.stream()
                .map(c -> new AiServiceClient.DocChunkPayload(
                        c.getId().toString(), c.getChunkIndex(), c.getPageNumber(),
                        c.getPageStart(), c.getPageEnd(), c.getContent()
                ))
                .toList();

        Map<UUID, String> criterionById = items.stream()
                .collect(Collectors.toMap(ChecklistItem::getId, ChecklistItem::getCriterion));

        try {
            AiServiceClient.ComplianceEvaluateResult evalResult = aiServiceClient.evaluateCompliance(
                    doc.getFilename(), criteriaPayloads, chunkPayloads
            );

            List<ComplianceResultResponse> savedResults = new ArrayList<>();
            if (evalResult.results() != null) {
                for (AiServiceClient.ComplianceItemResultItem itemRes : evalResult.results()) {
                    UUID itemId = UUID.fromString(itemRes.checklist_item_id());
                    UUID srcChunkId = itemRes.source_chunk_id() != null ? UUID.fromString(itemRes.source_chunk_id()) : null;

                    ComplianceResult saved = resultRepository.save(new ComplianceResult(
                            documentId, itemId, checklist.getId(), workspaceId, run.getId(),
                            srcChunkId, itemRes.outcome(), itemRes.evidence(), itemRes.explanation()
                    ));
                    savedResults.add(ComplianceResultResponse.from(saved, criterionById.getOrDefault(itemId, "Criterion")));
                }
            }

            run.setStatus("COMPLETED");
            run.setPromptVersion(evalResult.prompt_version());
            run.setModelProvider(evalResult.model_provider());
            run.setModelName(evalResult.model_name());
            run.setCompletedAt(Instant.now());
            run = runRepository.save(run);

            auditLogService.record(workspaceId, userId, "COMPLIANCE_RUN_COMPLETED",
                    "DOCUMENT", documentId, Map.of("runId", run.getId().toString(), "checklistId", checklist.getId().toString()));

            return ComplianceRunResponse.from(run, savedResults);
        } catch (Exception ex) {
            run.setStatus("FAILED");
            run.setErrorMessage(ex.getMessage());
            run.setCompletedAt(Instant.now());
            runRepository.save(run);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<ComplianceRunResponse> listComplianceRuns(UUID workspaceId, UUID documentId, UUID userId) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.VIEWER);
        if (documentRepository.findByIdAndWorkspaceId(documentId, workspaceId).isEmpty()) {
            throw new ResourceNotFoundException("Document not found in workspace.");
        }
        List<ComplianceRun> runs = runRepository.findAllByDocumentIdAndWorkspaceIdOrderByCreatedAtDesc(documentId, workspaceId);
        if (runs.isEmpty()) return List.of();

        List<UUID> checklistIds = runs.stream().map(ComplianceRun::getChecklistId).distinct().toList();
        Map<UUID, String> criterionById = itemRepository.findAllByChecklistIdInOrderByItemOrderAsc(checklistIds).stream()
                .collect(Collectors.toMap(ChecklistItem::getId, ChecklistItem::getCriterion, (a, b) -> a));

        List<UUID> runIds = runs.stream().map(ComplianceRun::getId).toList();
        Map<UUID, List<ComplianceResultResponse>> resultsByRun = new HashMap<>();
        for (ComplianceResult r : resultRepository.findAllByComplianceRunIdInOrderByCreatedAtAsc(runIds)) {
            resultsByRun.computeIfAbsent(r.getComplianceRunId(), k -> new ArrayList<>())
                    .add(ComplianceResultResponse.from(r, criterionById.getOrDefault(r.getChecklistItemId(), "Criterion")));
        }

        return runs.stream()
                .map(run -> ComplianceRunResponse.from(run, resultsByRun.getOrDefault(run.getId(), List.of())))
                .toList();
    }

    @Transactional
    public ComplianceResultResponse reviewResult(UUID workspaceId, UUID resultId, UUID userId, ReviewComplianceResultRequest request) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.REVIEWER);
        ComplianceResult res = resultRepository.findByIdAndWorkspaceId(resultId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance result not found in workspace."));

        res.setOutcome(request.outcome());
        if (request.explanation() != null && !request.explanation().isBlank()) {
            res.setExplanation(request.explanation().trim());
        }
        res.setReviewedBy(userId);
        res = resultRepository.save(res);

        String criterion = itemRepository.findById(res.getChecklistItemId())
                .map(ChecklistItem::getCriterion)
                .orElse("Criterion");

        auditLogService.record(workspaceId, userId, "COMPLIANCE_RESULT_REVIEWED",
                "COMPLIANCE_RESULT", resultId, Map.of("outcome", request.outcome()));

        return ComplianceResultResponse.from(res, criterion);
    }
}
