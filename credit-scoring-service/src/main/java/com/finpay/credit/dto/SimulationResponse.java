package com.finpay.credit.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SimulationResponse {

    private Integer currentScore;
    private Integer projectedScore;
    private Integer change;
    private String message;
    private String advice;
}
