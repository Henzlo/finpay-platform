package com.finpay.payment.repository;

import com.finpay.payment.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc();

    List<OutboxEvent> findByPublishedFalseAndRetryCountLessThan(int maxRetry);
}
