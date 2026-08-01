package com.finpay.chatbot.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring AI 0.8.1 auto-configures {@link OpenAiChatClient} (implements {@link ChatClient}).
 * There is no OpenAiChatModel / ChatClient.create(...) API in this version.
 */
@Configuration
@Slf4j
public class SpringAiConfig {

    @Value("${spring.ai.openai.api-key:placeholder}")
    private String apiKey;

    @PostConstruct
    void warnIfPlaceholderKey() {
        if (apiKey == null || apiKey.isBlank() || "placeholder".equals(apiKey)) {
            log.warn("OpenAI API key not configured - chatbot will return fallback responses");
        }
    }

    @Bean
    @Primary
    public ChatClient chatClient(OpenAiChatClient openAiChatClient) {
        return openAiChatClient;
    }
}
