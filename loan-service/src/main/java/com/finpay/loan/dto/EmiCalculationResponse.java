package com.finpay.loan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmiCalculationResponse {

    private BigDecimal monthlyEmi;
    private BigDecimal totalAmount;
    private BigDecimal totalInterest;
    private BigDecimal principalAmount;
    private Integer tenureMonths;
    private BigDecimal interestRate;
    private List<EmiBreakdown> amortizationSchedule;
}
