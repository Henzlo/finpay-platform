package com.finpay.loan.controller;

import com.finpay.loan.dto.EmiCalculationRequest;
import com.finpay.loan.dto.EmiCalculationResponse;
import com.finpay.loan.dto.EmiScheduleResponse;
import com.finpay.loan.dto.LoanApplicationRequest;
import com.finpay.loan.dto.LoanApprovalRequest;
import com.finpay.loan.dto.LoanRejectionRequest;
import com.finpay.loan.dto.LoanResponse;
import com.finpay.loan.exception.UnauthorizedException;
import com.finpay.loan.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
@Tag(name = "Loan API")
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/apply")
    @Operation(summary = "Apply for a new loan")
    public ResponseEntity<LoanResponse> apply(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Name", required = false) String userName,
            @Valid @RequestBody LoanApplicationRequest request) {

        String name = userName != null ? userName : (userEmail != null ? userEmail : userId.toString());
        String email = userEmail != null ? userEmail : "";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loanService.applyLoan(request, userId, name, email));
    }

    @GetMapping("/my-loans")
    @Operation(summary = "List loans for the current user")
    public ResponseEntity<List<LoanResponse>> myLoans(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(loanService.getMyLoans(userId));
    }

    @GetMapping("/pending")
    @Operation(summary = "List pending loan applications (admin)")
    public ResponseEntity<List<LoanResponse>> pending(
            @RequestHeader("X-User-Role") String role) {
        requireAdmin(role);
        return ResponseEntity.ok(loanService.getPendingLoans());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan by id")
    public ResponseEntity<LoanResponse> getById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(loanService.getLoanById(id, userId, role));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve a loan (admin)")
    public ResponseEntity<LoanResponse> approve(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader("X-User-Role") String role,
            @RequestBody(required = false) LoanApprovalRequest request) {
        requireAdmin(role);
        LoanApprovalRequest body = request != null ? request : new LoanApprovalRequest();
        return ResponseEntity.ok(loanService.approveLoan(id, adminId, body));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject a loan (admin)")
    public ResponseEntity<LoanResponse> reject(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody LoanRejectionRequest request) {
        requireAdmin(role);
        return ResponseEntity.ok(loanService.rejectLoan(id, adminId, request));
    }

    @PostMapping("/calculate-emi")
    @Operation(summary = "Calculate EMI and amortization schedule")
    public ResponseEntity<EmiCalculationResponse> calculateEmi(
            @Valid @RequestBody EmiCalculationRequest request) {
        return ResponseEntity.ok(loanService.calculateEmi(request));
    }

    @GetMapping("/{id}/emi-schedule")
    @Operation(summary = "Get EMI schedule for a loan")
    public ResponseEntity<List<EmiScheduleResponse>> emiSchedule(@PathVariable UUID id) {
        return ResponseEntity.ok(loanService.getEmiSchedule(id));
    }

    private void requireAdmin(String role) {
        if (role == null || !("ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role))) {
            throw new UnauthorizedException("Admin role required");
        }
    }
}
