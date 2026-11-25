package com.documind.backend.workspace;

import com.documind.backend.audit.AuditLogService;
import com.documind.backend.common.exception.ConflictException;
import com.documind.backend.common.exception.ResourceNotFoundException;
import com.documind.backend.user.User;
import com.documind.backend.user.UserRepository;
import com.documind.backend.workspace.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final AuditLogService auditLogService;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            WorkspaceMemberRepository memberRepository,
                            UserRepository userRepository,
                            WorkspaceAuthorizationService authorizationService,
                            AuditLogService auditLogService) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public WorkspaceResponse createWorkspace(User creator, CreateWorkspaceRequest request) {
        Workspace workspace = new Workspace(UUID.randomUUID(), request.name().trim(), creator);
        workspace = workspaceRepository.save(workspace);

        WorkspaceMember adminMembership = new WorkspaceMember(workspace, creator, WorkspaceRole.ADMIN);
        adminMembership = memberRepository.save(adminMembership);

        auditLogService.record(workspace.getId(), creator.getId(), "WORKSPACE_CREATED",
                "WORKSPACE", workspace.getId(), Map.of("name", workspace.getName()));

        return WorkspaceResponse.from(adminMembership);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listMyWorkspaces(UUID userId) {
        return workspaceRepository.findMembershipsByUserId(userId).stream()
                .map(WorkspaceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse getWorkspace(UUID workspaceId, UUID userId) {
        WorkspaceMember membership = authorizationService.requireRole(workspaceId, userId, WorkspaceRole.VIEWER);
        return WorkspaceResponse.from(membership);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> listMembers(UUID workspaceId, UUID userId) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.VIEWER);
        return memberRepository.findAllByWorkspaceIdWithUser(workspaceId).stream()
                .map(WorkspaceMemberResponse::from)
                .toList();
    }

    @Transactional
    public WorkspaceMemberResponse addMember(UUID workspaceId, UUID actorUserId, AddMemberRequest request) {
        WorkspaceMember actorMembership = authorizationService.requireRole(workspaceId, actorUserId, WorkspaceRole.ADMIN);
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        User targetUser = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("No user found with email: " + email));

        if (memberRepository.findByIdWorkspaceIdAndIdUserId(workspaceId, targetUser.getId()).isPresent()) {
            throw new ConflictException("User is already a member of this workspace.");
        }

        WorkspaceMember newMember = new WorkspaceMember(actorMembership.getWorkspace(), targetUser, request.role());
        newMember = memberRepository.save(newMember);

        auditLogService.record(workspaceId, actorUserId, "MEMBER_ADDED", "USER", targetUser.getId(),
                Map.of("email", targetUser.getEmail(), "role", request.role().name()));

        return WorkspaceMemberResponse.from(newMember);
    }

    @Transactional
    public WorkspaceMemberResponse updateMemberRole(UUID workspaceId, UUID actorUserId,
                                                    UUID targetUserId, UpdateMemberRoleRequest request) {
        authorizationService.requireRole(workspaceId, actorUserId, WorkspaceRole.ADMIN);

        WorkspaceMember targetMember = memberRepository.findByIdWorkspaceIdAndIdUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found."));

        WorkspaceRole previousRole = targetMember.getRole();
        if (previousRole == WorkspaceRole.ADMIN && request.role() != WorkspaceRole.ADMIN) {
            long adminCount = memberRepository.countByIdWorkspaceIdAndRole(workspaceId, WorkspaceRole.ADMIN);
            if (adminCount <= 1) {
                throw new IllegalArgumentException("Cannot demote the last ADMIN of a workspace.");
            }
        }

        targetMember.setRole(request.role());
        targetMember = memberRepository.save(targetMember);

        auditLogService.record(workspaceId, actorUserId, "MEMBER_ROLE_UPDATED", "USER", targetUserId,
                Map.of("previousRole", previousRole.name(), "newRole", request.role().name()));

        return WorkspaceMemberResponse.from(targetMember);
    }

    @Transactional
    public void removeMember(UUID workspaceId, UUID actorUserId, UUID targetUserId) {
        authorizationService.requireRole(workspaceId, actorUserId, WorkspaceRole.ADMIN);

        WorkspaceMember targetMember = memberRepository.findByIdWorkspaceIdAndIdUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found."));

        if (targetMember.getRole() == WorkspaceRole.ADMIN) {
            long adminCount = memberRepository.countByIdWorkspaceIdAndRole(workspaceId, WorkspaceRole.ADMIN);
            if (adminCount <= 1) {
                throw new IllegalArgumentException("Cannot remove the last ADMIN from a workspace.");
            }
        }

        memberRepository.delete(targetMember);

        auditLogService.record(workspaceId, actorUserId, "MEMBER_REMOVED", "USER", targetUserId,
                Map.of("removedRole", targetMember.getRole().name()));
    }
}
