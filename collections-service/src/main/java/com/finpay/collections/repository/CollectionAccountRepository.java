package com.finpay.collections.repository;

import com.finpay.collections.entity.CollectionAccount;
import com.finpay.collections.entity.CollectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CollectionAccountRepository extends JpaRepository<CollectionAccount, UUID> {

    List<CollectionAccount> findByAgentId(UUID agentId);

    List<CollectionAccount> findByStatus(CollectionStatus status);

    Optional<CollectionAccount> findByLoanId(UUID loanId);

    List<CollectionAccount> findByAgentIdAndStatus(UUID agentId, CollectionStatus status);

    long countByAgentId(UUID agentId);

    long countByAgentIdAndStatus(UUID agentId, CollectionStatus status);
}
