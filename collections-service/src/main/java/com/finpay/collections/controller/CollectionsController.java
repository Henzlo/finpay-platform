package com.finpay.collections.controller;

import com.finpay.collections.dto.AgentPerformanceResponse;
import com.finpay.collections.dto.AssignAgentRequest;
import com.finpay.collections.dto.BestTimeResponse;
import com.finpay.collections.dto.CallLogRequest;
import com.finpay.collections.dto.CallLogResponse;
import com.finpay.collections.dto.CollectionAccountResponse;
import com.finpay.collections.dto.CreateAccountRequest;
import com.finpay.collections.exception.UnauthorizedException;
import com.finpay.collections.service.CollectionsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/collections")
@RequiredArgsConstructor
@Tag(name = "Collections API")
public class CollectionsController {

    private final CollectionsService collectionsService;

    @GetMapping("/accounts")
    @Operation(summary = "Get all collection accounts")
    public ResponseEntity<List<CollectionAccountResponse>> getAllAccounts(
            @RequestHeader("X-User-Role") String role) {
        requireAdmin(role);
        return ResponseEntity.ok(collectionsService.getAllAccounts());
    }

    @GetMapping("/my-accounts")
    @Operation(summary = "Get agent assigned accounts")
    public ResponseEntity<List<CollectionAccountResponse>> getMyAccounts(
            @RequestHeader("X-User-Id") UUID agentId) {
        return ResponseEntity.ok(collectionsService.getMyAccounts(agentId));
    }

    @GetMapping("/accounts/{id}")
    @Operation(summary = "Get collection account by id")
    public ResponseEntity<CollectionAccountResponse> getAccountById(@PathVariable UUID id) {
        return ResponseEntity.ok(collectionsService.getAccountById(id));
    }

    @PostMapping("/accounts")
    @Operation(summary = "Manually create collection account for testing")
    public ResponseEntity<CollectionAccountResponse> createAccount(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody CreateAccountRequest request) {
        requireAdmin(role);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collectionsService.createAccountManually(request));
    }

    @PostMapping("/assign-agent")
    @Operation(summary = "Assign agent to collection account")
    public ResponseEntity<CollectionAccountResponse> assignAgent(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody AssignAgentRequest request) {
        requireAdmin(role);
        return ResponseEntity.ok(collectionsService.assignAgent(request));
    }

    @PostMapping("/call-log")
    @Operation(summary = "Log a collection call")
    public ResponseEntity<CallLogResponse> logCall(
            @RequestHeader("X-User-Id") UUID agentId,
            @RequestHeader(value = "X-User-Name", required = false) String agentName,
            @Valid @RequestBody CallLogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collectionsService.logCall(request, agentId, agentName));
    }

    @GetMapping("/accounts/{id}/call-logs")
    @Operation(summary = "Get call logs for an account")
    public ResponseEntity<List<CallLogResponse>> getCallLogs(@PathVariable UUID id) {
        return ResponseEntity.ok(collectionsService.getCallLogs(id));
    }

    @GetMapping("/accounts/{id}/best-time")
    @Operation(summary = "AI best time to call")
    public ResponseEntity<BestTimeResponse> getBestTime(@PathVariable UUID id) {
        return ResponseEntity.ok(collectionsService.getBestTimeToCall(id));
    }

    @GetMapping("/agent/performance")
    @Operation(summary = "Get agent performance metrics")
    public ResponseEntity<AgentPerformanceResponse> getAgentPerformance(
            @RequestHeader("X-User-Id") UUID agentId) {
        return ResponseEntity.ok(collectionsService.getAgentPerformance(agentId));
    }

    private void requireAdmin(String role) {
        if (role == null || !("ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role))) {
            throw new UnauthorizedException("Admin role required");
        }
    }
}
