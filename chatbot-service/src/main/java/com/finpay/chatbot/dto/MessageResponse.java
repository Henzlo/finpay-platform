package com.finpay.chatbot.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageResponse {

    private String id;
    private String role;
    private String content;
    private LocalDateTime timestamp;
}
