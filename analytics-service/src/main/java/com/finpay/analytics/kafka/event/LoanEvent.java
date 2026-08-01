package com.finpay.analytics.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanEvent {

    private String loanId;
    private String userId;
    private String status;
    private String purpose;
    private BigDecimal amount;
    private LocalDateTime timestamp;
}
