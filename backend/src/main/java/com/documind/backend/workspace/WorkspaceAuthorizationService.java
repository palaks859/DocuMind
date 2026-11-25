package com.documind.backend.workspace;

import com.documind.backend.common.exception.ForbiddenOperationException;
import com.documind.backend.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WorkspaceAuthorizationService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;

    public WorkspaceAuthorizationService(WorkspaceRepository workspaceRepository,
                                         WorkspaceMemberRepository memberRepository) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public WorkspaceMember requireRole(UUID workspaceId, UUID userId, WorkspaceRole minimumRole) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new ResourceNotFoundException("Workspace not found.");
        }

        WorkspaceMember membership = memberRepository.findByIdWorkspaceIdAndIdUserId(workspaceId, userId)
                .orElseThrow(() -> new ForbiddenOperationException("You do not have access to this workspace."));

        if (!membership.getRole().hasAtLeast(minimumRole)) {
            throw new ForbiddenOperationException(
                    "Insufficient workspace permissions. Required role: " + minimumRole.name());
        }
        return membership;
    }
}
