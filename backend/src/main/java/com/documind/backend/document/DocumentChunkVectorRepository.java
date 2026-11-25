package com.documind.backend.document;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class DocumentChunkVectorRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public void insertChunkWithEmbedding(
            UUID id,
            UUID documentId,
            UUID workspaceId,
            int chunkIndex,
            String content,
            Integer tokenCount,
            Integer pageNumber,
            Integer pageStart,
            Integer pageEnd,
            Integer startOffset,
            Integer endOffset,
            List<Double> embedding,
            String embeddingModel,
            String embeddingModelRevision
    ) {
        String vectorLiteral = toPgVectorLiteral(embedding);
        entityManager.createNativeQuery("""
                INSERT INTO document_chunks (
                    id, document_id, workspace_id, chunk_index, content, token_count,
                    page_number, page_start, page_end, start_offset, end_offset,
                    embedding, embedding_model, embedding_model_revision, created_at
                ) VALUES (
                    :id, :documentId, :workspaceId, :chunkIndex, :content, :tokenCount,
                    :pageNumber, :pageStart, :pageEnd, :startOffset, :endOffset,
                    CAST(:embedding AS vector), :embeddingModel, :embeddingModelRevision, :createdAt
                )
                """)
                .setParameter("id", id)
                .setParameter("documentId", documentId)
                .setParameter("workspaceId", workspaceId)
                .setParameter("chunkIndex", chunkIndex)
                .setParameter("content", content)
                .setParameter("tokenCount", tokenCount)
                .setParameter("pageNumber", pageNumber)
                .setParameter("pageStart", pageStart)
                .setParameter("pageEnd", pageEnd)
                .setParameter("startOffset", startOffset)
                .setParameter("endOffset", endOffset)
                .setParameter("embedding", vectorLiteral)
                .setParameter("embeddingModel", embeddingModel)
                .setParameter("embeddingModelRevision", embeddingModelRevision)
                .setParameter("createdAt", Timestamp.from(Instant.now()))
                .executeUpdate();
    }

    @SuppressWarnings("unchecked")
    public List<SemanticChunkHit> searchSimilarChunks(
            UUID workspaceId,
            List<Double> queryEmbedding,
            int topK,
            List<UUID> documentIds
    ) {
        String vectorLiteral = toPgVectorLiteral(queryEmbedding);
        boolean filterByDocs = documentIds != null && !documentIds.isEmpty();

        String sql = """
                SELECT
                    c.id,
                    c.document_id,
                    d.filename,
                    c.chunk_index,
                    c.page_number,
                    c.page_start,
                    c.page_end,
                    c.content,
                    1.0 - (c.embedding <=> CAST(:queryVec AS vector)) AS similarity
                FROM document_chunks c
                JOIN documents d ON d.id = c.document_id AND d.workspace_id = c.workspace_id
                WHERE c.workspace_id = :workspaceId
                  AND d.status = 'READY'
                  AND c.embedding IS NOT NULL
                """
                + (filterByDocs ? " AND c.document_id IN (:documentIds) " : "")
                + """
                ORDER BY c.embedding <=> CAST(:queryVec AS vector) ASC
                LIMIT :topK
                """;

        var query = entityManager.createNativeQuery(sql)
                .setParameter("workspaceId", workspaceId)
                .setParameter("queryVec", vectorLiteral)
                .setParameter("topK", topK);

        if (filterByDocs) {
            query.setParameter("documentIds", documentIds);
        }

        List<Object[]> rows = query.getResultList();
        List<SemanticChunkHit> results = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            results.add(new SemanticChunkHit(
                    (UUID) row[0],
                    (UUID) row[1],
                    (String) row[2],
                    ((Number) row[3]).intValue(),
                    row[4] != null ? ((Number) row[4]).intValue() : null,
                    row[5] != null ? ((Number) row[5]).intValue() : null,
                    row[6] != null ? ((Number) row[6]).intValue() : null,
                    (String) row[7],
                    ((Number) row[8]).doubleValue()
            ));
        }
        return results;
    }

    public static String toPgVectorLiteral(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .map(v -> Float.toString(v.floatValue()))
                .collect(Collectors.joining(",", "[", "]"));
    }

    public record SemanticChunkHit(
            UUID chunkId,
            UUID documentId,
            String documentFilename,
            int chunkIndex,
            Integer pageNumber,
            Integer pageStart,
            Integer pageEnd,
            String content,
            double similarityScore
    ) {}
}
