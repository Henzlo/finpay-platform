package com.finpay.fraud.repository;

import com.finpay.fraud.entity.FraudCheck;
import com.finpay.fraud.entity.FraudCheckStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface FraudCheckRepository extends JpaRepository<FraudCheck, UUID> {

    List<FraudCheck> findByUserId(UUID userId);

    List<FraudCheck> findByStatus(FraudCheckStatus status);

    long countByUserIdAndCheckedAtAfter(UUID userId, LocalDateTime after);

    List<FraudCheck> findByReferenceId(String referenceId);

    long countByUserIdAndReferenceTypeAndCheckedAtAfter(UUID userId, String type, LocalDateTime after);
}
