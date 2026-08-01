package com.finpay.credit.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.credit.kafka.event.PaymentEvent;
import com.finpay.credit.service.CreditScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentCreditConsumer {

    private final ObjectMapper objectMapper;
    private final CreditScoringService creditScoringService;

    @KafkaListener(
            topics = "payment-events",
            groupId = "credit-scoring-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumePaymentEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            PaymentEvent event = objectMapper.treeToValue(root, PaymentEvent.class);

            String userId = event.getUserId();
            if (userId == null || userId.isBlank()) {
                log.warn("Ignoring payment-events message without userId: {}", message);
                return;
            }

            if (root.hasNonNull("transactionRef")) {
                creditScoringService.updatePaymentHistory(userId, true);
                log.info("Credit improved for successful payment userId={} paymentId={}",
                        userId, event.getPaymentId());
            } else {
                creditScoringService.updatePaymentHistory(userId, false);
                log.info("Credit impacted for failed payment userId={} paymentId={}",
                        userId, event.getPaymentId());
            }
        } catch (Exception ex) {
            log.error("Failed to process payment-events message={}", message, ex);
        }
    }
}
