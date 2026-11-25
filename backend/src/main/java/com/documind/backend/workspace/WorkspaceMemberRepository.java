package com.documind.backend.workspace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, WorkspaceMemberId> {
    Optional<WorkspaceMember> findByIdWorkspaceIdAndIdUserId(UUID workspaceId, UUID userId);

    @Query("SELECT wm FROM WorkspaceMember wm JOIN FETCH wm.user WHERE wm.id.workspaceId = :workspaceId ORDER BY wm.joinedAt ASC")
    List<WorkspaceMember> findAllByWorkspaceIdWithUser(@Param("workspaceId") UUID workspaceId);

    long countByIdWorkspaceIdAndRole(UUID workspaceId, WorkspaceRole role);
}
