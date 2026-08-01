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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "collection_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID loanId;

    @Column(nullable = false)
    private UUID userId;

    private String userName;

    private String userEmail;

    private String userPhone;

    private UUID agentId;

    private String agentName;

    private BigDecimal overdueAmount;

    private Integer overdueDays;

    private Integer missedEmis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollectionStatus status;

    private LocalDate promisedPayDate;

    private BigDecimal promisedAmount;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime assignedAt;

    private LocalDateTime lastContactedAt;

    private LocalDateTime resolvedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
