package com.finpay.credit.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.credit.kafka.event.LoanEvent;
import com.finpay.credit.service.CreditScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoanCreditConsumer {

    private final ObjectMapper objectMapper;
    private final CreditScoringService creditScoringService;

    @KafkaListener(
            topics = "loan-events",
            groupId = "credit-scoring-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeLoanEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            if (!root.hasNonNull("monthlyEmi")) {
                log.debug("Ignoring non-approval loan-events message={}", message);
                return;
            }

            LoanEvent event = objectMapper.treeToValue(root, LoanEvent.class);
            if (event.getUserId() == null || event.getUserId().isBlank()) {
                log.warn("Ignoring loan-events approval without userId: {}", message);
                return;
            }

            creditScoringService.handleLoanApproved(event.getUserId(), event.getMonthlyEmi());
            log.info("Loan approved - credit updated userId={} loanId={}",
                    event.getUserId(), event.getLoanId());
        } catch (Exception ex) {
            log.error("Failed to process loan-events message={}", message, ex);
        }
    }
}
