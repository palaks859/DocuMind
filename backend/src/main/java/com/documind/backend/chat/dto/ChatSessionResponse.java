package com.documind.backend.chat.dto;

import com.documind.backend.chat.ChatSession;
import java.time.Instant;
import java.util.UUID;

public record ChatSessionResponse(
        UUID id,
        UUID workspaceId,
        UUID userId,
        String title,
        Instant createdAt,
        Instant updatedAt
) {
    public static ChatSessionResponse from(ChatSession s) {
        return new ChatSessionResponse(
                s.getId(), s.getWorkspaceId(), s.getUserId(),
                s.getTitle(), s.getCreatedAt(), s.getUpdatedAt()
        );
    }
}
