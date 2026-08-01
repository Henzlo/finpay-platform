package com.finpay.credit.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ScoreHistoryResponse {

    private Integer score;
    private Integer change;
    private String reason;
    private String changeType;
    private LocalDateTime date;
}
