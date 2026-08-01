package com.finpay.notification.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanApprovedEvent {

    private String loanId;
    private String userId;
    private String userEmail;
    private BigDecimal amount;
    private BigDecimal monthlyEmi;
    private Integer tenureMonths;
    private LocalDateTime timestamp;
}
