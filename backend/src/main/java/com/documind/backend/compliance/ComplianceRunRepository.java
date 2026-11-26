package com.documind.backend.compliance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ComplianceRunRepository extends JpaRepository<ComplianceRun, UUID> {
    List<ComplianceRun> findAllByDocumentIdAndWorkspaceIdOrderByCreatedAtDesc(UUID documentId, UUID workspaceId);
}
