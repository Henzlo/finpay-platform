package com.finpay.loan.dto;

import com.finpay.loan.entity.EmiStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmiScheduleResponse {

    private UUID id;
    private UUID loanId;
    private Integer emiNumber;
    private LocalDate dueDate;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal totalAmount;
    private BigDecimal outstandingBalance;
    private EmiStatus status;
    private LocalDateTime paidAt;
}
