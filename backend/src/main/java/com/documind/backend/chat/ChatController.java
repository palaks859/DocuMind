package com.documind.backend.chat;

import com.documind.backend.auth.CurrentUserService;
import com.documind.backend.chat.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/chat")
@Tag(name = "Grounded RAG Chat", description = "Evidence-grounded document Q&A sessions with verifiable citations")
public class ChatController {

    private final ChatService chatService;
    private final CurrentUserService currentUserService;

    public ChatController(ChatService chatService, CurrentUserService currentUserService) {
        this.chatService = chatService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/sessions")
    @Operation(summary = "Create a new chat session in a workspace (VIEWER+)")
    public ResponseEntity<ChatSessionResponse> createSession(@PathVariable UUID workspaceId,
                                                             @Valid @RequestBody(required = false) CreateSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.createSession(workspaceId, currentUserService.getCurrentUserId(), request));
    }

    @GetMapping("/sessions")
    @Operation(summary = "List user's chat sessions in a workspace (VIEWER+)")
    public ResponseEntity<List<ChatSessionResponse>> listSessions(@PathVariable UUID workspaceId) {
        return ResponseEntity.ok(chatService.listSessions(workspaceId, currentUserService.getCurrentUserId()));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "Get full message history and citations for a session (VIEWER+)")
    public ResponseEntity<List<ChatMessageResponse>> listMessages(@PathVariable UUID workspaceId,
                                                                  @PathVariable UUID sessionId) {
        return ResponseEntity.ok(chatService.listMessages(
                workspaceId, sessionId, currentUserService.getCurrentUserId()));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "Ask a question grounded strictly in workspace documents (VIEWER+)")
    public ResponseEntity<ChatTurnResponse> askQuestion(@PathVariable UUID workspaceId,
                                                        @PathVariable UUID sessionId,
                                                        @Valid @RequestBody AskQuestionRequest request) {
        return ResponseEntity.ok(chatService.askQuestion(
                workspaceId, sessionId, currentUserService.getCurrentUserId(), request));
    }

    @PatchMapping("/messages/{messageId}/feedback")
    @Operation(summary = "Submit HELPFUL or NOT_HELPFUL feedback on an assistant answer (VIEWER+)")
    public ResponseEntity<ChatMessageResponse> submitFeedback(@PathVariable UUID workspaceId,
                                                              @PathVariable UUID messageId,
                                                              @Valid @RequestBody MessageFeedbackRequest request) {
        return ResponseEntity.ok(chatService.submitFeedback(
                workspaceId, messageId, currentUserService.getCurrentUserId(), request));
    }
}
