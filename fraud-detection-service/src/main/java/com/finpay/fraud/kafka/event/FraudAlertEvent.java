package com.finpay.fraud.kafka.event;

import com.finpay.fraud.entity.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlertEvent {

    private String alertId;
    private String referenceId;
    private String referenceType;
    private String userId;
    private String userEmail;
    private RiskLevel riskLevel;
    private Integer riskScore;
    private List<String> triggeredRules;
    private String recommendation;
    private LocalDateTime timestamp;
}
