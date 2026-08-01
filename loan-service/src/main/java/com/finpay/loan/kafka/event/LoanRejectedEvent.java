package com.finpay.loan.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRejectedEvent {

    private String eventId;
    private String loanId;
    private String userId;
    private String userEmail;
    private String reason;
    private LocalDateTime timestamp;
}
