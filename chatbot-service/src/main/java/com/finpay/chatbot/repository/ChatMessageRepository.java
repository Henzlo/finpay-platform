package com.finpay.chatbot.repository;

import com.finpay.chatbot.entity.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);

    List<ChatMessage> findByUserIdOrderByTimestampDesc(String userId);

    long countBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);
}
