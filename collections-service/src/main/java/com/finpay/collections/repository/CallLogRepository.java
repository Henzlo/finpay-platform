package com.finpay.collections.repository;

import com.finpay.collections.entity.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CallLogRepository extends JpaRepository<CallLog, UUID> {

    List<CallLog> findByAccountIdOrderByCalledAtDesc(UUID accountId);

    List<CallLog> findByAgentIdOrderByCalledAtDesc(UUID agentId);

    long countByAgentIdAndCalledAtAfter(UUID agentId, LocalDateTime after);
}
