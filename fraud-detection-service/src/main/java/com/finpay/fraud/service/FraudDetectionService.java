package com.finpay.fraud.service;

import com.finpay.fraud.dto.FraudCheckRequest;
import com.finpay.fraud.dto.FraudCheckResponse;
import com.finpay.fraud.dto.FraudStatsResponse;
import com.finpay.fraud.entity.FraudCheck;
import com.finpay.fraud.entity.FraudCheckStatus;
import com.finpay.fraud.entity.RiskLevel;
import com.finpay.fraud.kafka.event.FraudAlertEvent;
import com.finpay.fraud.repository.FraudCheckRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FraudDetectionService {

    private static final String FRAUD_EVENTS_TOPIC = "fraud-events";
    private static final Duration VELOCITY_TTL = Duration.ofMinutes(60);

    @Value("${fraud.rules.max-applications-per-day:3}")
    private int maxApplicationsPerDay;

    @Value("${fraud.rules.suspicious-hour-start:2}")
    private int suspiciousHourStart;

    @Value("${fraud.rules.suspicious-hour-end:5}")
    private int suspiciousHourEnd;

    @Value("${fraud.rules.max-amount-threshold:1000000}")
    private BigDecimal maxAmountThreshold;

    private final FraudCheckRepository fraudCheckRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public FraudDetectionService(
            FraudCheckRepository fraudCheckRepository,
            RedisTemplate<String, String> redisTemplate,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.fraudCheckRepository = fraudCheckRepository;
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public FraudCheckResponse checkFraud(FraudCheckRequest request) {
        int riskScore = 0;
        List<String> triggeredRules = new ArrayList<>();

        LocalDateTime dayAgo = LocalDateTime.now().minusHours(24);
        long applicationCount = fraudCheckRepository.countByUserIdAndCheckedAtAfter(request.getUserId(), dayAgo);
        if (applicationCount >= maxApplicationsPerDay) {
            riskScore += 25;
            triggeredRules.add("DUPLICATE_APPLICATION: Multiple applications in 24 hours");
        }

        int currentHour = LocalDateTime.now().getHour();
        if (currentHour >= suspiciousHourStart && currentHour <= suspiciousHourEnd) {
            riskScore += 15;
            triggeredRules.add("SUSPICIOUS_TIMING: Application at unusual hours");
        }

        if (request.getAmount() != null && request.getAmount().compareTo(maxAmountThreshold) > 0) {
            riskScore += 20;
            triggeredRules.add("HIGH_AMOUNT: Amount exceeds threshold");
        }

        String velocityKey = "fraud:velocity:" + request.getUserId();
        Long velocityCount = redisTemplate.opsForValue().increment(velocityKey);
        if (velocityCount != null && velocityCount == 1L) {
            redisTemplate.expire(velocityKey, VELOCITY_TTL);
        }
        if (velocityCount != null && velocityCount > 5) {
            riskScore += 30;
            triggeredRules.add("HIGH_VELOCITY: Too many requests in short time");
        }

        if (request.getIpAddress() != null && !request.getIpAddress().isBlank()) {
            String ipKey = "fraud:ip:" + request.getIpAddress();
            Long ipCount = redisTemplate.opsForValue().increment(ipKey);
            if (ipCount != null && ipCount == 1L) {
                redisTemplate.expire(ipKey, VELOCITY_TTL);
            }
            if (ipCount != null && ipCount > 10) {
                riskScore += 20;
                triggeredRules.add("IP_VELOCITY: Multiple requests from same IP");
            }
        }

        RiskLevel riskLevel;
        FraudCheckStatus status;
        String recommendation;

        if (riskScore <= 20) {
            riskLevel = RiskLevel.LOW;
            status = FraudCheckStatus.PASSED;
            recommendation = "Transaction approved. No suspicious activity.";
        } else if (riskScore <= 50) {
            riskLevel = RiskLevel.MEDIUM;
            status = FraudCheckStatus.FLAGGED;
            recommendation = "Manual review recommended.";
        } else if (riskScore <= 80) {
            riskLevel = RiskLevel.HIGH;
            status = FraudCheckStatus.FLAGGED;
            recommendation = "High risk detected. Senior review required.";
        } else {
            riskLevel = RiskLevel.VERY_HIGH;
            status = FraudCheckStatus.BLOCKED;
            recommendation = "Transaction blocked. Contact support.";
        }

        FraudCheck check = FraudCheck.builder()
                .referenceId(request.getReferenceId())
                .referenceType(request.getReferenceType())
                .userId(request.getUserId())
                .userEmail(request.getUserEmail())
                .riskLevel(riskLevel)
                .status(status)
                .riskScore(riskScore)
                .triggeredRules(triggeredRules)
                .recommendation(recommendation)
                .ipAddress(request.getIpAddress())
                .build();

        FraudCheck saved = fraudCheckRepository.save(check);
        log.info("Fraud check completed id={} referenceId={} riskLevel={} score={}",
                saved.getId(), saved.getReferenceId(), saved.getRiskLevel(), saved.getRiskScore());

        if (riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.VERY_HIGH) {
            publishFraudAlert(saved);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FraudCheckResponse> getFraudHistory(UUID userId) {
        return fraudCheckRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FraudStatsResponse getFraudStats() {
        List<FraudCheck> allChecks = fraudCheckRepository.findAll();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        long flaggedToday = allChecks.stream()
                .filter(c -> c.getCheckedAt() != null && !c.getCheckedAt().isBefore(startOfDay))
                .filter(c -> c.getStatus() == FraudCheckStatus.FLAGGED)
                .count();

        long blockedToday = allChecks.stream()
                .filter(c -> c.getCheckedAt() != null && !c.getCheckedAt().isBefore(startOfDay))
                .filter(c -> c.getStatus() == FraudCheckStatus.BLOCKED)
                .count();

        long passedToday = allChecks.stream()
                .filter(c -> c.getCheckedAt() != null && !c.getCheckedAt().isBefore(startOfDay))
                .filter(c -> c.getStatus() == FraudCheckStatus.PASSED)
                .count();

        Map<String, Long> riskLevelDistribution = Arrays.stream(RiskLevel.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        level -> allChecks.stream().filter(c -> c.getRiskLevel() == level).count(),
                        (a, b) -> a,
                        LinkedHashMap::new));

        return FraudStatsResponse.builder()
                .totalChecks(allChecks.size())
                .flaggedToday(flaggedToday)
                .blockedToday(blockedToday)
                .passedToday(passedToday)
                .riskLevelDistribution(riskLevelDistribution)
                .build();
    }

    @Transactional(readOnly = true)
    public FraudCheckResponse getFraudCheckById(UUID id) {
        return fraudCheckRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Fraud check not found: " + id));
    }

    private void publishFraudAlert(FraudCheck check) {
        FraudAlertEvent alert = FraudAlertEvent.builder()
                .alertId(UUID.randomUUID().toString())
                .referenceId(check.getReferenceId())
                .referenceType(check.getReferenceType())
                .userId(check.getUserId() != null ? check.getUserId().toString() : null)
                .userEmail(check.getUserEmail())
                .riskLevel(check.getRiskLevel())
                .riskScore(check.getRiskScore())
                .triggeredRules(check.getTriggeredRules())
                .recommendation(check.getRecommendation())
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(FRAUD_EVENTS_TOPIC, check.getReferenceId(), alert);
        log.info("Published fraud alert alertId={} referenceId={} riskLevel={}",
                alert.getAlertId(), alert.getReferenceId(), alert.getRiskLevel());
    }

    private FraudCheckResponse toResponse(FraudCheck check) {
        return FraudCheckResponse.builder()
                .id(check.getId())
                .referenceId(check.getReferenceId())
                .referenceType(check.getReferenceType())
                .riskLevel(check.getRiskLevel())
                .status(check.getStatus())
                .riskScore(check.getRiskScore())
                .triggeredRules(check.getTriggeredRules())
                .recommendation(check.getRecommendation())
                .blocked(check.getStatus() == FraudCheckStatus.BLOCKED)
                .checkedAt(check.getCheckedAt())
                .build();
    }
}
