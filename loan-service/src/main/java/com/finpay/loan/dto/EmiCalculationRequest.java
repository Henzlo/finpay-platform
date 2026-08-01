package com.finpay.loan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmiCalculationRequest {

    @NotNull
    private BigDecimal amount;

    @NotNull
    private Integer tenureMonths;

    @Builder.Default
    private BigDecimal interestRate = new BigDecimal("12.0");
}
