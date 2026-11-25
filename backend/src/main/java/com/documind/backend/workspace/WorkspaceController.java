package com.documind.backend.workspace;

import com.documind.backend.auth.CurrentUserService;
import com.documind.backend.user.User;
import com.documind.backend.workspace.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
@Tag(name = "Workspaces & RBAC", description = "Workspace management and role-based member administration")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final CurrentUserService currentUserService;

    public WorkspaceController(WorkspaceService workspaceService, CurrentUserService currentUserService) {
        this.workspaceService = workspaceService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @Operation(summary = "Create a new workspace (creator becomes ADMIN)")
    public ResponseEntity<WorkspaceResponse> create(@Valid @RequestBody CreateWorkspaceRequest request) {
        User current = currentUserService.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(workspaceService.createWorkspace(current, request));
    }

    @GetMapping
    @Operation(summary = "List workspaces accessible to the current user")
    public ResponseEntity<List<WorkspaceResponse>> listMine() {
        return ResponseEntity.ok(workspaceService.listMyWorkspaces(currentUserService.getCurrentUserId()));
    }

    @GetMapping("/{workspaceId}")
    @Operation(summary = "Get workspace details (VIEWER+)")
    public ResponseEntity<WorkspaceResponse> getById(@PathVariable UUID workspaceId) {
        return ResponseEntity.ok(workspaceService.getWorkspace(workspaceId, currentUserService.getCurrentUserId()));
    }

    @GetMapping("/{workspaceId}/members")
    @Operation(summary = "List members and roles in a workspace (VIEWER+)")
    public ResponseEntity<List<WorkspaceMemberResponse>> listMembers(@PathVariable UUID workspaceId) {
        return ResponseEntity.ok(workspaceService.listMembers(workspaceId, currentUserService.getCurrentUserId()));
    }

    @PostMapping("/{workspaceId}/members")
    @Operation(summary = "Add a user to a workspace with a role (ADMIN only)")
    public ResponseEntity<WorkspaceMemberResponse> addMember(@PathVariable UUID workspaceId,
                                                             @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workspaceService.addMember(workspaceId, currentUserService.getCurrentUserId(), request));
    }

    @PatchMapping("/{workspaceId}/members/{userId}")
    @Operation(summary = "Update a workspace member's role (ADMIN only)")
    public ResponseEntity<WorkspaceMemberResponse> updateMemberRole(@PathVariable UUID workspaceId,
                                                                    @PathVariable UUID userId,
                                                                    @Valid @RequestBody UpdateMemberRoleRequest request) {
        return ResponseEntity.ok(workspaceService.updateMemberRole(
                workspaceId, currentUserService.getCurrentUserId(), userId, request));
    }

    @DeleteMapping("/{workspaceId}/members/{userId}")
    @Operation(summary = "Remove a member from a workspace (ADMIN only)")
    public ResponseEntity<Void> removeMember(@PathVariable UUID workspaceId, @PathVariable UUID userId) {
        workspaceService.removeMember(workspaceId, currentUserService.getCurrentUserId(), userId);
        return ResponseEntity.noContent().build();
    }
}
