package com.finpay.loan.entity;

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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    private String userName;

    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanPurpose purpose;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Integer tenureMonths;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal interestRate = new BigDecimal("12.0");

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    private String rejectionReason;

    private Integer creditScoreAtApplication;

    @Column(precision = 19, scale = 2)
    private BigDecimal monthlyEmi;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime appliedAt;

    private LocalDateTime approvedAt;

    private LocalDateTime disbursedAt;

    private UUID approvedBy;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
