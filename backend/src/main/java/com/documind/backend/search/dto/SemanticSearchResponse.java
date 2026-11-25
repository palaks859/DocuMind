package com.documind.backend.search.dto;

import com.documind.backend.document.DocumentChunkVectorRepository;
import java.util.List;
import java.util.UUID;

public record SemanticSearchResponse(
        String query,
        int totalHits,
        List<SearchHitDto> hits
) {
    public record SearchHitDto(
            UUID chunkId,
            UUID documentId,
            String documentFilename,
            int chunkIndex,
            Integer pageNumber,
            Integer pageStart,
            Integer pageEnd,
            String content,
            double similarityScore
    ) {
        public static SearchHitDto from(DocumentChunkVectorRepository.SemanticChunkHit h) {
            return new SearchHitDto(
                    h.chunkId(), h.documentId(), h.documentFilename(),
                    h.chunkIndex(), h.pageNumber(), h.pageStart(), h.pageEnd(),
                    h.content(), Math.round(h.similarityScore() * 10000.0) / 10000.0
            );
        }
    }
}
