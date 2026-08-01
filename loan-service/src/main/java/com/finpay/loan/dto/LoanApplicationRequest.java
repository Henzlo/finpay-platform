package com.finpay.loan.dto;

import com.finpay.loan.entity.LoanPurpose;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationRequest {

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private LoanPurpose purpose;

    @NotNull
    @Min(6)
    @Max(60)
    private Integer tenureMonths;

    private String notes;
}
