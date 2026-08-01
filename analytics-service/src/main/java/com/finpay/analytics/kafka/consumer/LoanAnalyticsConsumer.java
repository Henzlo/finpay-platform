package com.finpay.analytics.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.analytics.store.AnalyticsStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoanAnalyticsConsumer {

    private final ObjectMapper objectMapper;
    private final AnalyticsStore analyticsStore;

    @KafkaListener(
            topics = "loan-events",
            groupId = "analytics-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeLoanEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            log.info("Consumed loan-events message={}", message);

            String status = resolveLoanStatus(root);
            analyticsStore.incrementLoans();

            if ("SUBMITTED".equalsIgnoreCase(status)) {
                analyticsStore.incrementPending();
            } else if ("APPROVED".equalsIgnoreCase(status)) {
                analyticsStore.decrementPending();
                analyticsStore.incrementActive();
            }

            log.info("Processed loan analytics event status={} loanId={}",
                    status, root.path("loanId").asText(null));
        } catch (Exception ex) {
            log.error("Failed to process loan-events message={}", message, ex);
        }
    }

    /**
     * Prefer explicit status when present; otherwise infer from producer event shapes
     * (LoanApplied / LoanApproved / LoanRejected) used by loan-service.
     */
    private String resolveLoanStatus(JsonNode root) {
        if (root.hasNonNull("status")) {
            return root.get("status").asText();
        }
        if (root.hasNonNull("monthlyEmi")) {
            return "APPROVED";
        }
        if (root.hasNonNull("reason") || root.hasNonNull("rejectionReason")) {
            return "REJECTED";
        }
        return "SUBMITTED";
    }
}
