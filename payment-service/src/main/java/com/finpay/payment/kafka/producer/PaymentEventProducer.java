package com.finpay.payment.kafka.producer;

import com.finpay.payment.kafka.event.PaymentFailedEvent;
import com.finpay.payment.kafka.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentEventProducer {

    public static final String PAYMENT_EVENTS_TOPIC = "payment-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentSuccess(PaymentSuccessEvent event) {
        send(event.getPaymentId(), event, "PAYMENT_SUCCESS");
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        send(event.getPaymentId(), event, "PAYMENT_FAILED");
    }

    private void send(String key, Object payload, String eventType) {
        try {
            kafkaTemplate.send(PAYMENT_EVENTS_TOPIC, key, payload).get(5, TimeUnit.SECONDS);
            log.info("Published {} for paymentId: {}", eventType, key);
        } catch (Exception ex) {
            log.error("Failed to publish {} for paymentId: {}", eventType, key, ex);
            throw new IllegalStateException("Failed to publish " + eventType, ex);
        }
    }
}
