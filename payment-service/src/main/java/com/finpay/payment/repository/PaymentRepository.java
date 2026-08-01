package com.finpay.payment.repository;

import com.finpay.payment.entity.Payment;
import com.finpay.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String key);

    List<Payment> findByLoanId(UUID loanId);

    List<Payment> findByUserId(UUID userId);

    List<Payment> findByLoanIdAndStatus(UUID loanId, PaymentStatus status);
}
