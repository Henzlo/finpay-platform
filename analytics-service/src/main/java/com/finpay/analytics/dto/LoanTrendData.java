package com.finpay.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanTrendData {

    private String month;
    private long loansApplied;
    private long loansApproved;
    private long loansRejected;
    private BigDecimal totalAmount;
}
