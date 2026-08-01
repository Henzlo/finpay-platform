package com.finpay.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {

    private long totalLoans;
    private long pendingLoans;
    private long activeLoans;
    private long npaLoans;
    private BigDecimal totalDisbursed;
    private BigDecimal totalCollected;
    private long totalUsers;
    private LocalDateTime generatedAt;
}
