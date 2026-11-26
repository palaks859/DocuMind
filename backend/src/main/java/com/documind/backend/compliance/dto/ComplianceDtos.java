package com.documind.backend.compliance.dto;

import com.documind.backend.compliance.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ComplianceDtos {

    public record CreateChecklistItemRequest(
            @NotBlank @Size(max = 250) String criterion,
            @Size(max = 2000) String description
    ) {}

    public record CreateChecklistRequest(
            @NotBlank @Size(max = 250) String name,
            @Size(max = 2000) String description,
            @NotEmpty @Valid List<CreateChecklistItemRequest> items
    ) {}

    public record ChecklistItemResponse(
            UUID id,
            String criterion,
            String description,
            int itemOrder
    ) {
        public static ChecklistItemResponse from(ChecklistItem i) {
            return new ChecklistItemResponse(i.getId(), i.getCriterion(), i.getDescription(), i.getItemOrder());
        }
    }

    public record ChecklistResponse(
            UUID id,
            UUID workspaceId,
            String name,
            String description,
            Instant createdAt,
            List<ChecklistItemResponse> items
    ) {
        public static ChecklistResponse from(ComplianceChecklist c, List<ChecklistItemResponse> items) {
            return new ChecklistResponse(c.getId(), c.getWorkspaceId(), c.getName(), c.getDescription(), c.getCreatedAt(), items);
        }
    }

    public record RunComplianceRequest(
            UUID checklistId
    ) {}

    public record ComplianceResultResponse(
            UUID id,
            UUID checklistItemId,
            String criterion,
            UUID sourceChunkId,
            String outcome,
            String evidence,
            String explanation,
            UUID reviewedBy
    ) {
        public static ComplianceResultResponse from(ComplianceResult r, String criterion) {
            return new ComplianceResultResponse(
                    r.getId(), r.getChecklistItemId(), criterion, r.getSourceChunkId(),
                    r.getOutcome(), r.getEvidence(), r.getExplanation(), r.getReviewedBy()
            );
        }
    }

    public record ComplianceRunResponse(
            UUID id,
            UUID documentId,
            UUID checklistId,
            String status,
            String promptVersion,
            String modelProvider,
            String modelName,
            Instant createdAt,
            Instant completedAt,
            List<ComplianceResultResponse> results
    ) {
        public static ComplianceRunResponse from(ComplianceRun run, List<ComplianceResultResponse> results) {
            return new ComplianceRunResponse(
                    run.getId(), run.getDocumentId(), run.getChecklistId(), run.getStatus(),
                    run.getPromptVersion(), run.getModelProvider(), run.getModelName(),
                    run.getCreatedAt(), run.getCompletedAt(), results
            );
        }
    }

    public record ReviewComplianceResultRequest(
            @Pattern(regexp = "PASS|FAIL|UNCLEAR", message = "outcome must be PASS, FAIL, or UNCLEAR")
            String outcome,
            @Size(max = 2000) String explanation
    ) {}
}
