package com.documind.backend.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MessageCitationRepository extends JpaRepository<MessageCitation, UUID> {
    List<MessageCitation> findAllByMessageIdInOrderByRelevanceScoreDesc(List<UUID> messageIds);
    List<MessageCitation> findAllByMessageIdOrderByRelevanceScoreDesc(UUID messageId);
}
