package com.finpay.reporting.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class DailySummaryData {

    private LocalDate date;
    private long totalLoans;
    private long approvedLoans;
    private long rejectedLoans;
    private BigDecimal totalDisbursed;
    private long totalPayments;
    private BigDecimal totalCollected;
    private long newUsers;
    private String generatedAt;
}
