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
@Table(name = "credit_score_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditScoreHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    private Integer oldScore;

    private Integer newScore;

    /** newScore - oldScore */
    private Integer change;

    private String reason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime changedAt;
}
