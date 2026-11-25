package com.documind.backend.document;

import com.documind.backend.auth.CurrentUserService;
import com.documind.backend.document.dto.DocumentResponse;
import com.documind.backend.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/documents")
@Tag(name = "Documents", description = "Workspace document upload, metadata listing, download, and deletion")
public class DocumentController {

    private final DocumentService documentService;
    private final CurrentUserService currentUserService;

    public DocumentController(DocumentService documentService, CurrentUserService currentUserService) {
        this.documentService = documentService;
        this.currentUserService = currentUserService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document (PDF, DOCX, TXT) into a workspace (REVIEWER+)")
    public ResponseEntity<DocumentResponse> upload(@PathVariable UUID workspaceId,
                                                   @RequestPart("file") MultipartFile file) {
        User uploader = currentUserService.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.upload(workspaceId, uploader, file));
    }

    @GetMapping
    @Operation(summary = "List all documents in a workspace (VIEWER+)")
    public ResponseEntity<List<DocumentResponse>> list(@PathVariable UUID workspaceId) {
        return ResponseEntity.ok(documentService.listDocuments(workspaceId, currentUserService.getCurrentUserId()));
    }

    @GetMapping("/{documentId}")
    @Operation(summary = "Get document metadata (VIEWER+)")
    public ResponseEntity<DocumentResponse> get(@PathVariable UUID workspaceId, @PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.getDocument(
                workspaceId, documentId, currentUserService.getCurrentUserId()));
    }

    @GetMapping("/{documentId}/content")
    @Operation(summary = "Download or view raw document file content (VIEWER+)")
    public ResponseEntity<Resource> download(@PathVariable UUID workspaceId, @PathVariable UUID documentId) {
        DocumentService.DownloadedDocument downloaded = documentService.downloadDocument(
                workspaceId, documentId, currentUserService.getCurrentUserId());
        Document doc = downloaded.document();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getFilename().replace("\"", "_") + "\"")
                .body(downloaded.resource());
    }

    @DeleteMapping("/{documentId}")
    @Operation(summary = "Delete a document and its stored file (REVIEWER+)")
    public ResponseEntity<Void> delete(@PathVariable UUID workspaceId, @PathVariable UUID documentId) {
        documentService.deleteDocument(workspaceId, documentId, currentUserService.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}
