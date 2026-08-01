package com.finpay.loan.kafka.event;

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
public class LoanAppliedEvent {

    private String eventId;
    private String loanId;
    private String userId;
    private String userEmail;
    private String userName;
    private BigDecimal amount;
    private String purpose;
    private Integer tenureMonths;
    private LocalDateTime timestamp;
}
