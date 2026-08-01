package com.finpay.payment.dto;

import com.finpay.payment.entity.PaymentMode;
import com.finpay.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryResponse {

    private UUID id;
    private UUID loanId;
    private BigDecimal amount;
    private PaymentMode paymentMode;
    private PaymentStatus status;
    private String transactionRef;
    private LocalDateTime paidAt;
    private Integer emiNumber;
    private LocalDate emiDueDate;
}
