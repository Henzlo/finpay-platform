package com.finpay.credit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.credit.dto.CreditScoreResponse;
import com.finpay.credit.dto.ScoreHistoryResponse;
import com.finpay.credit.dto.SimulationRequest;
import com.finpay.credit.dto.SimulationResponse;
import com.finpay.credit.entity.CreditScore;
import com.finpay.credit.entity.CreditScoreHistory;
import com.finpay.credit.repository.CreditScoreHistoryRepository;
import com.finpay.credit.repository.CreditScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreditScoringService {

    private static final int BASE_SCORE = 600;
    private static final int MIN_SCORE = 300;
    private static final int MAX_SCORE = 900;
    private static final Duration SCORE_CACHE_TTL = Duration.ofHours(1);
    private static final double DEFAULT_MONTHLY_INCOME = 50_000.0;
    private static final int DEFAULT_MONTHS_ACTIVE = 1;

    private final CreditScoreRepository creditScoreRepository;
    private final CreditScoreHistoryRepository creditScoreHistoryRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public CreditScoreResponse calculateScore(UUID userId) {
        return calculateScore(userId, "Score recalculated");
    }

    @Transactional
    public CreditScoreResponse calculateScore(UUID userId, String changeReason) {
        Map<String, Object> payments = readJsonMap(paymentsKey(userId));
        int onTime = asInt(payments.get("onTimePayments"), 0);
        int missed = asInt(payments.get("missedPayments"), 0);
        int totalPayments = onTime + missed;
        // No payment history yet → factor 0 (do not award full payment points)
        double paymentRatio = totalPayments == 0 ? 0.0 : (onTime * 100.0) / totalPayments;
        int paymentFactor = clamp((int) Math.round(paymentRatio), 0, 100);
        double paymentPoints = paymentFactor * 1.2;

        Map<String, Object> burden = readJsonMap(burdenKey(userId));
        double monthlyIncome = asDouble(burden.get("monthlyIncome"), DEFAULT_MONTHLY_INCOME);
        if (monthlyIncome <= 0) {
            monthlyIncome = DEFAULT_MONTHLY_INCOME;
        }
        double totalEmi = asDouble(burden.get("totalEmi"), 0);
        double burdenRatio = (totalEmi / monthlyIncome) * 100.0;
        int burdenFactor = clamp((int) Math.round(Math.max(0, 100 - burdenRatio)), 0, 100);
        double burdenPoints = burdenFactor * 0.75;

        Map<String, Object> loans = readJsonMap(loansKey(userId));
        int activeLoans = asInt(loans.get("activeLoans"), 0);
        int loansFactor = clamp(Math.max(0, 100 - (activeLoans * 20)), 0, 100);
        double loansPoints = loansFactor * 0.6;

        Map<String, Object> age = readJsonMap(ageKey(userId));
        int monthsActive = asInt(age.get("monthsActive"), DEFAULT_MONTHS_ACTIVE);
        if (monthsActive < 1) {
            monthsActive = DEFAULT_MONTHS_ACTIVE;
        }
        int ageFactor = clamp(Math.min(100, monthsActive * 5), 0, 100);
        double agePoints = ageFactor * 0.45;

        int finalScore = clamp(
                (int) Math.round(BASE_SCORE + paymentPoints + burdenPoints + loansPoints + agePoints),
                MIN_SCORE,
                MAX_SCORE);

        CategoryInfo categoryInfo = resolveCategory(finalScore);
        List<String> tips = buildImprovementTips(paymentFactor, burdenFactor, loansFactor);
        List<String> positives = buildPositiveFactors(paymentFactor, burdenFactor, loansFactor, ageFactor);

        Integer previousScore = creditScoreRepository.findTopByUserIdOrderByCalculatedAtDesc(userId)
                .map(CreditScore::getScore)
                .orElse(null);

        CreditScore saved = creditScoreRepository.save(CreditScore.builder()
                .userId(userId)
                .score(finalScore)
                .category(categoryInfo.category())
                .paymentFactor(paymentFactor)
                .burdenFactor(burdenFactor)
                .loansFactor(loansFactor)
                .ageFactor(ageFactor)
                .changeReason(changeReason)
                .build());

        if (previousScore != null && !previousScore.equals(finalScore)) {
            creditScoreHistoryRepository.save(CreditScoreHistory.builder()
                    .userId(userId)
                    .oldScore(previousScore)
                    .newScore(finalScore)
                    .change(finalScore - previousScore)
                    .reason(changeReason)
                    .build());
        }

        CreditScoreResponse response = CreditScoreResponse.builder()
                .userId(userId)
                .score(finalScore)
                .category(categoryInfo.category())
                .categoryColor(categoryInfo.color())
                .maxLoanEligible(categoryInfo.maxLoanEligible())
                .approvalChance(categoryInfo.approvalChance())
                .improvementTips(tips)
                .positiveFactors(positives)
                .calculatedAt(saved.getCalculatedAt())
                .build();

        cacheScore(userId, response);
        log.info("Calculated credit score={} category={} for userId={}", finalScore, categoryInfo.category(), userId);
        return response;
    }

    public CreditScoreResponse getLatestScore(UUID userId) {
        String cached = redisTemplate.opsForValue().get(scoreCacheKey(userId));
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, CreditScoreResponse.class);
            } catch (JsonProcessingException ex) {
                log.warn("Failed to deserialize cached score for userId={}", userId, ex);
            }
        }
        return calculateScore(userId);
    }

    public List<ScoreHistoryResponse> getScoreHistory(UUID userId) {
        return creditScoreHistoryRepository.findByUserIdOrderByChangedAtDesc(userId).stream()
                .map(history -> ScoreHistoryResponse.builder()
                        .score(history.getNewScore())
                        .change(history.getChange())
                        .reason(history.getReason())
                        .changeType(resolveChangeType(history.getChange()))
                        .date(history.getChangedAt())
                        .build())
                .toList();
    }

    public SimulationResponse simulateScoreChange(SimulationRequest request) {
        UUID userId = request.getUserId();
        CreditScoreResponse current = getLatestScore(userId);
        int currentScore = current.getScore();
        String action = request.getAction() == null ? "" : request.getAction().trim().toUpperCase();

        int projected;
        String message;
        String advice;

        switch (action) {
            case "ON_TIME_PAYMENT" -> {
                projected = clamp(currentScore + 15, MIN_SCORE, MAX_SCORE);
                message = "Making on-time payments consistently improves your score";
                advice = "Set up auto-pay to never miss EMIs";
            }
            case "MISSED_PAYMENT" -> {
                projected = clamp(currentScore - 30, MIN_SCORE, MAX_SCORE);
                message = "Missing payments significantly impacts your credit score";
                advice = "Always maintain EMI payment buffer";
            }
            case "CLOSE_LOAN" -> {
                projected = clamp(currentScore + 25, MIN_SCORE, MAX_SCORE);
                message = "Closing a loan reduces your burden and improves creditworthiness";
                advice = "Prioritize closing highest EMI loans";
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported simulation action: " + request.getAction()
                            + ". Use CLOSE_LOAN, ON_TIME_PAYMENT, or MISSED_PAYMENT");
        }

        return SimulationResponse.builder()
                .currentScore(currentScore)
                .projectedScore(projected)
                .change(projected - currentScore)
                .message(message)
                .advice(advice)
                .build();
    }

    @Transactional
    public void updatePaymentHistory(String userId, boolean onTime) {
        UUID uid = UUID.fromString(userId);
        Map<String, Object> payments = readJsonMap(paymentsKey(uid));
        int onTimePayments = asInt(payments.get("onTimePayments"), 0);
        int missedPayments = asInt(payments.get("missedPayments"), 0);

        if (onTime) {
            onTimePayments++;
        } else {
            missedPayments++;
        }

        Map<String, Object> updated = new HashMap<>();
        updated.put("onTimePayments", onTimePayments);
        updated.put("missedPayments", missedPayments);
        writeJson(paymentsKey(uid), updated);

        String reason = onTime ? "On-time payment recorded" : "Missed payment recorded";
        calculateScore(uid, reason);
    }

    public void initializeUserCredit(String userId) {
        UUID uid = UUID.fromString(userId);

        writeJson(paymentsKey(uid), Map.of("onTimePayments", 0, "missedPayments", 0));
        writeJson(loansKey(uid), Map.of("activeLoans", 0));
        writeJson(burdenKey(uid), Map.of("monthlyIncome", (int) DEFAULT_MONTHLY_INCOME, "totalEmi", 0));
        writeJson(ageKey(uid), Map.of("monthsActive", DEFAULT_MONTHS_ACTIVE));

        log.info("Initialized credit profile defaults for userId={}", userId);
    }

    @Transactional
    public void handleLoanApproved(String userId, BigDecimal monthlyEmi) {
        UUID uid = UUID.fromString(userId);

        Map<String, Object> loans = readJsonMap(loansKey(uid));
        int activeLoans = asInt(loans.get("activeLoans"), 0) + 1;
        writeJson(loansKey(uid), Map.of("activeLoans", activeLoans));

        Map<String, Object> burden = readJsonMap(burdenKey(uid));
        double monthlyIncome = asDouble(burden.get("monthlyIncome"), DEFAULT_MONTHLY_INCOME);
        double totalEmi = asDouble(burden.get("totalEmi"), 0);
        double emiToAdd = monthlyEmi == null ? 0 : monthlyEmi.doubleValue();
        Map<String, Object> updatedBurden = new HashMap<>();
        updatedBurden.put("monthlyIncome", monthlyIncome);
        updatedBurden.put("totalEmi", totalEmi + emiToAdd);
        writeJson(burdenKey(uid), updatedBurden);

        calculateScore(uid, "Loan approved - active loans and EMI burden updated");
    }

    private void cacheScore(UUID userId, CreditScoreResponse response) {
        try {
            redisTemplate.opsForValue().set(
                    scoreCacheKey(userId),
                    objectMapper.writeValueAsString(response),
                    SCORE_CACHE_TTL);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to cache credit score for userId={}", userId, ex);
        }
    }

    private CategoryInfo resolveCategory(int score) {
        if (score >= 750) {
            return new CategoryInfo("EXCELLENT", "GREEN", 500_000, "HIGH");
        }
        if (score >= 650) {
            return new CategoryInfo("GOOD", "YELLOW", 300_000, "MEDIUM");
        }
        if (score >= 550) {
            return new CategoryInfo("FAIR", "ORANGE", 150_000, "LOW");
        }
        return new CategoryInfo("POOR", "RED", 50_000, "VERY LOW");
    }

    private List<String> buildImprovementTips(int paymentFactor, int burdenFactor, int loansFactor) {
        List<String> tips = new ArrayList<>();
        if (paymentFactor < 70) {
            tips.add("Pay EMIs on time consistently");
        }
        if (burdenFactor < 60) {
            tips.add("Reduce existing loan burden");
        }
        if (loansFactor < 60) {
            tips.add("Close some existing loans");
        }
        tips.add("Maintain account for longer duration");
        return tips;
    }

    private List<String> buildPositiveFactors(int paymentFactor, int burdenFactor, int loansFactor, int ageFactor) {
        List<String> positives = new ArrayList<>();
        if (paymentFactor >= 70) {
            positives.add("Strong on-time payment history");
        }
        if (burdenFactor >= 60) {
            positives.add("Healthy EMI-to-income ratio");
        }
        if (loansFactor >= 60) {
            positives.add("Manageable number of active loans");
        }
        if (ageFactor >= 50) {
            positives.add("Established account age");
        }
        return positives;
    }

    private String resolveChangeType(Integer change) {
        if (change == null || change == 0) {
            return "NEUTRAL";
        }
        return change > 0 ? "IMPROVED" : "DECLINED";
    }

    private Map<String, Object> readJsonMap(String key) {
        String raw = redisTemplate.opsForValue().get(key);
        if (raw == null || raw.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse Redis key={}", key, ex);
            return new HashMap<>();
        }
    }

    private void writeJson(String key, Map<String, ?> value) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to write Redis key=" + key, ex);
        }
    }

    private static int asInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static double asDouble(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String paymentsKey(UUID userId) {
        return "credit:payments:" + userId;
    }

    private static String burdenKey(UUID userId) {
        return "credit:burden:" + userId;
    }

    private static String loansKey(UUID userId) {
        return "credit:loans:" + userId;
    }

    private static String ageKey(UUID userId) {
        return "credit:age:" + userId;
    }

    private static String scoreCacheKey(UUID userId) {
        return "credit:score:" + userId;
    }

    private record CategoryInfo(
            String category,
            String color,
            int maxLoanEligible,
            String approvalChance) {
    }
}
