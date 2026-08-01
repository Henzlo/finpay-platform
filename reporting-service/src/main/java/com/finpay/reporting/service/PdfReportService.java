package com.finpay.reporting.service;

import com.finpay.reporting.dto.DailySummaryData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class PdfReportService {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${reporting.output-dir:/tmp/finpay-reports}")
    private String outputDir;

    public String generatePdfReport(String title, List<DailySummaryData> data, String reportId) {
        ensureOutputDir();

        LocalDate fromDate = data.isEmpty() ? null : data.get(0).getDate();
        LocalDate toDate = data.isEmpty() ? null : data.get(data.size() - 1).getDate();

        long totalLoans = data.stream().mapToLong(DailySummaryData::getTotalLoans).sum();
        BigDecimal totalCollected = data.stream()
                .map(DailySummaryData::getTotalCollected)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDisbursed = data.stream()
                .map(DailySummaryData::getTotalDisbursed)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder content = new StringBuilder();
        content.append("=== FINPAY REPORT ===\n");
        content.append("Title: ").append(title).append("\n");
        content.append("Generated: ").append(LocalDateTime.now().format(DATE_TIME)).append("\n");
        content.append("Period: ").append(fromDate).append(" to ").append(toDate).append("\n\n");
        content.append("SUMMARY:\n");
        content.append("Total Loans: ").append(totalLoans).append("\n");
        content.append("Total Collected: ").append(formatCurrency(totalCollected)).append("\n");
        content.append("Total Disbursed: ").append(formatCurrency(totalDisbursed)).append("\n\n");
        content.append("DAILY BREAKDOWN:\n");
        content.append(String.format("%-12s | %6s | %8s | %12s%n",
                "Date", "Loans", "Approved", "Collected"));

        for (DailySummaryData day : data) {
            content.append(String.format("%-12s | %6d | %8d | %12s%n",
                    day.getDate(),
                    day.getTotalLoans(),
                    day.getApprovedLoans(),
                    formatCurrency(day.getTotalCollected())));
        }

        Path filePath = Paths.get(outputDir, reportId + ".txt");
        try {
            Files.writeString(filePath, content.toString());
            log.info("PDF-style text report written to {}", filePath);
            return filePath.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write PDF report: " + e.getMessage(), e);
        }
    }

    public String generateExcelReport(String title, List<DailySummaryData> data, String reportId) {
        ensureOutputDir();

        StringBuilder csv = new StringBuilder();
        csv.append("# ").append(title).append("\n");
        csv.append("Date,Loans,Approved,Rejected,Disbursed,Payments,Collected\n");

        for (DailySummaryData day : data) {
            csv.append(day.getDate()).append(",")
                    .append(day.getTotalLoans()).append(",")
                    .append(day.getApprovedLoans()).append(",")
                    .append(day.getRejectedLoans()).append(",")
                    .append(nullSafe(day.getTotalDisbursed())).append(",")
                    .append(day.getTotalPayments()).append(",")
                    .append(nullSafe(day.getTotalCollected()))
                    .append("\n");
        }

        Path filePath = Paths.get(outputDir, reportId + ".csv");
        try {
            Files.writeString(filePath, csv.toString());
            log.info("Excel-style CSV report written to {}", filePath);
            return filePath.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write Excel report: " + e.getMessage(), e);
        }
    }

    private void ensureOutputDir() {
        try {
            Files.createDirectories(Paths.get(outputDir));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create output directory: " + outputDir, e);
        }
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "₹0";
        }
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        format.setMaximumFractionDigits(0);
        return "₹" + format.format(amount);
    }

    private String nullSafe(BigDecimal value) {
        return value != null ? value.toPlainString() : "0";
    }
}
