package com.finpay.loan.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ErrorResponse {

    private int status;
    private String message;
    private List<String> errors;
    private LocalDateTime timestamp;
}
