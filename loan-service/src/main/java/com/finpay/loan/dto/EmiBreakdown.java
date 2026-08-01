package com.finpay.loan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmiBreakdown {

    private Integer month;
    private BigDecimal emi;
    private BigDecimal principal;
    private BigDecimal interest;
    private BigDecimal balance;
}
