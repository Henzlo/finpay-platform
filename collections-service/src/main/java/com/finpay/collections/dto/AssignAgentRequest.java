package com.finpay.collections.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignAgentRequest {

    @NotNull
    private UUID accountId;

    @NotNull
    private UUID agentId;

    private String agentName;
}
