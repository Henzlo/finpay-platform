package com.finpay.fraud.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationEvent {

    private String loanId;
    private String userId;
    private String userEmail;
    private BigDecimal amount;
    private String purpose;
    private Integer tenureMonths;
    private LocalDateTime timestamp;
}
