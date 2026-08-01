package com.finpay.loan.service;

import com.finpay.loan.dto.EmiBreakdown;
import com.finpay.loan.dto.EmiCalculationRequest;
import com.finpay.loan.dto.EmiCalculationResponse;
import com.finpay.loan.dto.EmiScheduleResponse;
import com.finpay.loan.dto.LoanApplicationRequest;
import com.finpay.loan.dto.LoanApprovalRequest;
import com.finpay.loan.dto.LoanRejectionRequest;
import com.finpay.loan.dto.LoanResponse;
import com.finpay.loan.entity.EmiSchedule;
import com.finpay.loan.entity.EmiStatus;
import com.finpay.loan.entity.Loan;
import com.finpay.loan.entity.LoanStatus;
import com.finpay.loan.exception.InvalidLoanStateException;
import com.finpay.loan.exception.LoanNotFoundException;
import com.finpay.loan.exception.UnauthorizedException;
import com.finpay.loan.kafka.event.LoanAppliedEvent;
import com.finpay.loan.kafka.event.LoanApprovedEvent;
import com.finpay.loan.kafka.event.LoanRejectedEvent;
import com.finpay.loan.kafka.producer.LoanEventProducer;
import com.finpay.loan.repository.EmiScheduleRepository;
import com.finpay.loan.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanService {

    private static final BigDecimal DEFAULT_INTEREST_RATE = new BigDecimal("12.0");
    private static final int MONEY_SCALE = 2;
    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);

    private final LoanRepository loanRepository;
    private final EmiScheduleRepository emiScheduleRepository;
    private final LoanEventProducer loanEventProducer;

    @Transactional
    public LoanResponse applyLoan(
            LoanApplicationRequest request,
            UUID userId,
            String userName,
            String userEmail) {

        BigDecimal interestRate = DEFAULT_INTEREST_RATE;
        BigDecimal monthlyEmi = calculateMonthlyEmi(request.getAmount(), request.getTenureMonths(), interestRate);

        Loan loan = Loan.builder()
                .userId(userId)
                .userName(userName)
                .userEmail(userEmail)
                .purpose(request.getPurpose())
                .amount(request.getAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .tenureMonths(request.getTenureMonths())
                .interestRate(interestRate)
                .monthlyEmi(monthlyEmi)
                .status(LoanStatus.SUBMITTED)
                .build();

        loan = loanRepository.saveAndFlush(loan);

        LoanAppliedEvent event = LoanAppliedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .loanId(loan.getId().toString())
                .userId(userId.toString())
                .userEmail(userEmail)
                .userName(userName)
                .amount(loan.getAmount())
                .purpose(loan.getPurpose().name())
                .tenureMonths(loan.getTenureMonths())
                .timestamp(LocalDateTime.now())
                .build();
        loanEventProducer.publishLoanApplied(event);

        log.info("Loan applied: {} by {}", loan.getId(), userId);
        return toLoanResponse(loan, List.of());
    }

    @Transactional
    public LoanResponse approveLoan(UUID loanId, UUID adminId, LoanApprovalRequest request) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanId));

        if (!EnumSet.of(LoanStatus.SUBMITTED, LoanStatus.UNDER_REVIEW).contains(loan.getStatus())) {
            throw new InvalidLoanStateException(
                    "Loan cannot be approved from status " + loan.getStatus());
        }

        if (request.getInterestRate() != null) {
            loan.setInterestRate(request.getInterestRate());
            loan.setMonthlyEmi(calculateMonthlyEmi(
                    loan.getAmount(), loan.getTenureMonths(), loan.getInterestRate()));
        }

        loan.setStatus(LoanStatus.APPROVED);
        loan.setApprovedBy(adminId);
        loan.setApprovedAt(LocalDateTime.now());
        loan = loanRepository.save(loan);

        List<EmiSchedule> schedule = generateEmiSchedule(loan);

        LoanApprovedEvent event = LoanApprovedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .loanId(loan.getId().toString())
                .userId(loan.getUserId().toString())
                .userEmail(loan.getUserEmail())
                .amount(loan.getAmount())
                .monthlyEmi(loan.getMonthlyEmi())
                .tenureMonths(loan.getTenureMonths())
                .approvedAt(loan.getApprovedAt())
                .timestamp(LocalDateTime.now())
                .build();
        loanEventProducer.publishLoanApproved(event);

        return toLoanResponse(loan, schedule);
    }

    @Transactional
    public LoanResponse rejectLoan(UUID loanId, UUID adminId, LoanRejectionRequest request) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanId));

        if (!EnumSet.of(LoanStatus.SUBMITTED, LoanStatus.UNDER_REVIEW).contains(loan.getStatus())) {
            throw new InvalidLoanStateException(
                    "Loan cannot be rejected from status " + loan.getStatus());
        }

        loan.setStatus(LoanStatus.REJECTED);
        loan.setRejectionReason(request.getReason());
        loan.setApprovedBy(adminId);
        loan = loanRepository.save(loan);

        LoanRejectedEvent event = LoanRejectedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .loanId(loan.getId().toString())
                .userId(loan.getUserId().toString())
                .userEmail(loan.getUserEmail())
                .reason(request.getReason())
                .timestamp(LocalDateTime.now())
                .build();
        loanEventProducer.publishLoanRejected(event);

        return toLoanResponse(loan, List.of());
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getMyLoans(UUID userId) {
        return loanRepository.findByUserId(userId).stream()
                .map(loan -> toLoanResponse(loan, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public LoanResponse getLoanById(UUID loanId, UUID userId, String role) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanId));

        if (isBorrower(role) && !loan.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not allowed to view this loan");
        }

        List<EmiSchedule> schedule = emiScheduleRepository.findByLoanId(loanId);
        return toLoanResponse(loan, schedule);
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getPendingLoans() {
        return loanRepository.findByStatus(LoanStatus.SUBMITTED).stream()
                .map(loan -> toLoanResponse(loan, List.of()))
                .toList();
    }

    public EmiCalculationResponse calculateEmi(EmiCalculationRequest request) {
        BigDecimal rate = request.getInterestRate() != null
                ? request.getInterestRate()
                : DEFAULT_INTEREST_RATE;
        BigDecimal amount = request.getAmount();
        int tenure = request.getTenureMonths();

        BigDecimal monthlyEmi = calculateMonthlyEmi(amount, tenure, rate);
        List<EmiBreakdown> schedule = buildAmortization(amount, tenure, rate, monthlyEmi);

        BigDecimal totalAmount = monthlyEmi.multiply(BigDecimal.valueOf(tenure))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal totalInterest = totalAmount.subtract(amount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        return EmiCalculationResponse.builder()
                .monthlyEmi(monthlyEmi)
                .totalAmount(totalAmount)
                .totalInterest(totalInterest)
                .principalAmount(amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .tenureMonths(tenure)
                .interestRate(rate)
                .amortizationSchedule(schedule)
                .build();
    }

    private List<EmiSchedule> generateEmiSchedule(Loan loan) {
        BigDecimal monthlyEmi = loan.getMonthlyEmi() != null
                ? loan.getMonthlyEmi()
                : calculateMonthlyEmi(loan.getAmount(), loan.getTenureMonths(), loan.getInterestRate());

        List<EmiBreakdown> amortization = buildAmortization(
                loan.getAmount(), loan.getTenureMonths(), loan.getInterestRate(), monthlyEmi);

        LocalDate startDueDate = LocalDate.now().plusMonths(1);
        List<EmiSchedule> entries = new ArrayList<>();

        for (EmiBreakdown row : amortization) {
            EmiSchedule entry = EmiSchedule.builder()
                    .loanId(loan.getId())
                    .emiNumber(row.getMonth())
                    .dueDate(startDueDate.plusMonths(row.getMonth() - 1L))
                    .principalAmount(row.getPrincipal())
                    .interestAmount(row.getInterest())
                    .totalAmount(row.getEmi())
                    .outstandingBalance(row.getBalance())
                    .status(EmiStatus.PENDING)
                    .build();
            entries.add(entry);
        }

        return emiScheduleRepository.saveAll(entries);
    }

    @Transactional(readOnly = true)
    public List<EmiScheduleResponse> getEmiSchedule(UUID loanId) {
        if (!loanRepository.existsById(loanId)) {
            throw new LoanNotFoundException("Loan not found: " + loanId);
        }
        return emiScheduleRepository.findByLoanId(loanId).stream()
                .map(this::toEmiResponse)
                .toList();
    }

    private BigDecimal calculateMonthlyEmi(BigDecimal amount, int tenureMonths, BigDecimal annualRatePercent) {
        if (tenureMonths <= 0) {
            throw new IllegalArgumentException("Tenure must be positive");
        }
        BigDecimal monthlyRate = annualRatePercent
                .divide(BigDecimal.valueOf(1200), MC);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return amount.divide(BigDecimal.valueOf(tenureMonths), MONEY_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate, MC);
        BigDecimal pow = onePlusR.pow(tenureMonths, MC);
        BigDecimal numerator = amount.multiply(monthlyRate, MC).multiply(pow, MC);
        BigDecimal denominator = pow.subtract(BigDecimal.ONE, MC);
        return numerator.divide(denominator, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private List<EmiBreakdown> buildAmortization(
            BigDecimal amount,
            int tenureMonths,
            BigDecimal annualRatePercent,
            BigDecimal monthlyEmi) {

        BigDecimal monthlyRate = annualRatePercent.divide(BigDecimal.valueOf(1200), MC);
        BigDecimal balance = amount;
        List<EmiBreakdown> rows = new ArrayList<>();

        for (int month = 1; month <= tenureMonths; month++) {
            BigDecimal interest = balance.multiply(monthlyRate, MC)
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal principal = monthlyEmi.subtract(interest).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

            if (month == tenureMonths) {
                principal = balance.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
                monthlyEmi = principal.add(interest).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            }

            balance = balance.subtract(principal).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                balance = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            }

            rows.add(EmiBreakdown.builder()
                    .month(month)
                    .emi(monthlyEmi)
                    .principal(principal)
                    .interest(interest)
                    .balance(balance)
                    .build());
        }
        return rows;
    }

    private boolean isBorrower(String role) {
        return role == null || "BORROWER".equalsIgnoreCase(role);
    }

    private LoanResponse toLoanResponse(Loan loan, List<EmiSchedule> schedule) {
        return LoanResponse.builder()
                .id(loan.getId())
                .userId(loan.getUserId())
                .userName(loan.getUserName())
                .userEmail(loan.getUserEmail())
                .purpose(loan.getPurpose())
                .amount(loan.getAmount())
                .interestRate(loan.getInterestRate())
                .monthlyEmi(loan.getMonthlyEmi())
                .tenureMonths(loan.getTenureMonths())
                .status(loan.getStatus())
                .rejectionReason(loan.getRejectionReason())
                .creditScoreAtApplication(loan.getCreditScoreAtApplication())
                .appliedAt(loan.getAppliedAt())
                .approvedAt(loan.getApprovedAt())
                .emiSchedule(schedule.stream().map(this::toEmiResponse).toList())
                .build();
    }

    private EmiScheduleResponse toEmiResponse(EmiSchedule emi) {
        return EmiScheduleResponse.builder()
                .id(emi.getId())
                .loanId(emi.getLoanId())
                .emiNumber(emi.getEmiNumber())
                .dueDate(emi.getDueDate())
                .principalAmount(emi.getPrincipalAmount())
                .interestAmount(emi.getInterestAmount())
                .totalAmount(emi.getTotalAmount())
                .outstandingBalance(emi.getOutstandingBalance())
                .status(emi.getStatus())
                .paidAt(emi.getPaidAt())
                .build();
    }
}
