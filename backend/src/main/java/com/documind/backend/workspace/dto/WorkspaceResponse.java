package com.documind.backend.workspace.dto;

import com.documind.backend.workspace.Workspace;
import com.documind.backend.workspace.WorkspaceMember;
import com.documind.backend.workspace.WorkspaceRole;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        WorkspaceRole myRole,
        Instant createdAt
) {
    public static WorkspaceResponse from(WorkspaceMember membership) {
        Workspace ws = membership.getWorkspace();
        return new WorkspaceResponse(ws.getId(), ws.getName(), membership.getRole(), ws.getCreatedAt());
    }
}
