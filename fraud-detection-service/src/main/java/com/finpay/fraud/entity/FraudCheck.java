package com.finpay.fraud.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "fraud_checks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String referenceId;

    private String referenceType;

    private UUID userId;

    private String userEmail;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    private FraudCheckStatus status;

    private Integer riskScore;

    @ElementCollection
    @CollectionTable(name = "fraud_check_triggered_rules", joinColumns = @JoinColumn(name = "fraud_check_id"))
    @Column(name = "triggered_rule")
    @Builder.Default
    private List<String> triggeredRules = new ArrayList<>();

    private String recommendation;

    private String ipAddress;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime checkedAt;
}
