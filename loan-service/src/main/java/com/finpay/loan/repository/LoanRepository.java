package com.finpay.loan.repository;

import com.finpay.loan.entity.Loan;
import com.finpay.loan.entity.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    List<Loan> findByUserId(UUID userId);

    List<Loan> findByStatus(LoanStatus status);

    List<Loan> findByUserIdAndStatus(UUID userId, LoanStatus status);

    long countByStatus(LoanStatus status);
}
