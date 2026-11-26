package com.documind.backend.document;

import com.documind.backend.config.AiServiceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.List;

@Component
@EnableConfigurationProperties(AiServiceProperties.class)
public class AiServiceClient {

    private final RestClient restClient;

    public AiServiceClient(AiServiceProperties properties) {
        HttpClient http1Client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(new JdkClientHttpRequestFactory(http1Client))
                .build();
    }

    public IngestResult ingestFromStorage(String storageKey, String filename, String contentType) {
        StorageIngestPayload payload = new StorageIngestPayload(storageKey, filename, contentType);
        return restClient.post()
                .uri("/api/v1/ingest/from-storage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(IngestResult.class);
    }

    public List<Double> embedQuery(String query) {
        EmbedPayload payload = new EmbedPayload(List.of(query), true);
        EmbedResult result = restClient.post()
                .uri("/api/v1/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(EmbedResult.class);
        if (result == null || result.embeddings() == null || result.embeddings().isEmpty()) {
            throw new IllegalStateException("AI service returned empty query embedding.");
        }
        return result.embeddings().get(0);
    }

    public RagAnswerResult generateGroundedAnswer(String question,
                                                  List<RagContextChunkPayload> chunks,
                                                  List<RagHistoryTurnPayload> history) {
        RagAnswerPayload payload = new RagAnswerPayload(question, chunks, history, null);
        return restClient.post()
                .uri("/api/v1/rag/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(RagAnswerResult.class);
    }

    public FieldExtractResult extractFields(String schemaName,
                                            String documentFilename,
                                            List<FieldSpecPayload> fields,
                                            List<DocChunkPayload> chunks) {
        FieldExtractPayload payload = new FieldExtractPayload(schemaName, documentFilename, fields, chunks);
        return restClient.post()
                .uri("/api/v1/extract/fields")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(FieldExtractResult.class);
    }

    public ComplianceEvaluateResult evaluateCompliance(String documentFilename,
                                                       List<ChecklistCriterionPayload> criteria,
                                                       List<DocChunkPayload> chunks) {
        ComplianceEvaluatePayload payload = new ComplianceEvaluatePayload(documentFilename, criteria, chunks);
        return restClient.post()
                .uri("/api/v1/compliance/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(ComplianceEvaluateResult.class);
    }

    public record StorageIngestPayload(String storage_key, String filename, String content_type) {}

    public record IngestResult(
            String filename,
            String content_type,
            int page_count,
            int total_chunks,
            String embedding_model,
            String embedding_model_revision,
            int embedding_dimension,
            List<ChunkItem> chunks
    ) {}

    public record ChunkItem(
            int chunk_index,
            String content,
            Integer token_count,
            Integer page_number,
            Integer page_start,
            Integer page_end,
            Integer start_offset,
            Integer end_offset,
            List<Double> embedding
    ) {}

    public record EmbedPayload(List<String> texts, boolean is_query) {}

    public record EmbedResult(String model, String revision, int dimension, List<List<Double>> embeddings) {}

    public record RagContextChunkPayload(
            String chunk_id,
            String document_id,
            String document_filename,
            int chunk_index,
            Integer page_number,
            Integer page_start,
            Integer page_end,
            String content,
            double similarity_score
    ) {}

    public record RagHistoryTurnPayload(String role, String content) {}

    public record RagAnswerPayload(
            String question,
            List<RagContextChunkPayload> chunks,
            List<RagHistoryTurnPayload> history,
            Double min_similarity
    ) {}

    public record RagCitationItem(
            String chunk_id,
            String document_id,
            String document_filename,
            Integer page_number,
            Integer page_start,
            Integer page_end,
            String passage_reference,
            String quoted_text,
            double relevance_score
    ) {}

    public record RagAnswerResult(
            String answer,
            boolean found_answer,
            String prompt_version,
            String model_provider,
            String model_name,
            List<RagCitationItem> citations
    ) {}

    public record DocChunkPayload(
            String chunk_id,
            int chunk_index,
            Integer page_number,
            Integer page_start,
            Integer page_end,
            String content
    ) {}

    public record FieldSpecPayload(String field_name, String description) {}

    public record FieldExtractPayload(
            String schema_name,
            String document_filename,
            List<FieldSpecPayload> fields,
            List<DocChunkPayload> chunks
    ) {}

    public record ExtractedFieldItem(
            String field_name,
            Object field_value,
            String source_chunk_id,
            String evidence,
            Integer page_number,
            Integer page_start,
            Integer page_end,
            double confidence
    ) {}

    public record FieldExtractResult(
            String schema_name,
            String prompt_version,
            String model_provider,
            String model_name,
            List<ExtractedFieldItem> fields
    ) {}

    public record ChecklistCriterionPayload(String item_id, String criterion, String description) {}

    public record ComplianceEvaluatePayload(
            String document_filename,
            List<ChecklistCriterionPayload> criteria,
            List<DocChunkPayload> chunks
    ) {}

    public record ComplianceItemResultItem(
            String checklist_item_id,
            String source_chunk_id,
            String outcome,
            String evidence,
            String explanation
    ) {}

    public record ComplianceEvaluateResult(
            String prompt_version,
            String model_provider,
            String model_name,
            List<ComplianceItemResultItem> results
    ) {}
}
