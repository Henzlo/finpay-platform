package com.finpay.credit.repository;

import com.finpay.credit.entity.CreditScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditScoreRepository extends JpaRepository<CreditScore, UUID> {

    Optional<CreditScore> findTopByUserIdOrderByCalculatedAtDesc(UUID userId);

    List<CreditScore> findByUserIdOrderByCalculatedAtDesc(UUID userId);
}
