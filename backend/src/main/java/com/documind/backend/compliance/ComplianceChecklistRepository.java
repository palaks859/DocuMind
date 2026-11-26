package com.documind.backend.compliance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComplianceChecklistRepository extends JpaRepository<ComplianceChecklist, UUID> {
    List<ComplianceChecklist> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
    Optional<ComplianceChecklist> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
