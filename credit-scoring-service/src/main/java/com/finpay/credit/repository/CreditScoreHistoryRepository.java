package com.finpay.credit.repository;

import com.finpay.credit.entity.CreditScoreHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CreditScoreHistoryRepository extends JpaRepository<CreditScoreHistory, UUID> {

    List<CreditScoreHistory> findByUserIdOrderByChangedAtDesc(UUID userId);
}
