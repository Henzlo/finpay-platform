package com.finpay.payment.dto;

import com.finpay.payment.entity.PaymentMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PayEmiRequest {

    @NotNull
    private UUID loanId;

    @NotNull
    private UUID emiId;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private PaymentMode paymentMode;
}
