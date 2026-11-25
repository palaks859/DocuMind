package com.documind.backend.document;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "document_id", nullable = false, updatable = false)
    private UUID documentId;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "page_start")
    private Integer pageStart;

    @Column(name = "page_end")
    private Integer pageEnd;

    @Column(name = "start_offset")
    private Integer startOffset;

    @Column(name = "end_offset")
    private Integer endOffset;

    @Column(name = "embedding_model", length = 200)
    private String embeddingModel;

    @Column(name = "embedding_model_revision", length = 200)
    private String embeddingModelRevision;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DocumentChunk() {}

    public DocumentChunk(UUID id, UUID documentId, UUID workspaceId, int chunkIndex,
                         String content, Integer tokenCount, Integer pageNumber,
                         Integer pageStart, Integer pageEnd, Integer startOffset, Integer endOffset) {
        this.id = id;
        this.documentId = documentId;
        this.workspaceId = workspaceId;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.tokenCount = tokenCount;
        this.pageNumber = pageNumber;
        this.pageStart = pageStart;
        this.pageEnd = pageEnd;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    @PrePersist
    void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID();
        if (this.createdAt == null) this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDocumentId() { return documentId; }
    public UUID getWorkspaceId() { return workspaceId; }
    public int getChunkIndex() { return chunkIndex; }
    public String getContent() { return content; }
    public Integer getTokenCount() { return tokenCount; }
    public Integer getPageNumber() { return pageNumber; }
    public Integer getPageStart() { return pageStart; }
    public Integer getPageEnd() { return pageEnd; }
    public Integer getStartOffset() { return startOffset; }
    public Integer getEndOffset() { return endOffset; }
    public Instant getCreatedAt() { return createdAt; }
}
