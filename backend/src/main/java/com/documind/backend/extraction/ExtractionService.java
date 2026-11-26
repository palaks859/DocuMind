package com.documind.backend.extraction;

import com.documind.backend.audit.AuditLogService;
import com.documind.backend.common.exception.ResourceNotFoundException;
import com.documind.backend.document.*;
import com.documind.backend.extraction.dto.ExtractionDtos.*;
import com.documind.backend.workspace.WorkspaceAuthorizationService;
import com.documind.backend.workspace.WorkspaceRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
public class ExtractionService {

    private final ExtractionRunRepository runRepository;
    private final ExtractedFieldRepository fieldRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final AiServiceClient aiServiceClient;
    private final WorkspaceAuthorizationService authorizationService;
    private final AuditLogService auditLogService;

    public ExtractionService(ExtractionRunRepository runRepository,
                             ExtractedFieldRepository fieldRepository,
                             DocumentRepository documentRepository,
                             DocumentChunkRepository chunkRepository,
                             AiServiceClient aiServiceClient,
                             WorkspaceAuthorizationService authorizationService,
                             AuditLogService auditLogService) {
        this.runRepository = runRepository;
        this.fieldRepository = fieldRepository;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.aiServiceClient = aiServiceClient;
        this.authorizationService = authorizationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ExtractionRunResponse runExtraction(UUID workspaceId, UUID documentId, UUID userId, RunExtractionRequest request) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.REVIEWER);
        Document doc = documentRepository.findByIdAndWorkspaceId(documentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found in workspace."));

        List<DocumentChunk> chunks = chunkRepository.findAllByDocumentIdAndWorkspaceIdOrderByChunkIndexAsc(documentId, workspaceId);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("Document has no ingested chunks available for extraction.");
        }

        String schemaName = (request != null && request.schemaName() != null && !request.schemaName().isBlank())
                ? request.schemaName().trim()
                : "enterprise_contract_v1";

        ExtractionRun run = runRepository.save(new ExtractionRun(documentId, workspaceId, userId, schemaName));

        List<AiServiceClient.FieldSpecPayload> fieldSpecs = (request != null && request.fields() != null)
                ? request.fields().stream().map(f -> new AiServiceClient.FieldSpecPayload(f.fieldName(), f.description())).toList()
                : List.of();

        List<AiServiceClient.DocChunkPayload> chunkPayloads = chunks.stream()
                .map(c -> new AiServiceClient.DocChunkPayload(
                        c.getId().toString(), c.getChunkIndex(), c.getPageNumber(),
                        c.getPageStart(), c.getPageEnd(), c.getContent()
                ))
                .toList();

        try {
            AiServiceClient.FieldExtractResult result = aiServiceClient.extractFields(
                    schemaName, doc.getFilename(), fieldSpecs, chunkPayloads
            );

            List<ExtractedFieldResponse> savedFields = new ArrayList<>();
            if (result.fields() != null) {
                for (AiServiceClient.ExtractedFieldItem item : result.fields()) {
                    UUID srcChunkId = item.source_chunk_id() != null ? UUID.fromString(item.source_chunk_id()) : null;
                    BigDecimal conf = BigDecimal.valueOf(Math.max(0.0, Math.min(1.0, item.confidence())))
                            .setScale(4, RoundingMode.HALF_UP);

                    ExtractedField saved = fieldRepository.save(new ExtractedField(
                            run.getId(), documentId, workspaceId, item.field_name(),
                            item.field_value(), srcChunkId, item.evidence(),
                            item.page_number(), item.page_start(), item.page_end(), conf
                    ));
                    savedFields.add(ExtractedFieldResponse.from(saved));
                }
            }

            run.setStatus("SUCCEEDED");
            run.setPromptVersion(result.prompt_version());
            run.setModelProvider(result.model_provider());
            run.setModelName(result.model_name());
            run.setCompletedAt(Instant.now());
            run = runRepository.save(run);

            auditLogService.record(workspaceId, userId, "EXTRACTION_RUN_COMPLETED",
                    "DOCUMENT", documentId, Map.of("runId", run.getId().toString(), "fieldsCount", savedFields.size()));

            return ExtractionRunResponse.from(run, savedFields);
        } catch (Exception ex) {
            run.setStatus("FAILED");
            run.setErrorMessage(ex.getMessage());
            run.setCompletedAt(Instant.now());
            runRepository.save(run);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<ExtractionRunResponse> listRuns(UUID workspaceId, UUID documentId, UUID userId) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.VIEWER);
        if (documentRepository.findByIdAndWorkspaceId(documentId, workspaceId).isEmpty()) {
            throw new ResourceNotFoundException("Document not found in workspace.");
        }
        List<ExtractionRun> runs = runRepository.findAllByDocumentIdAndWorkspaceIdOrderByCreatedAtDesc(documentId, workspaceId);
        if (runs.isEmpty()) return List.of();

        List<UUID> runIds = runs.stream().map(ExtractionRun::getId).toList();
        Map<UUID, List<ExtractedFieldResponse>> fieldsByRun = new HashMap<>();
        for (ExtractedField f : fieldRepository.findAllByExtractionRunIdInOrderByCreatedAtAsc(runIds)) {
            fieldsByRun.computeIfAbsent(f.getExtractionRunId(), k -> new ArrayList<>())
                    .add(ExtractedFieldResponse.from(f));
        }

        return runs.stream()
                .map(r -> ExtractionRunResponse.from(r, fieldsByRun.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    @Transactional
    public ExtractedFieldResponse reviewField(UUID workspaceId, UUID fieldId, UUID userId, ReviewFieldRequest request) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.REVIEWER);
        ExtractedField field = fieldRepository.findByIdAndWorkspaceId(fieldId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Extracted field not found in workspace."));

        field.setReviewStatus(request.reviewStatus());
        if ("EDITED".equals(request.reviewStatus())) {
            field.setFieldValue(request.overrideValue());
        }
        field.setReviewedBy(userId);
        field.setReviewedAt(Instant.now());
        field = fieldRepository.save(field);

        auditLogService.record(workspaceId, userId, "EXTRACTED_FIELD_REVIEWED",
                "EXTRACTED_FIELD", fieldId, Map.of("reviewStatus", request.reviewStatus()));

        return ExtractedFieldResponse.from(field);
    }
}
