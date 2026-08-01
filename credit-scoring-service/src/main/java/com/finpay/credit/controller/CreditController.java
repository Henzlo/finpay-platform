package com.finpay.credit.controller;

import com.finpay.credit.dto.CreditScoreResponse;
import com.finpay.credit.dto.ScoreHistoryResponse;
import com.finpay.credit.dto.SimulationRequest;
import com.finpay.credit.dto.SimulationResponse;
import com.finpay.credit.exception.UnauthorizedException;
import com.finpay.credit.service.CreditScoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/credit")
@RequiredArgsConstructor
@Tag(name = "Credit Scoring API")
public class CreditController {

    private final CreditScoringService creditScoringService;

    @GetMapping("/score")
    @Operation(summary = "Get my credit score")
    public ResponseEntity<CreditScoreResponse> getMyScore(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(creditScoringService.getLatestScore(userId));
    }

    @GetMapping("/score/{userId}")
    @Operation(summary = "Get user credit score - Admin")
    public ResponseEntity<CreditScoreResponse> getUserScore(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Role") String role) {
        requireAdmin(role);
        return ResponseEntity.ok(creditScoringService.getLatestScore(userId));
    }

    @GetMapping("/history")
    @Operation(summary = "Get score history")
    public ResponseEntity<List<ScoreHistoryResponse>> getHistory(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(creditScoringService.getScoreHistory(userId));
    }

    @PostMapping("/simulate")
    @Operation(summary = "Simulate score change")
    public ResponseEntity<SimulationResponse> simulate(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody SimulationRequest request) {
        if (request.getUserId() == null) {
            request.setUserId(userId);
        }
        return ResponseEntity.ok(creditScoringService.simulateScoreChange(request));
    }

    @PostMapping("/initialize")
    @Operation(summary = "Initialize credit profile")
    public ResponseEntity<CreditScoreResponse> initialize(
            @RequestHeader("X-User-Id") UUID userId) {
        creditScoringService.initializeUserCredit(userId.toString());
        return ResponseEntity.ok(creditScoringService.calculateScore(userId, "Credit profile initialized"));
    }

    private void requireAdmin(String role) {
        if (role == null || !("ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role))) {
            throw new UnauthorizedException("Admin role required");
        }
    }
}
