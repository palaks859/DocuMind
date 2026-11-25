package com.documind.backend.audit;

import com.documind.backend.audit.dto.AuditLogResponse;
import com.documind.backend.auth.CurrentUserService;
import com.documind.backend.workspace.WorkspaceAuthorizationService;
import com.documind.backend.workspace.WorkspaceRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/audit-logs")
@Tag(name = "Audit Logs", description = "Traceable workspace activity and access logs")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final CurrentUserService currentUserService;

    public AuditLogController(AuditLogRepository auditLogRepository,
                              WorkspaceAuthorizationService authorizationService,
                              CurrentUserService currentUserService) {
        this.auditLogRepository = auditLogRepository;
        this.authorizationService = authorizationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @Operation(summary = "List paginated audit logs for a workspace (ADMIN only)")
    public ResponseEntity<Page<AuditLogResponse>> listAuditLogs(
            @PathVariable UUID workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        authorizationService.requireRole(workspaceId, currentUserService.getCurrentUserId(), WorkspaceRole.ADMIN);
        int boundedSize = Math.min(Math.max(size, 1), 100);
        Page<AuditLogResponse> result = auditLogRepository
                .findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId, PageRequest.of(Math.max(page, 0), boundedSize))
                .map(AuditLogResponse::from);
        return ResponseEntity.ok(result);
    }
}
