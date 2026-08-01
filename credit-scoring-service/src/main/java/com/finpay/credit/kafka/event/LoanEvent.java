package com.finpay.credit.kafka.event;

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
    private BigDecimal amount;
    private String status;
    private Integer tenureMonths;
    private BigDecimal monthlyEmi;
    private LocalDateTime timestamp;
}
