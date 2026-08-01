package com.finpay.chatbot.controller;

import com.finpay.chatbot.dto.ChatRequest;
import com.finpay.chatbot.dto.ChatResponse;
import com.finpay.chatbot.dto.MessageResponse;
import com.finpay.chatbot.dto.QuickChatRequest;
import com.finpay.chatbot.dto.SessionResponse;
import com.finpay.chatbot.service.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "AI Chatbot API")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/chat")
    @Operation(summary = "Send message to AI chatbot")
    public ResponseEntity<ChatResponse> chat(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatbotService.chat(request, userId));
    }

    @GetMapping("/sessions")
    @Operation(summary = "Get user chat sessions")
    public ResponseEntity<List<SessionResponse>> getSessions(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(chatbotService.getUserSessions(userId));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "Get session message history")
    public ResponseEntity<List<MessageResponse>> getSessionMessages(
            @PathVariable String sessionId,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(chatbotService.getSessionHistory(sessionId, userId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Delete chat session")
    public ResponseEntity<Void> deleteSession(
            @PathVariable String sessionId,
            @RequestHeader("X-User-Id") String userId) {
        chatbotService.deleteSession(sessionId, userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/chat/quick")
    @Operation(summary = "Quick chat without auth")
    public ResponseEntity<Map<String, String>> quickChat(
            @Valid @RequestBody QuickChatRequest request) {
        return ResponseEntity.ok(Map.of(
                "response",
                "Please login to use FinPay Assistant with your account context. "
                        + "General question: " + request.getMessage()));
    }
}
