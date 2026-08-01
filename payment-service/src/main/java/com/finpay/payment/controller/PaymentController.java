package com.finpay.payment.controller;

import com.finpay.payment.dto.PayEmiRequest;
import com.finpay.payment.dto.PaymentHistoryResponse;
import com.finpay.payment.dto.PaymentResponse;
import com.finpay.payment.service.PaymentService;
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
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payment API")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/pay-emi")
    @Operation(summary = "Pay EMI with idempotency")
    public ResponseEntity<PaymentResponse> payEmi(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @Valid @RequestBody PayEmiRequest request) {
        String email = userEmail != null ? userEmail : "";
        return ResponseEntity.ok(paymentService.payEmi(request, userId, email));
    }

    @GetMapping("/history")
    @Operation(summary = "Get payment history - CQRS read model")
    public ResponseEntity<List<PaymentHistoryResponse>> history(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(paymentService.getPaymentHistory(userId));
    }

    @GetMapping("/loan/{loanId}")
    @Operation(summary = "Get payments for a loan")
    public ResponseEntity<List<PaymentHistoryResponse>> loanPayments(@PathVariable UUID loanId) {
        return ResponseEntity.ok(paymentService.getLoanPayments(loanId));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<PaymentResponse> getById(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }
}
