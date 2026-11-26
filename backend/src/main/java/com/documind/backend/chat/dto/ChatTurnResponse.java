package com.documind.backend.chat.dto;

public record ChatTurnResponse(
        ChatMessageResponse userMessage,
        ChatMessageResponse assistantMessage
) {}
