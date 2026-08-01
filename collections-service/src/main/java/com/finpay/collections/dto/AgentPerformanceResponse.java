package com.finpay.collections.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class AgentPerformanceResponse {

    private UUID agentId;
    private String agentName;
    private long totalAssigned;
    private long resolvedToday;
    private long callsToday;
    private long promisesToday;
    private BigDecimal collectedToday;
    private int performanceScore;
}
