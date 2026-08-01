package com.finpay.collections.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateAccountRequest {

    @NotNull
    private UUID loanId;

    @NotNull
    private UUID userId;

    private String userName;

    private String userEmail;

    private String userPhone;

    @NotNull
    private BigDecimal overdueAmount;

    @NotNull
    private Integer overdueDays;

    private Integer missedEmis;
}
