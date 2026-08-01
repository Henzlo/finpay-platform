package com.finpay.analytics.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.analytics.store.AnalyticsStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentAnalyticsConsumer {

    private final ObjectMapper objectMapper;
    private final AnalyticsStore analyticsStore;

    @KafkaListener(
            topics = "payment-events",
            groupId = "analytics-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumePaymentEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            log.info("Consumed payment-events message={}", message);

            BigDecimal amount = root.hasNonNull("amount")
                    ? new BigDecimal(root.get("amount").asText())
                    : BigDecimal.ZERO;
            String status = resolvePaymentStatus(root);

            if ("SUCCESS".equalsIgnoreCase(status)) {
                analyticsStore.addPayment(amount);
            }

            log.info("Processed payment analytics event status={} paymentId={}",
                    status, root.path("paymentId").asText(null));
        } catch (Exception ex) {
            log.error("Failed to process payment-events message={}", message, ex);
        }
    }

    /**
     * Prefer explicit status when present; otherwise treat presence of transactionRef
     * as success (PaymentSuccessEvent from payment-service).
     */
    private String resolvePaymentStatus(JsonNode root) {
        if (root.hasNonNull("status")) {
            return root.get("status").asText();
        }
        if (root.hasNonNull("transactionRef")) {
            return "SUCCESS";
        }
        return "FAILED";
    }
}
