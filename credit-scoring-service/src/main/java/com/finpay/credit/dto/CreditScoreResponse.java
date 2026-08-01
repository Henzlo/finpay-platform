package com.finpay.credit.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CreditScoreResponse {

    private UUID userId;
    private Integer score;
    private String category;
    private String categoryColor;
    private Integer maxLoanEligible;
    private String approvalChance;
    private List<String> improvementTips;
    private List<String> positiveFactors;
    private LocalDateTime calculatedAt;
}
