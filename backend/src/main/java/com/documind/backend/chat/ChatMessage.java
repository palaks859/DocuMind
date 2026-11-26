package com.documind.backend.chat;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "role", nullable = false, length = 30)
    private String role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "found_answer", nullable = false)
    private boolean foundAnswer = true;

    @Column(name = "prompt_version", length = 100)
    private String promptVersion;

    @Column(name = "model_provider", length = 100)
    private String modelProvider;

    @Column(name = "model_name", length = 200)
    private String modelName;

    @Column(name = "feedback", length = 20)
    private String feedback;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatMessage() {}

    public ChatMessage(UUID sessionId, UUID workspaceId, String role, String content,
                       boolean foundAnswer, String promptVersion, String modelProvider, String modelName) {
        this.id = UUID.randomUUID();
        this.sessionId = sessionId;
        this.workspaceId = workspaceId;
        this.role = role;
        this.content = content;
        this.foundAnswer = foundAnswer;
        this.promptVersion = promptVersion;
        this.modelProvider = modelProvider;
        this.modelName = modelName;
    }

    @PrePersist
    void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID();
        if (this.createdAt == null) this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getSessionId() { return sessionId; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public boolean isFoundAnswer() { return foundAnswer; }
    public String getPromptVersion() { return promptVersion; }
    public String getModelProvider() { return modelProvider; }
    public String getModelName() { return modelName; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public Instant getCreatedAt() { return createdAt; }
}
