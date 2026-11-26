package com.documind.backend.compliance;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compliance_runs")
public class ComplianceRun {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "document_id", nullable = false, updatable = false)
    private UUID documentId;

    @Column(name = "checklist_id", nullable = false, updatable = false)
    private UUID checklistId;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "requested_by")
    private UUID requestedBy;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "prompt_version", length = 100)
    private String promptVersion;

    @Column(name = "model_provider", length = 100)
    private String modelProvider;

    @Column(name = "model_name", length = 200)
    private String modelName;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ComplianceRun() {}

    public ComplianceRun(UUID documentId, UUID checklistId, UUID workspaceId, UUID requestedBy) {
        this.id = UUID.randomUUID();
        this.documentId = documentId;
        this.checklistId = checklistId;
        this.workspaceId = workspaceId;
        this.requestedBy = requestedBy;
        this.status = "PROCESSING";
    }

    @PrePersist
    void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID();
        if (this.createdAt == null) this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDocumentId() { return documentId; }
    public UUID getChecklistId() { return checklistId; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public String getModelProvider() { return modelProvider; }
    public void setModelProvider(String modelProvider) { this.modelProvider = modelProvider; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
