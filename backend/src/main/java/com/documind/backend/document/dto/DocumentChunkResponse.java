package com.documind.backend.document.dto;

import com.documind.backend.document.DocumentChunk;
import java.util.UUID;

public record DocumentChunkResponse(
        UUID id,
        int chunkIndex,
        String content,
        Integer tokenCount,
        Integer pageNumber,
        Integer pageStart,
        Integer pageEnd,
        Integer startOffset,
        Integer endOffset
) {
    public static DocumentChunkResponse from(DocumentChunk c) {
        return new DocumentChunkResponse(
                c.getId(), c.getChunkIndex(), c.getContent(), c.getTokenCount(),
                c.getPageNumber(), c.getPageStart(), c.getPageEnd(),
                c.getStartOffset(), c.getEndOffset()
        );
    }
}
