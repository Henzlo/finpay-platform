package com.finpay.analytics.service;

import com.finpay.analytics.dto.DashboardStats;
import com.finpay.analytics.dto.PaymentStatsData;
import com.finpay.analytics.store.AnalyticsStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsStore analyticsStore;

    public Mono<DashboardStats> getDashboardStats() {
        return Mono.just(analyticsStore.getStats());
    }

    public Flux<DashboardStats> getDashboardStream() {
        return Flux.interval(Duration.ofSeconds(3))
                .map(tick -> analyticsStore.getStats())
                .distinctUntilChanged();
    }

    public Flux<PaymentStatsData> getSimpleTrend() {
        return Flux.fromIterable(last7DaysMockData());
    }

    private List<PaymentStatsData> last7DaysMockData() {
        List<PaymentStatsData> data = new ArrayList<>();
        LocalDate today = LocalDate.now();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long successful = random.nextLong(5, 25);
            long failed = random.nextLong(0, 5);
            BigDecimal amount = BigDecimal.valueOf(successful * 5_000L + random.nextLong(0, 10_000));
            data.add(PaymentStatsData.builder()
                    .date(date)
                    .totalPayments(successful + failed)
                    .totalAmount(amount)
                    .successfulPayments(successful)
                    .failedPayments(failed)
                    .build());
        }
        return data;
    }
}
