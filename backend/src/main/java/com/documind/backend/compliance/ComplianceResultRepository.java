package com.documind.backend.compliance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComplianceResultRepository extends JpaRepository<ComplianceResult, UUID> {
    List<ComplianceResult> findAllByComplianceRunIdInOrderByCreatedAtAsc(List<UUID> runIds);
    Optional<ComplianceResult> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
