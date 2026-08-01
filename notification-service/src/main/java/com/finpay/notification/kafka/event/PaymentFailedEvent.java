package com.finpay.notification.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {

    private String paymentId;
    private String loanId;
    private String userId;
    private String userEmail;
    private BigDecimal amount;
    private String failureReason;
    private LocalDateTime timestamp;
}
