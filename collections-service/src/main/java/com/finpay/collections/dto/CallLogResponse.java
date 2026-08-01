package com.finpay.collections.dto;

import com.finpay.collections.entity.Disposition;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CallLogResponse {

    private UUID id;
    private UUID accountId;
    private UUID agentId;
    private String agentName;
    private LocalDateTime calledAt;
    private Integer durationSeconds;
    private Disposition disposition;
    private LocalDate promiseDate;
    private BigDecimal promiseAmount;
    private String notes;
    private String aiSummary;
}
