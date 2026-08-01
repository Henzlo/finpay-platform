package com.finpay.notification.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.notification.entity.NotificationType;
import com.finpay.notification.kafka.event.LoanAppliedEvent;
import com.finpay.notification.kafka.event.LoanApprovedEvent;
import com.finpay.notification.kafka.event.LoanRejectedEvent;
import com.finpay.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoanEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @KafkaListener(
            topics = "loan-events",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeLoanEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            log.info("Consumed loan-events message={}", message);

            if (root.hasNonNull("monthlyEmi")) {
                LoanApprovedEvent event = objectMapper.treeToValue(root, LoanApprovedEvent.class);
                notificationService.createAndSendNotification(
                        event.getUserId(),
                        event.getUserEmail(),
                        "Loan Approved! 🎉",
                        "Your loan of ₹" + formatAmount(event.getAmount())
                                + " has been approved. Monthly EMI: ₹"
                                + formatAmount(event.getMonthlyEmi()),
                        NotificationType.LOAN_APPROVED,
                        event.getLoanId(),
                        "/loans/" + event.getLoanId());
                log.info("Handled LOAN_APPROVED for loanId={}", event.getLoanId());
                return;
            }

            if (root.hasNonNull("reason") || root.hasNonNull("rejectionReason")) {
                LoanRejectedEvent event = objectMapper.treeToValue(root, LoanRejectedEvent.class);
                String reason = event.getReason() != null
                        ? event.getReason()
                        : root.path("rejectionReason").asText("Not specified");
                notificationService.createAndSendNotification(
                        event.getUserId(),
                        event.getUserEmail(),
                        "Loan Application Update",
                        "Your loan application was not approved. Reason: " + reason,
                        NotificationType.LOAN_REJECTED,
                        event.getLoanId(),
                        "/loans/" + event.getLoanId());
                log.info("Handled LOAN_REJECTED for loanId={}", event.getLoanId());
                return;
            }

            LoanAppliedEvent event = objectMapper.treeToValue(root, LoanAppliedEvent.class);
            notificationService.createAndSendNotification(
                    event.getUserId(),
                    event.getUserEmail(),
                    "Loan Application Received",
                    "Your loan application for ₹" + formatAmount(event.getAmount())
                            + " has been received and is under review.",
                    NotificationType.LOAN_APPLIED,
                    event.getLoanId(),
                    "/loans/" + event.getLoanId());
            log.info("Handled LOAN_APPLIED for loanId={}", event.getLoanId());
        } catch (Exception ex) {
            log.error("Failed to process loan-events message={}", message, ex);
        }
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? "0" : amount.toPlainString();
    }
}
