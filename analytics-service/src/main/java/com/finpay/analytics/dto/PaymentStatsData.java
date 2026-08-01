package com.finpay.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatsData {

    private LocalDate date;
    private long totalPayments;
    private BigDecimal totalAmount;
    private long successfulPayments;
    private long failedPayments;
}
