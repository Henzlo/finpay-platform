package com.finpay.reporting.service;

import com.finpay.reporting.dto.DailySummaryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DataAggregatorService {

    private final JdbcTemplate jdbcTemplate;

    public DailySummaryData getDailySummary(LocalDate date) {
        long totalLoans = count(
                "SELECT COUNT(*) FROM loans WHERE DATE(applied_at) = ?", date);
        long approvedLoans = count(
                "SELECT COUNT(*) FROM loans WHERE DATE(approved_at) = ? AND status = 'APPROVED'", date);
        long rejectedLoans = count(
                "SELECT COUNT(*) FROM loans WHERE DATE(applied_at) = ? AND status = 'REJECTED'", date);
        BigDecimal totalDisbursed = sum(
                "SELECT COALESCE(SUM(amount), 0) FROM loans WHERE DATE(approved_at) = ? "
                        + "AND status IN ('APPROVED','ACTIVE','CLOSED')", date);
        long totalPayments = count(
                "SELECT COUNT(*) FROM payments WHERE DATE(created_at) = ? AND status = 'SUCCESS'", date);
        BigDecimal totalCollected = sum(
                "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE DATE(created_at) = ? "
                        + "AND status = 'SUCCESS'", date);
        long newUsers = count(
                "SELECT COUNT(*) FROM users WHERE DATE(created_at) = ?", date);

        return DailySummaryData.builder()
                .date(date)
                .totalLoans(totalLoans)
                .approvedLoans(approvedLoans)
                .rejectedLoans(rejectedLoans)
                .totalDisbursed(totalDisbursed)
                .totalPayments(totalPayments)
                .totalCollected(totalCollected)
                .newUsers(newUsers)
                .generatedAt(LocalDateTime.now().toString())
                .build();
    }

    public List<DailySummaryData> getDateRangeSummary(LocalDate from, LocalDate to) {
        List<DailySummaryData> results = new ArrayList<>();
        LocalDate current = from;
        while (!current.isAfter(to)) {
            results.add(getDailySummary(current));
            current = current.plusDays(1);
        }
        return results;
    }

    public Map<String, Object> getTopMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalLoansEver", count("SELECT COUNT(*) FROM loans"));
        metrics.put("totalCollectedEver", sum(
                "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'SUCCESS'"));
        metrics.put("activeLoans", count(
                "SELECT COUNT(*) FROM loans WHERE status = 'ACTIVE'"));
        metrics.put("npaLoans", count(
                "SELECT COUNT(*) FROM loans WHERE status = 'NPA'"));
        return metrics;
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value != null ? value : 0L;
    }

    private BigDecimal sum(String sql, Object... args) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
        return value != null ? value : BigDecimal.ZERO;
    }
}
