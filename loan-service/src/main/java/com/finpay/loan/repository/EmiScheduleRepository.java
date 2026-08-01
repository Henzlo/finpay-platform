package com.finpay.loan.repository;

import com.finpay.loan.entity.EmiSchedule;
import com.finpay.loan.entity.EmiStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmiScheduleRepository extends JpaRepository<EmiSchedule, UUID> {

    List<EmiSchedule> findByLoanId(UUID loanId);

    List<EmiSchedule> findByLoanIdAndStatus(UUID loanId, EmiStatus status);

    Optional<EmiSchedule> findFirstByLoanIdAndStatusOrderByDueDateAsc(UUID loanId, EmiStatus status);

    List<EmiSchedule> findByDueDateBeforeAndStatus(LocalDate date, EmiStatus status);
}
