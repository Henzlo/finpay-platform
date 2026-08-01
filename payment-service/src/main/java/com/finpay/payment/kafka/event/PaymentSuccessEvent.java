package com.finpay.payment.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEvent {

    private String eventId;
    private String paymentId;
    private String loanId;
    private String userId;
    private String userEmail;
    private BigDecimal amount;
    private String transactionRef;
    private String paymentMode;
    private LocalDateTime paidAt;
    private LocalDateTime timestamp;
}
