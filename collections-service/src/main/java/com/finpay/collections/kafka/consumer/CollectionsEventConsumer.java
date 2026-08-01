package com.finpay.collections.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.collections.entity.CollectionAccount;
import com.finpay.collections.kafka.event.PaymentMissedEvent;
import com.finpay.collections.service.CollectionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class CollectionsEventConsumer {

    private final ObjectMapper objectMapper;
    private final CollectionsService collectionsService;

    @KafkaListener(
            topics = "payment-events",
            groupId = "collections-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumePaymentEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            boolean hasTransactionRef = root.hasNonNull("transactionRef");
            String status = root.hasNonNull("status") ? root.get("status").asText("") : "";
            boolean missed = !hasTransactionRef
                    && (status.toUpperCase().contains("FAILED") || status.toUpperCase().contains("OVERDUE"));

            if (!missed) {
                log.debug("Ignoring non-missed payment event: {}", message);
                return;
            }

            PaymentMissedEvent event = PaymentMissedEvent.builder()
                    .loanId(textOrNull(root, "loanId"))
                    .userId(textOrNull(root, "userId"))
                    .userName(textOrNull(root, "userName"))
                    .userEmail(textOrNull(root, "userEmail"))
                    .overdueAmount(decimalOrNull(root, "overdueAmount", "amount"))
                    .overdueDays(intOrDefault(root, "overdueDays", 1))
                    .missedEmis(intOrDefault(root, "missedEmis", 1))
                    .timestamp(LocalDateTime.now())
                    .build();

            if (event.getLoanId() == null || event.getLoanId().isBlank()
                    || event.getUserId() == null || event.getUserId().isBlank()) {
                log.warn("Ignoring missed payment event without loanId/userId: {}", message);
                return;
            }

            CollectionAccount account = collectionsService.createAccount(event);
            log.info("Collection account created/updated id={} loanId={} status={}",
                    account.getId(), account.getLoanId(), account.getStatus());
        } catch (Exception ex) {
            log.error("Failed to process payment-events message={}", message, ex);
        }
    }

    private String textOrNull(JsonNode root, String field) {
        return root.hasNonNull(field) ? root.get(field).asText() : null;
    }

    private BigDecimal decimalOrNull(JsonNode root, String primary, String fallback) {
        if (root.hasNonNull(primary)) {
            return new BigDecimal(root.get(primary).asText());
        }
        if (root.hasNonNull(fallback)) {
            return new BigDecimal(root.get(fallback).asText());
        }
        return BigDecimal.ZERO;
    }

    private int intOrDefault(JsonNode root, String field, int defaultValue) {
        return root.hasNonNull(field) ? root.get(field).asInt(defaultValue) : defaultValue;
    }
}
