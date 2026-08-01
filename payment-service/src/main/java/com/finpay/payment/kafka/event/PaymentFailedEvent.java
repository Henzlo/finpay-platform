package com.finpay.payment.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {

    private String eventId;
    private String paymentId;
    private String loanId;
    private String userId;
    private String failureReason;
    private LocalDateTime timestamp;
}
