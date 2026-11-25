package com.documind.backend.document;

import com.documind.backend.audit.AuditLogService;
import com.documind.backend.common.exception.ResourceNotFoundException;
import com.documind.backend.document.dto.DocumentResponse;
import com.documind.backend.storage.StorageService;
import com.documind.backend.storage.StoredFileResult;
import com.documind.backend.user.User;
import com.documind.backend.workspace.WorkspaceAuthorizationService;
import com.documind.backend.workspace.WorkspaceMember;
import com.documind.backend.workspace.WorkspaceRole;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );

    private final DocumentRepository documentRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final StorageService storageService;
    private final AuditLogService auditLogService;

    public DocumentService(DocumentRepository documentRepository,
                           WorkspaceAuthorizationService authorizationService,
                           StorageService storageService,
                           AuditLogService auditLogService) {
        this.documentRepository = documentRepository;
        this.authorizationService = authorizationService;
        this.storageService = storageService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public DocumentResponse upload(UUID workspaceId, User uploader, MultipartFile file) {
        WorkspaceMember membership = authorizationService.requireRole(
                workspaceId, uploader.getId(), WorkspaceRole.REVIEWER);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file must not be empty.");
        }

        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType + ". Allowed: PDF, DOCX, TXT.");
        }

        String originalFilename = file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()
                ? file.getOriginalFilename().trim()
                : "untitled";

        UUID documentId = UUID.randomUUID();
        StoredFileResult stored = storageService.store(workspaceId, documentId, file);

        Document document = new Document(
                documentId,
                membership.getWorkspace(),
                uploader,
                originalFilename,
                contentType,
                stored.sizeBytes(),
                stored.storageKey(),
                stored.sha256Checksum()
        );
        document = documentRepository.save(document);

        auditLogService.record(workspaceId, uploader.getId(), "DOCUMENT_UPLOADED",
                "DOCUMENT", document.getId(),
                Map.of("filename", document.getFilename(), "fileSize", document.getFileSize(), "contentType", contentType));

        return DocumentResponse.from(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listDocuments(UUID workspaceId, UUID userId) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.VIEWER);
        return documentRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional
    public DocumentResponse getDocument(UUID workspaceId, UUID documentId, UUID userId) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.VIEWER);
        Document document = documentRepository.findByIdAndWorkspaceId(documentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found in workspace."));

        auditLogService.record(workspaceId, userId, "DOCUMENT_VIEWED",
                "DOCUMENT", document.getId(), Map.of("filename", document.getFilename()));

        return DocumentResponse.from(document);
    }

    @Transactional
    public DownloadedDocument downloadDocument(UUID workspaceId, UUID documentId, UUID userId) {
        authorizationService.requireRole(workspaceId, userId, WorkspaceRole.VIEWER);
        Document document = documentRepository.findByIdAndWorkspaceId(documentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found in workspace."));

        Resource resource = storageService.loadAsResource(document.getStorageKey());

        auditLogService.record(workspaceId, userId, "DOCUMENT_DOWNLOADED",
                "DOCUMENT", document.getId(), Map.of("filename", document.getFilename()));

        return new DownloadedDocument(document, resource);
    }

    @Transactional
    public void deleteDocument(UUID workspaceId, UUID documentId, UUID actorUserId) {
        authorizationService.requireRole(workspaceId, actorUserId, WorkspaceRole.REVIEWER);
        Document document = documentRepository.findByIdAndWorkspaceId(documentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found in workspace."));

        String storageKey = document.getStorageKey();
        String filename = document.getFilename();

        documentRepository.delete(document);
        storageService.delete(storageKey);

        auditLogService.record(workspaceId, actorUserId, "DOCUMENT_DELETED",
                "DOCUMENT", documentId, Map.of("filename", filename));
    }

    public record DownloadedDocument(Document document, Resource resource) {}
}
