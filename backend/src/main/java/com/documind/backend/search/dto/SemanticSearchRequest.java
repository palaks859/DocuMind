package com.documind.backend.search.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record SemanticSearchRequest(
        @NotBlank @Size(max = 2000) String query,
        @Min(1) @Max(25) Integer topK,
        List<UUID> documentIds
) {
    public int resolvedTopK() {
        return topK != null ? topK : 5;
    }
}
