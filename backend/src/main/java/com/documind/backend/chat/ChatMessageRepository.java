package com.documind.backend.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findAllBySessionIdAndWorkspaceIdOrderByCreatedAtAsc(UUID sessionId, UUID workspaceId);
    Optional<ChatMessage> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
