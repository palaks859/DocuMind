package com.documind.backend.compliance;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "checklist_items")
public class ChecklistItem {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "checklist_id", nullable = false, updatable = false)
    private UUID checklistId;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "criterion", nullable = false, length = 250)
    private String criterion;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "item_order", nullable = false)
    private int itemOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChecklistItem() {}

    public ChecklistItem(UUID checklistId, UUID workspaceId, String criterion, String description, int itemOrder) {
        this.id = UUID.randomUUID();
        this.checklistId = checklistId;
        this.workspaceId = workspaceId;
        this.criterion = criterion;
        this.description = description;
        this.itemOrder = itemOrder;
    }

    @PrePersist
    void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID();
        if (this.createdAt == null) this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getChecklistId() { return checklistId; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getCriterion() { return criterion; }
    public String getDescription() { return description; }
    public int getItemOrder() { return itemOrder; }
    public Instant getCreatedAt() { return createdAt; }
}
