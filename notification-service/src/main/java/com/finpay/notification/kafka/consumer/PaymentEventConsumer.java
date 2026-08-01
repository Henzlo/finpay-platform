package com.finpay.notification.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.notification.entity.NotificationType;
import com.finpay.notification.kafka.event.PaymentFailedEvent;
import com.finpay.notification.kafka.event.PaymentSuccessEvent;
import com.finpay.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @KafkaListener(
            topics = "payment-events",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumePaymentEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            log.info("Consumed payment-events message={}", message);

            if (root.hasNonNull("transactionRef")) {
                PaymentSuccessEvent event = objectMapper.treeToValue(root, PaymentSuccessEvent.class);
                notificationService.createAndSendNotification(
                        event.getUserId(),
                        event.getUserEmail(),
                        "Payment Successful ✅",
                        "EMI payment of ₹" + formatAmount(event.getAmount())
                                + " received. Transaction ID: " + event.getTransactionRef(),
                        NotificationType.PAYMENT_SUCCESS,
                        event.getPaymentId(),
                        "/payments/" + event.getPaymentId());
                log.info("Handled PAYMENT_SUCCESS for paymentId={}", event.getPaymentId());
                return;
            }

            PaymentFailedEvent event = objectMapper.treeToValue(root, PaymentFailedEvent.class);
            String reason = event.getFailureReason() != null ? event.getFailureReason() : "Unknown";
            notificationService.createAndSendNotification(
                    event.getUserId(),
                    event.getUserEmail(),
                    "Payment Failed ❌",
                    "Your payment of ₹" + formatAmount(event.getAmount())
                            + " failed. Reason: " + reason,
                    NotificationType.PAYMENT_FAILED,
                    event.getPaymentId(),
                    "/payments/" + event.getPaymentId());
            log.info("Handled PAYMENT_FAILED for paymentId={}", event.getPaymentId());
        } catch (Exception ex) {
            log.error("Failed to process payment-events message={}", message, ex);
        }
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? "0" : amount.toPlainString();
    }
}
