package com.finpay.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatResponse {

    private String sessionId;
    private String messageId;
    private String response;

    @Builder.Default
    private String role = "ASSISTANT";

    private LocalDateTime timestamp;

    @JsonProperty("isNewSession")
    private boolean isNewSession;
}
