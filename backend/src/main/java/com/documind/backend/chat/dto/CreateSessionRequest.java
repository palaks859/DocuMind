package com.documind.backend.chat.dto;

import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
        @Size(max = 300) String title
) {}
