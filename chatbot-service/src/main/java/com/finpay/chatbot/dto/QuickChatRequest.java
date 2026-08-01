package com.finpay.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QuickChatRequest {

    @NotBlank
    private String message;
}
