package com.finpay.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_read_model")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReadModel {

    @Id
    private UUID id;

    private UUID loanId;

    private UUID userId;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String transactionRef;

    private LocalDateTime paidAt;

    private Integer emiNumber;

    private LocalDate emiDueDate;
}
