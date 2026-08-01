package com.finpay.chatbot.repository;

import com.finpay.chatbot.entity.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {

    List<ChatSession> findByUserIdOrderByLastMessageAtDesc(String userId);

    Optional<ChatSession> findByIdAndUserId(String id, String userId);
}
