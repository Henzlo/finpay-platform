package com.finpay.collections.kafka.event;

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
public class PaymentMissedEvent {

    private String loanId;
    private String userId;
    private String userName;
    private String userEmail;
    private BigDecimal overdueAmount;
    private Integer overdueDays;
    private Integer missedEmis;
    private LocalDateTime timestamp;
}
