package com.finpay.notification.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanRejectedEvent {

    private String loanId;
    private String userId;
    private String userEmail;
    private String reason;
    private LocalDateTime timestamp;
}
