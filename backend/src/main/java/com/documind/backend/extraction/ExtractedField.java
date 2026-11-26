package com.documind.backend.extraction;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "extracted_fields")
public class ExtractedField {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "extraction_run_id", nullable = false, updatable = false)
    private UUID extractionRunId;

    @Column(name = "document_id", nullable = false, updatable = false)
    private UUID documentId;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "field_name", nullable = false, length = 200)
    private String fieldName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "field_value", columnDefinition = "jsonb")
    private Object fieldValue;

    @Column(name = "source_chunk_id")
    private UUID sourceChunkId;

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "page_start")
    private Integer pageStart;

    @Column(name = "page_end")
    private Integer pageEnd;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "review_status", nullable = false, length = 20)
    private String reviewStatus = "PENDING";

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExtractedField() {}

    public ExtractedField(UUID extractionRunId, UUID documentId, UUID workspaceId,
                          String fieldName, Object fieldValue, UUID sourceChunkId,
                          String evidence, Integer pageNumber, Integer pageStart, Integer pageEnd,
                          BigDecimal confidence) {
        this.id = UUID.randomUUID();
        this.extractionRunId = extractionRunId;
        this.documentId = documentId;
        this.workspaceId = workspaceId;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.sourceChunkId = sourceChunkId;
        this.evidence = evidence;
        this.pageNumber = pageNumber;
        this.pageStart = pageStart;
        this.pageEnd = pageEnd;
        this.confidence = confidence;
        this.reviewStatus = "PENDING";
    }

    @PrePersist
    void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID();
        if (this.createdAt == null) this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getExtractionRunId() { return extractionRunId; }
    public UUID getDocumentId() { return documentId; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getFieldName() { return fieldName; }
    public Object getFieldValue() { return fieldValue; }
    public void setFieldValue(Object fieldValue) { this.fieldValue = fieldValue; }
    public UUID getSourceChunkId() { return sourceChunkId; }
    public String getEvidence() { return evidence; }
    public Integer getPageNumber() { return pageNumber; }
    public Integer getPageStart() { return pageStart; }
    public Integer getPageEnd() { return pageEnd; }
    public BigDecimal getConfidence() { return confidence; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public UUID getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(UUID reviewedBy) { this.reviewedBy = reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
