package com.documind.backend.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface StorageService {
    StoredFileResult store(UUID workspaceId, UUID documentId, MultipartFile file);
    Resource loadAsResource(String storageKey);
    void delete(String storageKey);
}
