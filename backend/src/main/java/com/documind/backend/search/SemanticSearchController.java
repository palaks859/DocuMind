package com.documind.backend.search;

import com.documind.backend.auth.CurrentUserService;
import com.documind.backend.search.dto.SemanticSearchRequest;
import com.documind.backend.search.dto.SemanticSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/search")
@Tag(name = "Semantic Search", description = "pgvector cosine similarity retrieval over workspace document chunks")
public class SemanticSearchController {

    private final SemanticSearchService searchService;
    private final CurrentUserService currentUserService;

    public SemanticSearchController(SemanticSearchService searchService, CurrentUserService currentUserService) {
        this.searchService = searchService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @Operation(summary = "Run semantic vector search across workspace documents (VIEWER+)")
    public ResponseEntity<SemanticSearchResponse> search(@PathVariable UUID workspaceId,
                                                         @Valid @RequestBody SemanticSearchRequest request) {
        return ResponseEntity.ok(searchService.search(workspaceId, currentUserService.getCurrentUserId(), request));
    }
}
