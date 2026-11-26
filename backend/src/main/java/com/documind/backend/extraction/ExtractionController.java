package com.documind.backend.extraction;

import com.documind.backend.auth.CurrentUserService;
import com.documind.backend.extraction.dto.ExtractionDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}")
@Tag(name = "Structured Field Extraction", description = "Schema-driven field extraction with source evidence and human review")
public class ExtractionController {

    private final ExtractionService extractionService;
    private final CurrentUserService currentUserService;

    public ExtractionController(ExtractionService extractionService, CurrentUserService currentUserService) {
        this.extractionService = extractionService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/documents/{documentId}/extractions")
    @Operation(summary = "Run structured field extraction on a document (REVIEWER+)")
    public ResponseEntity<ExtractionRunResponse> runExtraction(@PathVariable UUID workspaceId,
                                                               @PathVariable UUID documentId,
                                                               @Valid @RequestBody(required = false) RunExtractionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(extractionService.runExtraction(workspaceId, documentId, currentUserService.getCurrentUserId(), request));
    }

    @GetMapping("/documents/{documentId}/extractions")
    @Operation(summary = "List extraction runs and extracted fields for a document (VIEWER+)")
    public ResponseEntity<List<ExtractionRunResponse>> listRuns(@PathVariable UUID workspaceId,
                                                                @PathVariable UUID documentId) {
        return ResponseEntity.ok(extractionService.listRuns(workspaceId, documentId, currentUserService.getCurrentUserId()));
    }

    @PatchMapping("/extractions/fields/{fieldId}/review")
    @Operation(summary = "Approve, reject, or override an extracted field value (REVIEWER+)")
    public ResponseEntity<ExtractedFieldResponse> reviewField(@PathVariable UUID workspaceId,
                                                              @PathVariable UUID fieldId,
                                                              @Valid @RequestBody ReviewFieldRequest request) {
        return ResponseEntity.ok(extractionService.reviewField(
                workspaceId, fieldId, currentUserService.getCurrentUserId(), request));
    }
}
