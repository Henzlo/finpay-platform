package com.finpay.analytics.store;

import com.finpay.analytics.dto.DashboardStats;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class AnalyticsStore {

    private final AtomicLong totalLoans = new AtomicLong(0);
    private final AtomicLong pendingLoans = new AtomicLong(0);
    private final AtomicLong activeLoans = new AtomicLong(0);
    private final AtomicLong totalPayments = new AtomicLong(0);
    private final AtomicReference<BigDecimal> totalCollected =
            new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> totalDisbursed =
            new AtomicReference<>(BigDecimal.ZERO);

    public void incrementLoans() {
        totalLoans.incrementAndGet();
    }

    public void incrementPending() {
        pendingLoans.incrementAndGet();
    }

    public void decrementPending() {
        pendingLoans.updateAndGet(v -> Math.max(0, v - 1));
    }

    public void incrementActive() {
        activeLoans.incrementAndGet();
    }

    public void addPayment(BigDecimal amount) {
        totalPayments.incrementAndGet();
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        totalCollected.updateAndGet(v -> v.add(safeAmount));
    }

    public DashboardStats getStats() {
        return DashboardStats.builder()
                .totalLoans(totalLoans.get())
                .pendingLoans(pendingLoans.get())
                .activeLoans(activeLoans.get())
                .npaLoans(0)
                .totalDisbursed(totalDisbursed.get())
                .totalCollected(totalCollected.get())
                .totalUsers(0)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
