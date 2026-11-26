package com.documind.backend.extraction;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExtractedFieldRepository extends JpaRepository<ExtractedField, UUID> {
    List<ExtractedField> findAllByExtractionRunIdInOrderByCreatedAtAsc(List<UUID> runIds);
    Optional<ExtractedField> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
