package com.finpay.loan.kafka.producer;

import com.finpay.loan.kafka.event.LoanAppliedEvent;
import com.finpay.loan.kafka.event.LoanApprovedEvent;
import com.finpay.loan.kafka.event.LoanRejectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanEventProducer {

    public static final String LOAN_EVENTS_TOPIC = "loan-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishLoanApplied(LoanAppliedEvent event) {
        send(event.getLoanId(), event, "LOAN_APPLIED");
    }

    public void publishLoanApproved(LoanApprovedEvent event) {
        send(event.getLoanId(), event, "LOAN_APPROVED");
    }

    public void publishLoanRejected(LoanRejectedEvent event) {
        send(event.getLoanId(), event, "LOAN_REJECTED");
    }

    private void send(String key, Object payload, String eventType) {
        try {
            kafkaTemplate.send(LOAN_EVENTS_TOPIC, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} for loanId: {}", eventType, key, ex);
                        } else {
                            log.info("Published {} for loanId: {}", eventType, key);
                        }
                    });
        } catch (Exception ex) {
            log.error("Failed to publish {} for loanId: {}", eventType, key, ex);
        }
    }
}
