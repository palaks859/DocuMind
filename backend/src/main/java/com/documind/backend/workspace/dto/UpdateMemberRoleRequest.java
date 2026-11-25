package com.documind.backend.workspace.dto;

import com.documind.backend.workspace.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull(message = "Role is required (ADMIN, REVIEWER, or VIEWER)")
        WorkspaceRole role
) {}
