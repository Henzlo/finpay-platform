package com.finpay.loan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoanRejectionRequest {

    @NotBlank
    private String reason;
}
