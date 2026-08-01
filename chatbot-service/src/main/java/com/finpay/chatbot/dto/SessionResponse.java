package com.finpay.chatbot.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SessionResponse {

    private String id;
    private String userId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
    private int messageCount;
}
