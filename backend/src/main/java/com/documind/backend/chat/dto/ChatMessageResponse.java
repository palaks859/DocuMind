package com.documind.backend.chat.dto;

import com.documind.backend.chat.ChatMessage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID sessionId,
        String role,
        String content,
        boolean foundAnswer,
        String promptVersion,
        String modelProvider,
        String modelName,
        String feedback,
        Instant createdAt,
        List<CitationResponse> citations
) {
    public static ChatMessageResponse from(ChatMessage m, List<CitationResponse> citations) {
        return new ChatMessageResponse(
                m.getId(), m.getSessionId(), m.getRole(), m.getContent(),
                m.isFoundAnswer(), m.getPromptVersion(), m.getModelProvider(),
                m.getModelName(), m.getFeedback(), m.getCreatedAt(), citations
        );
    }
}
