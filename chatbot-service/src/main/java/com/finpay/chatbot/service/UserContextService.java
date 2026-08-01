package com.finpay.chatbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserContextService {

    public String buildUserContext(String userId) {
        return """
                User ID: %s
                Note: User account data would be fetched
                from loan-service and payment-service here.
                For now, provide general assistance and
                direct users to check their dashboard
                for specific account details.
                """.formatted(userId);
    }
}
