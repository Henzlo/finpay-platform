package com.finpay.loan.dto;

import com.finpay.loan.entity.LoanPurpose;
import com.finpay.loan.entity.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponse {

    private UUID id;
    private UUID userId;
    private String userName;
    private String userEmail;
    private LoanPurpose purpose;
    private BigDecimal amount;
    private BigDecimal interestRate;
    private BigDecimal monthlyEmi;
    private Integer tenureMonths;
    private LoanStatus status;
    private String rejectionReason;
    private Integer creditScoreAtApplication;
    private LocalDateTime appliedAt;
    private LocalDateTime approvedAt;
    private List<EmiScheduleResponse> emiSchedule;
}
