package com.finpay.payment.repository;

import com.finpay.payment.entity.PaymentReadModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentReadModelRepository extends JpaRepository<PaymentReadModel, UUID> {

    List<PaymentReadModel> findByUserIdOrderByPaidAtDesc(UUID userId);

    List<PaymentReadModel> findByLoanIdOrderByPaidAtDesc(UUID loanId);
}
