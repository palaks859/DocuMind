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

    public record StorageIngestPayload(
            String storage_key,
            String filename,
            String content_type
    ) {}

    public record IngestResult(
            String filename,
            String content_type,
            int page_count,
            int total_chunks,
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
            Integer end_offset
    ) {}
}
