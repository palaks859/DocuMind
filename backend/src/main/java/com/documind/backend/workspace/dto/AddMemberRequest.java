package com.documind.backend.workspace.dto;

import com.documind.backend.workspace.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(
        @NotBlank(message = "Member email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotNull(message = "Role is required (ADMIN, REVIEWER, or VIEWER)")
        WorkspaceRole role
) {}
