package com.finpay.fraud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class FraudCheckRequest {

    @NotBlank
    private String referenceId;

    @NotBlank
    private String referenceType;

    @NotNull
    private UUID userId;

    private String userEmail;

    private BigDecimal amount;

    private String ipAddress;

    private String deviceId;
}
