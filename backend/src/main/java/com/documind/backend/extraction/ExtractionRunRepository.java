package com.documind.backend.extraction;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ExtractionRunRepository extends JpaRepository<ExtractionRun, UUID> {
    List<ExtractionRun> findAllByDocumentIdAndWorkspaceIdOrderByCreatedAtDesc(UUID documentId, UUID workspaceId);
}
