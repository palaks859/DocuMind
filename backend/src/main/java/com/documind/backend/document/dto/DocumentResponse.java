package com.documind.backend.document.dto;

import com.documind.backend.document.Document;
import com.documind.backend.document.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID workspaceId,
        UUID uploadedBy,
        String filename,
        String contentType,
        long fileSize,
        String checksumSha256,
        Integer pageCount,
        DocumentStatus status,
        String failureMessage,
        Instant createdAt,
        Instant updatedAt
) {
    public static DocumentResponse from(Document doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getWorkspace().getId(),
                doc.getUploadedBy() != null ? doc.getUploadedBy().getId() : null,
                doc.getFilename(),
                doc.getContentType(),
                doc.getFileSize(),
                doc.getChecksumSha256(),
                doc.getPageCount(),
                doc.getStatus(),
                doc.getFailureMessage(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
