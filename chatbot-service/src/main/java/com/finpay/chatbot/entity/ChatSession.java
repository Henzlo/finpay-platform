package com.finpay.chatbot.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "chat_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    @Id
    private String id;

    private String userId;

    private String title;

    private LocalDateTime createdAt;

    private LocalDateTime lastMessageAt;

    @Builder.Default
    private boolean isActive = true;

    @Builder.Default
    private int messageCount = 0;
}
