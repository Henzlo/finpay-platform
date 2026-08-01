package com.finpay.collections.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "call_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID accountId;

    @Column(nullable = false)
    private UUID agentId;

    private String agentName;

    private LocalDateTime calledAt;

    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Disposition disposition;

    private LocalDate promiseDate;

    private BigDecimal promiseAmount;

    private String notes;

    private String aiSummary;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
