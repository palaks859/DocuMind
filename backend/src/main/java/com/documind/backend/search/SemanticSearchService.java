package com.documind.backend.search;

import com.documind.backend.audit.AuditLogService;
import com.documind.backend.document.AiServiceClient;
import com.documind.backend.document.DocumentChunkVectorRepository;
import com.documind.backend.search.dto.SemanticSearchRequest;
import com.documind.backend.search.dto.SemanticSearchResponse;
import com.documind.backend.workspace.WorkspaceAuthorizationService;
import com.documind.backend.workspace.WorkspaceRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SemanticSearchService {

    private final WorkspaceAuthorizationService authorizationService;
    private final AiServiceClient aiServiceClient;
    private final DocumentChunkVectorRepository vectorRepository;
    private final AuditLogService auditLogService;

    public SemanticSearchService(WorkspaceAuthorizationService authorizationService,
                                 AiServiceClient aiServiceClient,
                                 DocumentChunkVectorRepository vectorRepository,
                                 AuditLogService auditLogService) {
        this.authorizationService = authorizationService;
        this.aiServiceClient = aiServiceClient;
        this.vectorRepository = vectorRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public SemanticSearchResponse search(UUID workspaceId, UUID userId, SemanticSearchRequest request) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.VIEWER);

        List<Double> queryVec = aiServiceClient.embedQuery(request.query());
        List<DocumentChunkVectorRepository.SemanticChunkHit> hits = vectorRepository.searchSimilarChunks(
                workspaceId,
                queryVec,
                request.resolvedTopK(),
                request.documentIds()
        );

        auditLogService.record(workspaceId, userId, "SEMANTIC_SEARCH_EXECUTED",
                "WORKSPACE", workspaceId,
                Map.of("queryLength", request.query().length(), "hits", hits.size()));

        List<SemanticSearchResponse.SearchHitDto> dtoHits = hits.stream()
                .map(SemanticSearchResponse.SearchHitDto::from)
                .toList();

        return new SemanticSearchResponse(request.query(), dtoHits.size(), dtoHits);
    }
}
