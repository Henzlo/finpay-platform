package com.finpay.fraud.dto;

import com.finpay.fraud.entity.FraudCheckStatus;
import com.finpay.fraud.entity.RiskLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class FraudCheckResponse {

    private UUID id;
    private String referenceId;
    private String referenceType;
    private RiskLevel riskLevel;
    private FraudCheckStatus status;
    private Integer riskScore;
    private List<String> triggeredRules;
    private String recommendation;
    private boolean blocked;
    private LocalDateTime checkedAt;
}
