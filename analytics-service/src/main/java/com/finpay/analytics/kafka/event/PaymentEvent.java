package com.finpay.analytics.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    private String paymentId;
    private String loanId;
    private String userId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime timestamp;
}
