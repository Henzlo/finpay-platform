package com.finpay.analytics.controller;

import com.finpay.analytics.dto.DashboardStats;
import com.finpay.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics API")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get current dashboard stats")
    public Mono<ResponseEntity<DashboardStats>> getDashboard() {
        return analyticsService.getDashboardStats()
                .map(ResponseEntity::ok);
    }

    @GetMapping(value = "/dashboard/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Live dashboard SSE stream")
    public Flux<ServerSentEvent<DashboardStats>> streamDashboard() {
        return analyticsService.getDashboardStream()
                .map(stats -> ServerSentEvent.<DashboardStats>builder()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .event("dashboard-update")
                        .data(stats)
                        .build());
    }

    @GetMapping("/health-check")
    @Operation(summary = "Analytics service health check")
    public Mono<ResponseEntity<Map<String, String>>> healthCheck() {
        return Mono.just(ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "analytics")));
    }
}
