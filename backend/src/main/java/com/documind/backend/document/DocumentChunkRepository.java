package com.documind.backend.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {
    List<DocumentChunk> findAllByDocumentIdAndWorkspaceIdOrderByChunkIndexAsc(UUID documentId, UUID workspaceId);
    long countByDocumentId(UUID documentId);
}
