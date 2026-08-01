package com.finpay.notification.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanAppliedEvent {

    private String loanId;
    private String userId;
    private String userEmail;
    private String userName;
    private String purpose;
    private BigDecimal amount;
    private LocalDateTime timestamp;
}
