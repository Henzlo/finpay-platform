package com.finpay.credit.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class SimulationRequest {

    private UUID userId;

    /** CLOSE_LOAN / ON_TIME_PAYMENT / MISSED_PAYMENT */
    private String action;

    private Integer months;
}
