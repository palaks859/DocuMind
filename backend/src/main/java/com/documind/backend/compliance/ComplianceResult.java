package com.documind.backend.compliance;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compliance_results")
public class ComplianceResult {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "document_id", nullable = false, updatable = false)
    private UUID documentId;

    @Column(name = "checklist_item_id", nullable = false, updatable = false)
    private UUID checklistItemId;

    @Column(name = "checklist_id", nullable = false, updatable = false)
    private UUID checklistId;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "compliance_run_id", nullable = false, updatable = false)
    private UUID complianceRunId;

    @Column(name = "source_chunk_id")
    private UUID sourceChunkId;

    @Column(name = "outcome", nullable = false, length = 20)
    private String outcome;

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ComplianceResult() {}

    public ComplianceResult(UUID documentId, UUID checklistItemId, UUID checklistId, UUID workspaceId,
                            UUID complianceRunId, UUID sourceChunkId, String outcome,
                            String evidence, String explanation) {
        this.id = UUID.randomUUID();
        this.documentId = documentId;
        this.checklistItemId = checklistItemId;
        this.checklistId = checklistId;
        this.workspaceId = workspaceId;
        this.complianceRunId = complianceRunId;
        this.sourceChunkId = sourceChunkId;
        this.outcome = outcome;
        this.evidence = evidence;
        this.explanation = explanation;
    }

    @PrePersist
    void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID();
        if (this.createdAt == null) this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDocumentId() { return documentId; }
    public UUID getChecklistItemId() { return checklistItemId; }
    public UUID getChecklistId() { return checklistId; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getComplianceRunId() { return complianceRunId; }
    public UUID getSourceChunkId() { return sourceChunkId; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getEvidence() { return evidence; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public UUID getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(UUID reviewedBy) { this.reviewedBy = reviewedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
