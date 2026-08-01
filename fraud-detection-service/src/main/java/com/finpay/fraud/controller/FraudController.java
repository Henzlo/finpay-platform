package com.finpay.fraud.controller;

import com.finpay.fraud.dto.FraudCheckRequest;
import com.finpay.fraud.dto.FraudCheckResponse;
import com.finpay.fraud.dto.FraudStatsResponse;
import com.finpay.fraud.exception.UnauthorizedException;
import com.finpay.fraud.service.FraudDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/fraud")
@RequiredArgsConstructor
@Tag(name = "Fraud Detection API")
public class FraudController {

    private final FraudDetectionService fraudDetectionService;

    @PostMapping("/check")
    @Operation(summary = "Run fraud check manually")
    public ResponseEntity<FraudCheckResponse> checkFraud(@Valid @RequestBody FraudCheckRequest request) {
        return ResponseEntity.ok(fraudDetectionService.checkFraud(request));
    }

    @GetMapping("/history/{userId}")
    @Operation(summary = "Get fraud history for user")
    public ResponseEntity<List<FraudCheckResponse>> getFraudHistory(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID userId) {
        requireAdmin(role);
        return ResponseEntity.ok(fraudDetectionService.getFraudHistory(userId));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get fraud statistics")
    public ResponseEntity<FraudStatsResponse> getFraudStats(
            @RequestHeader("X-User-Role") String role) {
        requireAdmin(role);
        return ResponseEntity.ok(fraudDetectionService.getFraudStats());
    }

    @GetMapping("/check/{id}")
    @Operation(summary = "Get fraud check by ID")
    public ResponseEntity<FraudCheckResponse> getFraudCheckById(@PathVariable UUID id) {
        return ResponseEntity.ok(fraudDetectionService.getFraudCheckById(id));
    }

    @GetMapping("/my-checks")
    @Operation(summary = "Get my fraud checks")
    public ResponseEntity<List<FraudCheckResponse>> getMyChecks(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(fraudDetectionService.getFraudHistory(userId));
    }

    private void requireAdmin(String role) {
        if (role == null || !("ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role))) {
            throw new UnauthorizedException("Admin role required");
        }
    }
}
