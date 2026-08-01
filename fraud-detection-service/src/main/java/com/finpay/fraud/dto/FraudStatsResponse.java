package com.finpay.fraud.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class FraudStatsResponse {

    private long totalChecks;
    private long flaggedToday;
    private long blockedToday;
    private long passedToday;
    private Map<String, Long> riskLevelDistribution;
}
