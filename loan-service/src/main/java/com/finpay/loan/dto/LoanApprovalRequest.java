package com.finpay.loan.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanApprovalRequest {

    private BigDecimal interestRate;
    private String notes;
}
