package com.documind.backend.storage;

public record StoredFileResult(
        String storageKey,
        String sha256Checksum,
        long sizeBytes
) {}
