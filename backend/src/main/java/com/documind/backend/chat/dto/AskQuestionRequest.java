package com.documind.backend.chat.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record AskQuestionRequest(
        @NotBlank @Size(max = 4000) String question,
        @Min(1) @Max(15) Integer topK,
        List<UUID> documentIds
) {
    public int resolvedTopK() {
        return topK != null ? topK : 5;
    }
}
