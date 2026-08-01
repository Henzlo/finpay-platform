package com.finpay.chatbot.service;

import com.finpay.chatbot.dto.ChatRequest;
import com.finpay.chatbot.dto.ChatResponse;
import com.finpay.chatbot.dto.MessageResponse;
import com.finpay.chatbot.dto.SessionResponse;
import com.finpay.chatbot.entity.ChatMessage;
import com.finpay.chatbot.entity.ChatSession;
import com.finpay.chatbot.exception.SessionNotFoundException;
import com.finpay.chatbot.exception.UnauthorizedException;
import com.finpay.chatbot.repository.ChatMessageRepository;
import com.finpay.chatbot.repository.ChatSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ChatbotService {

    private static final String FALLBACK_RESPONSE =
            "I'm FinPay Assistant! Your OpenAI API key "
                    + "is not configured. Please set OPENAI_API_KEY "
                    + "environment variable. I can help you with "
                    + "loans, EMIs, and payments once configured!";

    private final ChatClient chatClient;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final UserContextService userContextService;
    private final String systemPrompt;
    private final String openAiApiKey;

    public ChatbotService(
            ChatClient chatClient,
            ChatMessageRepository chatMessageRepository,
            ChatSessionRepository chatSessionRepository,
            UserContextService userContextService,
            @Value("${chatbot.system-prompt}") String systemPrompt,
            @Value("${spring.ai.openai.api-key:placeholder}") String openAiApiKey) {
        this.chatClient = chatClient;
        this.chatMessageRepository = chatMessageRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.userContextService = userContextService;
        this.systemPrompt = systemPrompt;
        this.openAiApiKey = openAiApiKey;
    }

    public ChatResponse chat(ChatRequest request, String userId) {
        LocalDateTime now = LocalDateTime.now();
        boolean isNewSession = false;
        ChatSession session;

        if (!StringUtils.hasText(request.getSessionId())) {
            session = createSession(userId, request.getMessage(), now);
            isNewSession = true;
        } else {
            session = chatSessionRepository.findByIdAndUserId(request.getSessionId(), userId)
                    .orElseGet(() -> {
                        log.warn("Session {} not found for user {}, creating new session",
                                request.getSessionId(), userId);
                        return createSession(userId, request.getMessage(), now);
                    });
            if (!request.getSessionId().equals(session.getId())) {
                isNewSession = true;
            }
        }

        ChatMessage userMessage = chatMessageRepository.save(ChatMessage.builder()
                .sessionId(session.getId())
                .userId(userId)
                .role("USER")
                .content(request.getMessage())
                .timestamp(now)
                .build());

        List<ChatMessage> history = chatMessageRepository
                .findBySessionIdOrderByTimestampAsc(session.getId());
        if (history.size() > 10) {
            history = history.subList(history.size() - 10, history.size());
        }

        String aiResponse = generateAiResponse(userId, history);

        LocalDateTime assistantTime = LocalDateTime.now();
        ChatMessage assistantMessage = chatMessageRepository.save(ChatMessage.builder()
                .sessionId(session.getId())
                .userId(userId)
                .role("ASSISTANT")
                .content(aiResponse)
                .timestamp(assistantTime)
                .build());

        session.setLastMessageAt(assistantTime);
        session.setMessageCount(session.getMessageCount() + 1);
        chatSessionRepository.save(session);

        log.info("Chat completed sessionId={} userId={} userMessageId={} assistantMessageId={}",
                session.getId(), userId, userMessage.getId(), assistantMessage.getId());

        return ChatResponse.builder()
                .sessionId(session.getId())
                .messageId(assistantMessage.getId())
                .response(aiResponse)
                .role("ASSISTANT")
                .timestamp(assistantTime)
                .isNewSession(isNewSession)
                .build();
    }

    public List<MessageResponse> getSessionHistory(String sessionId, String userId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + sessionId));
        if (!userId.equals(session.getUserId())) {
            throw new UnauthorizedException("Not authorized to access this session");
        }

        return chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId).stream()
                .map(msg -> MessageResponse.builder()
                        .id(msg.getId())
                        .role(msg.getRole())
                        .content(msg.getContent())
                        .timestamp(msg.getTimestamp())
                        .build())
                .toList();
    }

    public List<SessionResponse> getUserSessions(String userId) {
        return chatSessionRepository.findByUserIdOrderByLastMessageAtDesc(userId).stream()
                .map(session -> SessionResponse.builder()
                        .id(session.getId())
                        .userId(session.getUserId())
                        .title(session.getTitle())
                        .createdAt(session.getCreatedAt())
                        .lastMessageAt(session.getLastMessageAt())
                        .messageCount(session.getMessageCount())
                        .build())
                .toList();
    }

    public void deleteSession(String sessionId, String userId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + sessionId));
        if (!userId.equals(session.getUserId())) {
            throw new UnauthorizedException("Not authorized to delete this session");
        }
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.delete(session);
        log.info("Deleted chat sessionId={} for userId={}", sessionId, userId);
    }

    private ChatSession createSession(String userId, String message, LocalDateTime now) {
        String title = message == null ? "" : message;
        if (title.length() > 50) {
            title = title.substring(0, 50);
        }
        ChatSession session = ChatSession.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .title(title)
                .createdAt(now)
                .lastMessageAt(now)
                .isActive(true)
                .messageCount(0)
                .build();
        return chatSessionRepository.save(session);
    }

    private String generateAiResponse(String userId, List<ChatMessage> history) {
        if (openAiApiKey == null || openAiApiKey.isBlank() || "placeholder".equals(openAiApiKey)) {
            return FALLBACK_RESPONSE;
        }

        try {
            String userContext = userContextService.buildUserContext(userId);
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt + "\n\n" + userContext));

            for (ChatMessage msg : history) {
                if ("ASSISTANT".equalsIgnoreCase(msg.getRole())) {
                    messages.add(new AssistantMessage(msg.getContent()));
                } else {
                    messages.add(new UserMessage(msg.getContent()));
                }
            }

            org.springframework.ai.chat.ChatResponse response =
                    chatClient.call(new Prompt(messages));
            return response.getResult().getOutput().getContent();
        } catch (Exception ex) {
            log.error("OpenAI call failed for userId={}", userId, ex);
            throw ex;
        }
    }
}
