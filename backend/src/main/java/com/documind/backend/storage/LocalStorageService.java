package com.documind.backend.storage;

import com.documind.backend.common.exception.ResourceNotFoundException;
import com.documind.backend.config.StorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
@EnableConfigurationProperties(StorageProperties.class)
public class LocalStorageService implements StorageService {

    private final Path rootLocation;

    public LocalStorageService(StorageProperties storageProperties) {
        this.rootLocation = Paths.get(storageProperties.localRoot()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize local storage root directory", e);
        }
    }

    @Override
    public StoredFileResult store(UUID workspaceId, UUID documentId, MultipartFile file) {
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String safeFilename = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safeFilename.length() > 180) {
            safeFilename = safeFilename.substring(safeFilename.length() - 180);
        }

        String storageKey = workspaceId + "/" + documentId + "_" + safeFilename;
        Path targetPath = resolveSafePath(storageKey);

        try {
            Files.createDirectories(targetPath.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = file.getInputStream();
                 DigestInputStream digestStream = new DigestInputStream(inputStream, digest)) {
                Files.copy(digestStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            String checksum = HexFormat.of().formatHex(digest.digest());
            return new StoredFileResult(storageKey, checksum, file.getSize());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to store uploaded file", e);
        }
    }

    @Override
    public Resource loadAsResource(String storageKey) {
        try {
            Path filePath = resolveSafePath(storageKey);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Stored document file could not be found.");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Invalid storage key path.");
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path filePath = resolveSafePath(storageKey);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete stored file: " + storageKey, e);
        }
    }

    private Path resolveSafePath(String storageKey) {
        Path resolved = this.rootLocation.resolve(storageKey).normalize();
        if (!resolved.startsWith(this.rootLocation)) {
            throw new IllegalArgumentException("Invalid storage path traversal attempt.");
        }
        return resolved;
    }
}
