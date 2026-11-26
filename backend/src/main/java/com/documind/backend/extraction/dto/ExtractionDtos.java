package com.documind.backend.extraction.dto;

import com.documind.backend.extraction.ExtractedField;
import com.documind.backend.extraction.ExtractionRun;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ExtractionDtos {

    public record FieldSpecDto(
            @Size(min = 1, max = 200) String fieldName,
            @Size(max = 500) String description
    ) {}

    public record RunExtractionRequest(
            @Size(max = 150) String schemaName,
            List<FieldSpecDto> fields
    ) {}

    public record ExtractedFieldResponse(
            UUID id,
            UUID extractionRunId,
            String fieldName,
            Object fieldValue,
            UUID sourceChunkId,
            String evidence,
            Integer pageNumber,
            Integer pageStart,
            Integer pageEnd,
            BigDecimal confidence,
            String reviewStatus,
            UUID reviewedBy,
            Instant reviewedAt
    ) {
        public static ExtractedFieldResponse from(ExtractedField f) {
            return new ExtractedFieldResponse(
                    f.getId(), f.getExtractionRunId(), f.getFieldName(), f.getFieldValue(),
                    f.getSourceChunkId(), f.getEvidence(), f.getPageNumber(), f.getPageStart(),
                    f.getPageEnd(), f.getConfidence(), f.getReviewStatus(), f.getReviewedBy(), f.getReviewedAt()
            );
        }
    }

    public record ExtractionRunResponse(
            UUID id,
            UUID documentId,
            String status,
            String schemaName,
            String promptVersion,
            String modelProvider,
            String modelName,
            String errorMessage,
            Instant createdAt,
            Instant completedAt,
            List<ExtractedFieldResponse> fields
    ) {
        public static ExtractionRunResponse from(ExtractionRun r, List<ExtractedFieldResponse> fields) {
            return new ExtractionRunResponse(
                    r.getId(), r.getDocumentId(), r.getStatus(), r.getSchemaName(),
                    r.getPromptVersion(), r.getModelProvider(), r.getModelName(),
                    r.getErrorMessage(), r.getCreatedAt(), r.getCompletedAt(), fields
            );
        }
    }

    public record ReviewFieldRequest(
            @Pattern(regexp = "APPROVED|REJECTED|EDITED", message = "reviewStatus must be APPROVED, REJECTED, or EDITED")
            String reviewStatus,
            Object overrideValue
    ) {}
}
