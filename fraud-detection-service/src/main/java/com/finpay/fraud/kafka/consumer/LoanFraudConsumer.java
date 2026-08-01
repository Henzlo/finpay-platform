package com.finpay.fraud.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.fraud.dto.FraudCheckRequest;
import com.finpay.fraud.dto.FraudCheckResponse;
import com.finpay.fraud.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoanFraudConsumer {

    private final ObjectMapper objectMapper;
    private final FraudDetectionService fraudDetectionService;

    @KafkaListener(
            topics = "loan-events",
            groupId = "fraud-detection-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeLoanEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            // LOAN_APPLIED events have no monthlyEmi (approved) and no reason (rejected)
            if (root.hasNonNull("monthlyEmi") || root.hasNonNull("reason") || root.hasNonNull("rejectionReason")) {
                log.debug("Ignoring non-application loan-events message={}", message);
                return;
            }

            String loanId = textOrNull(root, "loanId");
            String userId = textOrNull(root, "userId");
            if (loanId == null || userId == null) {
                log.warn("Ignoring loan-events message without loanId/userId: {}", message);
                return;
            }

            FraudCheckRequest request = new FraudCheckRequest();
            request.setReferenceId(loanId);
            request.setReferenceType("LOAN");
            request.setUserId(UUID.fromString(userId));
            request.setUserEmail(textOrNull(root, "userEmail"));
            request.setAmount(decimalOrNull(root, "amount"));

            FraudCheckResponse result = fraudDetectionService.checkFraud(request);
            log.info("Fraud check from loan-events loanId={} status={} riskLevel={} score={}",
                    loanId, result.getStatus(), result.getRiskLevel(), result.getRiskScore());
        } catch (Exception ex) {
            log.error("Failed to process loan-events message={}", message, ex);
        }
    }

    private String textOrNull(JsonNode root, String field) {
        return root.hasNonNull(field) ? root.get(field).asText() : null;
    }

    private BigDecimal decimalOrNull(JsonNode root, String field) {
        return root.hasNonNull(field) ? new BigDecimal(root.get(field).asText()) : null;
    }
}
