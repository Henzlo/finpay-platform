package com.finpay.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.payment.dto.PayEmiRequest;
import com.finpay.payment.dto.PaymentHistoryResponse;
import com.finpay.payment.dto.PaymentResponse;
import com.finpay.payment.entity.OutboxEvent;
import com.finpay.payment.entity.Payment;
import com.finpay.payment.entity.PaymentReadModel;
import com.finpay.payment.entity.PaymentStatus;
import com.finpay.payment.exception.PaymentNotFoundException;
import com.finpay.payment.kafka.event.PaymentFailedEvent;
import com.finpay.payment.kafka.event.PaymentSuccessEvent;
import com.finpay.payment.kafka.producer.PaymentEventProducer;
import com.finpay.payment.repository.OutboxEventRepository;
import com.finpay.payment.repository.PaymentReadModelRepository;
import com.finpay.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentReadModelRepository paymentReadModelRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentResponse payEmi(PayEmiRequest request, UUID userId, String userEmail) {
        String idempotencyKey = buildIdempotencyKey(request.getLoanId(), request.getEmiId(), userId);
        String redisKey = "idempotency:" + idempotencyKey;

        String existingPaymentId = redisTemplate.opsForValue().get(redisKey);
        if (existingPaymentId != null) {
            return paymentRepository.findByIdempotencyKey(idempotencyKey)
                    .or(() -> paymentRepository.findById(UUID.fromString(existingPaymentId)))
                    .map(payment -> toResponse(payment, "Payment already processed"))
                    .orElseGet(() -> PaymentResponse.builder()
                            .idempotencyKey(idempotencyKey)
                            .status(PaymentStatus.SUCCESS)
                            .message("Payment already processed")
                            .build());
        }

        Payment payment = paymentRepository.save(Payment.builder()
                .loanId(request.getLoanId())
                .userId(userId)
                .emiId(request.getEmiId())
                .amount(request.getAmount())
                .paymentMode(request.getPaymentMode())
                .status(PaymentStatus.INITIATED)
                .idempotencyKey(idempotencyKey)
                .build());

        try {
            LocalDateTime processedAt = LocalDateTime.now();
            String transactionRef = UUID.randomUUID().toString();

            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionRef(transactionRef);
            payment.setProcessedAt(processedAt);
            payment = paymentRepository.save(payment);

            PaymentSuccessEvent successEvent = PaymentSuccessEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .paymentId(payment.getId().toString())
                    .loanId(payment.getLoanId().toString())
                    .userId(userId.toString())
                    .userEmail(userEmail)
                    .amount(payment.getAmount())
                    .transactionRef(transactionRef)
                    .paymentMode(payment.getPaymentMode().name())
                    .paidAt(processedAt)
                    .timestamp(LocalDateTime.now())
                    .build();

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventType("PAYMENT_SUCCESS")
                    .aggregateId(payment.getLoanId().toString())
                    .payload(objectMapper.writeValueAsString(successEvent))
                    .published(false)
                    .retryCount(0)
                    .build();
            outboxEventRepository.save(outboxEvent);

            paymentReadModelRepository.save(PaymentReadModel.builder()
                    .id(payment.getId())
                    .loanId(payment.getLoanId())
                    .userId(payment.getUserId())
                    .amount(payment.getAmount())
                    .paymentMode(payment.getPaymentMode())
                    .status(PaymentStatus.SUCCESS)
                    .transactionRef(transactionRef)
                    .paidAt(processedAt)
                    .build());

            redisTemplate.opsForValue().set(redisKey, payment.getId().toString(), IDEMPOTENCY_TTL);

            log.info("Payment SUCCESS paymentId={} loanId={} userId={}",
                    payment.getId(), payment.getLoanId(), userId);

            return toResponse(payment, "EMI payment successful");
        } catch (Exception ex) {
            log.error("Payment processing failed for paymentId={}", payment.getId(), ex);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(ex.getMessage());
            payment.setProcessedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);

            paymentEventProducer.publishPaymentFailed(PaymentFailedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .paymentId(payment.getId().toString())
                    .loanId(payment.getLoanId().toString())
                    .userId(userId.toString())
                    .failureReason(ex.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());

            return toResponse(payment, "EMI payment failed: " + ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentHistoryResponse> getPaymentHistory(UUID userId) {
        return paymentReadModelRepository.findByUserIdOrderByPaidAtDesc(userId).stream()
                .map(this::toHistory)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentHistoryResponse> getLoanPayments(UUID loanId) {
        return paymentReadModelRepository.findByLoanIdOrderByPaidAtDesc(loanId).stream()
                .map(this::toHistory)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
        return toResponse(payment, null);
    }

    private String buildIdempotencyKey(UUID loanId, UUID emiId, UUID userId) {
        return "PAY-" + loanId + "-" + emiId + "-" + userId + "-" + LocalDate.now().format(DAY);
    }

    private PaymentResponse toResponse(Payment payment, String message) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .loanId(payment.getLoanId())
                .userId(payment.getUserId())
                .emiId(payment.getEmiId())
                .amount(payment.getAmount())
                .paymentMode(payment.getPaymentMode())
                .status(payment.getStatus())
                .transactionRef(payment.getTransactionRef())
                .idempotencyKey(payment.getIdempotencyKey())
                .message(message)
                .processedAt(payment.getProcessedAt())
                .build();
    }

    private PaymentHistoryResponse toHistory(PaymentReadModel model) {
        return PaymentHistoryResponse.builder()
                .id(model.getId())
                .loanId(model.getLoanId())
                .amount(model.getAmount())
                .paymentMode(model.getPaymentMode())
                .status(model.getStatus())
                .transactionRef(model.getTransactionRef())
                .paidAt(model.getPaidAt())
                .emiNumber(model.getEmiNumber())
                .emiDueDate(model.getEmiDueDate())
                .build();
    }
}
