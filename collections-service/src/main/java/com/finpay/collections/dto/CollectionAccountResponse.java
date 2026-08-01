package com.finpay.collections.dto;

import com.finpay.collections.entity.CollectionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CollectionAccountResponse {

    private UUID id;
    private UUID loanId;
    private UUID userId;
    private String userName;
    private String userEmail;
    private String agentName;
    private BigDecimal overdueAmount;
    private Integer overdueDays;
    private Integer missedEmis;
    private CollectionStatus status;
    private LocalDate promisedPayDate;
    private BigDecimal promisedAmount;
    private LocalDateTime assignedAt;
    private LocalDateTime lastContactedAt;
}
