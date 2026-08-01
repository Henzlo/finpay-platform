package com.finpay.payment.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.payment.entity.OutboxEvent;
import com.finpay.payment.kafka.event.PaymentSuccessEvent;
import com.finpay.payment.kafka.producer.PaymentEventProducer;
import com.finpay.payment.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxScheduler {

    private static final int MAX_RETRY = 3;

    private final OutboxEventRepository outboxEventRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> events =
                outboxEventRepository.findByPublishedFalseAndRetryCountLessThan(MAX_RETRY);

        if (events.isEmpty()) {
            return;
        }

        int processed = 0;
        for (OutboxEvent event : events) {
            try {
                PaymentSuccessEvent payload =
                        objectMapper.readValue(event.getPayload(), PaymentSuccessEvent.class);
                paymentEventProducer.publishPaymentSuccess(payload);
                event.setPublished(true);
                event.setPublishedAt(LocalDateTime.now());
                outboxEventRepository.save(event);
                processed++;
            } catch (Exception ex) {
                int retries = event.getRetryCount() == null ? 0 : event.getRetryCount();
                event.setRetryCount(retries + 1);
                outboxEventRepository.save(event);
                log.error("Outbox publish failed for eventId={} retryCount={}",
                        event.getId(), event.getRetryCount(), ex);
            }
        }

        log.info("Processed {} outbox events", processed);
    }
}
