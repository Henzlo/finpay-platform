package com.finpay.credit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_scores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    /** Score in the 300–900 range. */
    @Column(nullable = false)
    private Integer score;

    /** EXCELLENT / GOOD / FAIR / POOR */
    @Column(nullable = false)
    private String category;

    private Integer paymentFactor;

    private Integer burdenFactor;

    private Integer loansFactor;

    private Integer ageFactor;

    private String changeReason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime calculatedAt;
}
