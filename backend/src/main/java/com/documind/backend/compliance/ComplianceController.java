package com.documind.backend.compliance;

import com.documind.backend.auth.CurrentUserService;
import com.documind.backend.compliance.dto.ComplianceDtos.*;
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
@Tag(name = "Compliance Checklists", description = "Workspace compliance checklists, automated evaluation runs, and human review")
public class ComplianceController {

    private final ComplianceService complianceService;
    private final CurrentUserService currentUserService;

    public ComplianceController(ComplianceService complianceService, CurrentUserService currentUserService) {
        this.complianceService = complianceService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/checklists")
    @Operation(summary = "Create a workspace compliance checklist (REVIEWER+)")
    public ResponseEntity<ChecklistResponse> createChecklist(@PathVariable UUID workspaceId,
                                                             @Valid @RequestBody CreateChecklistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(complianceService.createChecklist(workspaceId, currentUserService.getCurrentUserId(), request));
    }

    @GetMapping("/checklists")
    @Operation(summary = "List workspace compliance checklists (VIEWER+)")
    public ResponseEntity<List<ChecklistResponse>> listChecklists(@PathVariable UUID workspaceId) {
        return ResponseEntity.ok(complianceService.listChecklists(workspaceId, currentUserService.getCurrentUserId()));
    }

    @PostMapping("/documents/{documentId}/compliance-runs")
    @Operation(summary = "Evaluate a document against a compliance checklist (REVIEWER+)")
    public ResponseEntity<ComplianceRunResponse> runCompliance(@PathVariable UUID workspaceId,
                                                               @PathVariable UUID documentId,
                                                               @Valid @RequestBody RunComplianceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(complianceService.runComplianceEvaluation(workspaceId, documentId, currentUserService.getCurrentUserId(), request));
    }

    @GetMapping("/documents/{documentId}/compliance-runs")
    @Operation(summary = "List compliance runs and item results for a document (VIEWER+)")
    public ResponseEntity<List<ComplianceRunResponse>> listComplianceRuns(@PathVariable UUID workspaceId,
                                                                          @PathVariable UUID documentId) {
        return ResponseEntity.ok(complianceService.listComplianceRuns(workspaceId, documentId, currentUserService.getCurrentUserId()));
    }

    @PatchMapping("/compliance/results/{resultId}/review")
    @Operation(summary = "Override or confirm a compliance evaluation outcome (REVIEWER+)")
    public ResponseEntity<ComplianceResultResponse> reviewResult(@PathVariable UUID workspaceId,
                                                                 @PathVariable UUID resultId,
                                                                 @Valid @RequestBody ReviewComplianceResultRequest request) {
        return ResponseEntity.ok(complianceService.reviewResult(
                workspaceId, resultId, currentUserService.getCurrentUserId(), request));
    }
}
