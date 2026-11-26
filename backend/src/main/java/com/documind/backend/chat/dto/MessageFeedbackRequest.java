package com.documind.backend.chat.dto;

import jakarta.validation.constraints.Pattern;

public record MessageFeedbackRequest(
        @Pattern(regexp = "HELPFUL|NOT_HELPFUL", message = "Feedback must be HELPFUL or NOT_HELPFUL")
        String feedback
) {}
