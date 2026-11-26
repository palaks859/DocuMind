package com.documind.backend.chat.dto;

import com.documind.backend.chat.MessageCitation;
import java.math.BigDecimal;
import java.util.UUID;

public record CitationResponse(
        UUID id,
        UUID documentId,
        String documentFilename,
        UUID chunkId,
        Integer pageNumber,
        Integer pageStart,
        Integer pageEnd,
        String passageReference,
        String quotedText,
        BigDecimal relevanceScore
) {
    public static CitationResponse from(MessageCitation c, String documentFilename) {
        return new CitationResponse(
                c.getId(), c.getDocumentId(), documentFilename, c.getChunkId(),
                c.getPageNumber(), c.getPageStart(), c.getPageEnd(),
                c.getPassageReference(), c.getQuotedText(), c.getRelevanceScore()
        );
    }
}
