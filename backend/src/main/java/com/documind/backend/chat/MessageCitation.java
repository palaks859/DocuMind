package com.documind.backend.chat;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "message_citations")
public class MessageCitation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "message_id", nullable = false, updatable = false)
    private UUID messageId;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "chunk_id")
    private UUID chunkId;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "page_start")
    private Integer pageStart;

    @Column(name = "page_end")
    private Integer pageEnd;

    @Column(name = "passage_reference", columnDefinition = "TEXT")
    private String passageReference;

    @Column(name = "quoted_text", columnDefinition = "TEXT")
    private String quotedText;

    @Column(name = "relevance_score", precision = 6, scale = 5)
    private BigDecimal relevanceScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MessageCitation() {}

    public MessageCitation(UUID messageId, UUID workspaceId, UUID documentId, UUID chunkId,
                           Integer pageNumber, Integer pageStart, Integer pageEnd,
                           String passageReference, String quotedText, BigDecimal relevanceScore) {
        this.id = UUID.randomUUID();
        this.messageId = messageId;
        this.workspaceId = workspaceId;
        this.documentId = documentId;
        this.chunkId = chunkId;
        this.pageNumber = pageNumber;
        this.pageStart = pageStart;
        this.pageEnd = pageEnd;
        this.passageReference = passageReference;
        this.quotedText = quotedText;
        this.relevanceScore = relevanceScore;
    }

    @PrePersist
    void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID();
        if (this.createdAt == null) this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getMessageId() { return messageId; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getDocumentId() { return documentId; }
    public UUID getChunkId() { return chunkId; }
    public Integer getPageNumber() { return pageNumber; }
    public Integer getPageStart() { return pageStart; }
    public Integer getPageEnd() { return pageEnd; }
    public String getPassageReference() { return passageReference; }
    public String getQuotedText() { return quotedText; }
    public BigDecimal getRelevanceScore() { return relevanceScore; }
    public Instant getCreatedAt() { return createdAt; }
}
