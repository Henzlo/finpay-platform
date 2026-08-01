package com.finpay.collections.service;

import com.finpay.collections.dto.AgentPerformanceResponse;
import com.finpay.collections.dto.AssignAgentRequest;
import com.finpay.collections.dto.BestTimeResponse;
import com.finpay.collections.dto.CallLogRequest;
import com.finpay.collections.dto.CallLogResponse;
import com.finpay.collections.dto.CollectionAccountResponse;
import com.finpay.collections.dto.CreateAccountRequest;
import com.finpay.collections.entity.CallLog;
import com.finpay.collections.entity.CollectionAccount;
import com.finpay.collections.entity.CollectionStatus;
import com.finpay.collections.entity.Disposition;
import com.finpay.collections.kafka.event.PaymentMissedEvent;
import com.finpay.collections.repository.CallLogRepository;
import com.finpay.collections.repository.CollectionAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CollectionsService {

    private final CollectionAccountRepository collectionAccountRepository;
    private final CallLogRepository callLogRepository;

    @Transactional
    public CollectionAccount createAccount(PaymentMissedEvent event) {
        UUID loanId = UUID.fromString(event.getLoanId());
        UUID userId = UUID.fromString(event.getUserId());

        CollectionAccount account = collectionAccountRepository.findByLoanId(loanId)
                .orElse(null);

        if (account != null) {
            account.setOverdueAmount(event.getOverdueAmount());
            account.setOverdueDays(event.getOverdueDays());
            if (event.getMissedEmis() != null) {
                account.setMissedEmis(event.getMissedEmis());
            }
            account.setStatus(resolveStatusByOverdueDays(event.getOverdueDays()));
            return collectionAccountRepository.save(account);
        }

        CollectionAccount created = CollectionAccount.builder()
                .loanId(loanId)
                .userId(userId)
                .userName(event.getUserName())
                .userEmail(event.getUserEmail())
                .overdueAmount(event.getOverdueAmount())
                .overdueDays(event.getOverdueDays())
                .missedEmis(event.getMissedEmis() != null ? event.getMissedEmis() : 1)
                .status(resolveStatusByOverdueDays(event.getOverdueDays()))
                .build();

        CollectionAccount saved = collectionAccountRepository.save(created);
        log.info("Created collection account id={} loanId={} status={}",
                saved.getId(), saved.getLoanId(), saved.getStatus());
        return saved;
    }

    @Transactional
    public CollectionAccountResponse createAccountManually(CreateAccountRequest request) {
        PaymentMissedEvent event = PaymentMissedEvent.builder()
                .loanId(request.getLoanId().toString())
                .userId(request.getUserId().toString())
                .userName(request.getUserName())
                .userEmail(request.getUserEmail())
                .overdueAmount(request.getOverdueAmount())
                .overdueDays(request.getOverdueDays())
                .missedEmis(request.getMissedEmis() != null ? request.getMissedEmis() : 1)
                .timestamp(LocalDateTime.now())
                .build();

        CollectionAccount account = createAccount(event);
        if (request.getUserPhone() != null) {
            account.setUserPhone(request.getUserPhone());
            account = collectionAccountRepository.save(account);
        }
        return toAccountResponse(account);
    }

    @Transactional(readOnly = true)
    public List<CollectionAccountResponse> getMyAccounts(UUID agentId) {
        return collectionAccountRepository.findByAgentId(agentId).stream()
                .map(this::toAccountResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CollectionAccountResponse> getAllAccounts() {
        return collectionAccountRepository.findAll().stream()
                .map(this::toAccountResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CollectionAccountResponse getAccountById(UUID accountId) {
        return toAccountResponse(findAccount(accountId));
    }

    @Transactional
    public CollectionAccountResponse assignAgent(AssignAgentRequest request) {
        CollectionAccount account = findAccount(request.getAccountId());
        account.setAgentId(request.getAgentId());
        account.setAgentName(request.getAgentName());
        if (account.getStatus() == CollectionStatus.ASSIGNED) {
            account.setStatus(CollectionStatus.IN_PROGRESS);
        }
        return toAccountResponse(collectionAccountRepository.save(account));
    }

    @Transactional
    public CallLogResponse logCall(CallLogRequest request, UUID agentId, String agentName) {
        CollectionAccount account = findAccount(request.getAccountId());
        LocalDateTime now = LocalDateTime.now();

        CallLog callLog = CallLog.builder()
                .accountId(account.getId())
                .agentId(agentId)
                .agentName(agentName != null ? agentName : account.getAgentName())
                .calledAt(now)
                .durationSeconds(request.getDurationSeconds())
                .disposition(request.getDisposition())
                .promiseDate(request.getPromiseDate())
                .promiseAmount(request.getPromiseAmount())
                .notes(request.getNotes())
                .aiSummary(buildAiSummary(request.getDisposition()))
                .build();

        if (request.getPromiseDate() != null) {
            account.setPromisedPayDate(request.getPromiseDate());
            account.setPromisedAmount(request.getPromiseAmount());
            account.setStatus(CollectionStatus.IN_PROGRESS);
        }

        if (request.getDisposition() == Disposition.ALREADY_PAID) {
            account.setStatus(CollectionStatus.RESOLVED);
            account.setResolvedAt(now);
        }

        account.setLastContactedAt(now);
        callLogRepository.save(callLog);
        collectionAccountRepository.save(account);

        return toCallLogResponse(callLog);
    }

    @Transactional(readOnly = true)
    public List<CallLogResponse> getCallLogs(UUID accountId) {
        findAccount(accountId);
        return callLogRepository.findByAccountIdOrderByCalledAtDesc(accountId).stream()
                .map(this::toCallLogResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BestTimeResponse getBestTimeToCall(UUID accountId) {
        findAccount(accountId);
        List<CallLog> callLogs = callLogRepository.findByAccountIdOrderByCalledAtDesc(accountId);

        if (callLogs.isEmpty()) {
            return BestTimeResponse.builder()
                    .bestDay("Tuesday-Thursday")
                    .bestTimeSlot("6:00 PM - 8:00 PM")
                    .preferredChannel("Phone Call")
                    .tip("Evening calls have higher success rate")
                    .build();
        }

        Map<Integer, Long> hourCounts = new HashMap<>();
        for (CallLog logEntry : callLogs) {
            if (logEntry.getDisposition() == Disposition.CONNECTED_WILL_PAY && logEntry.getCalledAt() != null) {
                int hour = logEntry.getCalledAt().getHour();
                hourCounts.merge(hour, 1L, Long::sum);
            }
        }

        if (hourCounts.isEmpty()) {
            return BestTimeResponse.builder()
                    .bestDay("Tuesday-Thursday")
                    .bestTimeSlot("6:00 PM - 8:00 PM")
                    .preferredChannel("Phone Call")
                    .tip("Evening calls have higher success rate")
                    .build();
        }

        int bestHour = hourCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(18);

        return BestTimeResponse.builder()
                .bestDay(resolveBestDay(callLogs))
                .bestTimeSlot(mapHourToSlot(bestHour))
                .preferredChannel("Phone Call")
                .tip("Based on previous successful CONNECTED_WILL_PAY calls for this account")
                .build();
    }

    @Transactional(readOnly = true)
    public AgentPerformanceResponse getAgentPerformance(UUID agentId) {
        long totalAssigned = collectionAccountRepository.countByAgentId(agentId);
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        List<CollectionAccount> resolved = collectionAccountRepository
                .findByAgentIdAndStatus(agentId, CollectionStatus.RESOLVED);
        long resolvedToday = resolved.stream()
                .filter(a -> a.getResolvedAt() != null && !a.getResolvedAt().isBefore(startOfDay))
                .count();

        long callsToday = callLogRepository.countByAgentIdAndCalledAtAfter(agentId, startOfDay);

        List<CallLog> todayLogs = callLogRepository.findByAgentIdOrderByCalledAtDesc(agentId).stream()
                .filter(c -> c.getCalledAt() != null && !c.getCalledAt().isBefore(startOfDay))
                .toList();

        long promisesToday = todayLogs.stream()
                .filter(c -> c.getPromiseDate() != null)
                .count();

        BigDecimal collectedToday = resolved.stream()
                .filter(a -> a.getResolvedAt() != null && !a.getResolvedAt().isBefore(startOfDay))
                .map(a -> a.getOverdueAmount() != null ? a.getOverdueAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String agentName = collectionAccountRepository.findByAgentId(agentId).stream()
                .map(CollectionAccount::getAgentName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(todayLogs.stream()
                        .map(CallLog::getAgentName)
                        .filter(name -> name != null && !name.isBlank())
                        .findFirst()
                        .orElse("Unknown"));

        int performanceScore = (int) Math.round(
                (resolvedToday * 100.0) / Math.max(totalAssigned, 1));
        performanceScore = Math.max(0, Math.min(100, performanceScore));

        return AgentPerformanceResponse.builder()
                .agentId(agentId)
                .agentName(agentName)
                .totalAssigned(totalAssigned)
                .resolvedToday(resolvedToday)
                .callsToday(callsToday)
                .promisesToday(promisesToday)
                .collectedToday(collectedToday)
                .performanceScore(performanceScore)
                .build();
    }

    private CollectionStatus resolveStatusByOverdueDays(Integer overdueDays) {
        int days = overdueDays != null ? overdueDays : 0;
        if (days >= 90) {
            return CollectionStatus.LEGAL_NOTICE;
        }
        if (days >= 60) {
            return CollectionStatus.ESCALATED;
        }
        if (days >= 30) {
            return CollectionStatus.IN_PROGRESS;
        }
        return CollectionStatus.ASSIGNED;
    }

    private String buildAiSummary(Disposition disposition) {
        return switch (disposition) {
            case CONNECTED_WILL_PAY ->
                    "Agent connected with borrower. Borrower agreed to pay.";
            case CONNECTED_CANT_PAY ->
                    "Agent connected with borrower. Borrower indicated inability to pay at this time.";
            case NOT_REACHABLE ->
                    "Call attempt made. Borrower not reachable.";
            case NUMBER_INVALID ->
                    "Contact number appears invalid. Requires contact update.";
            case DISPUTED ->
                    "Borrower disputed the outstanding amount. Requires admin review.";
            case ALREADY_PAID ->
                    "Borrower claims payment already made. Marked for resolution.";
            case PROMISE_BROKEN ->
                    "Borrower previously promised payment but did not follow through.";
        };
    }

    private String mapHourToSlot(int hour) {
        if (hour >= 6 && hour < 10) {
            return "Morning (6-9 AM)";
        }
        if (hour >= 10 && hour < 12) {
            return "Late Morning (10 AM-12 PM)";
        }
        if (hour >= 12 && hour < 15) {
            return "Afternoon (12-3 PM)";
        }
        if (hour >= 18 && hour < 21) {
            return "Evening (6-9 PM)";
        }
        return "Evening (6-9 PM)";
    }

    private String resolveBestDay(List<CallLog> callLogs) {
        Map<String, Long> dayCounts = new HashMap<>();
        for (CallLog logEntry : callLogs) {
            if (logEntry.getDisposition() == Disposition.CONNECTED_WILL_PAY && logEntry.getCalledAt() != null) {
                String day = logEntry.getCalledAt().getDayOfWeek().name();
                dayCounts.merge(day, 1L, Long::sum);
            }
        }
        return dayCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Tuesday-Thursday");
    }

    private CollectionAccount findAccount(UUID accountId) {
        return collectionAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Collection account not found: " + accountId));
    }

    private CollectionAccountResponse toAccountResponse(CollectionAccount account) {
        return CollectionAccountResponse.builder()
                .id(account.getId())
                .loanId(account.getLoanId())
                .userId(account.getUserId())
                .userName(account.getUserName())
                .userEmail(account.getUserEmail())
                .agentName(account.getAgentName())
                .overdueAmount(account.getOverdueAmount())
                .overdueDays(account.getOverdueDays())
                .missedEmis(account.getMissedEmis())
                .status(account.getStatus())
                .promisedPayDate(account.getPromisedPayDate())
                .promisedAmount(account.getPromisedAmount())
                .assignedAt(account.getAssignedAt())
                .lastContactedAt(account.getLastContactedAt())
                .build();
    }

    private CallLogResponse toCallLogResponse(CallLog callLog) {
        return CallLogResponse.builder()
                .id(callLog.getId())
                .accountId(callLog.getAccountId())
                .agentId(callLog.getAgentId())
                .agentName(callLog.getAgentName())
                .calledAt(callLog.getCalledAt())
                .durationSeconds(callLog.getDurationSeconds())
                .disposition(callLog.getDisposition())
                .promiseDate(callLog.getPromiseDate())
                .promiseAmount(callLog.getPromiseAmount())
                .notes(callLog.getNotes())
                .aiSummary(callLog.getAiSummary())
                .build();
    }
}
