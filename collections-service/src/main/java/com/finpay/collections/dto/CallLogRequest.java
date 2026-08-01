package com.finpay.collections.dto;

import com.finpay.collections.entity.Disposition;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CallLogRequest {

    @NotNull
    private UUID accountId;

    @NotNull
    private Disposition disposition;

    private Integer durationSeconds;

    private LocalDate promiseDate;

    private BigDecimal promiseAmount;

    private String notes;
}
