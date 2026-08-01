package com.finpay.reporting.service;

import com.finpay.reporting.dto.DailySummaryData;
import com.finpay.reporting.dto.ReportRequest;
import com.finpay.reporting.dto.ReportResponse;
import com.finpay.reporting.entity.ReportRecord;
import com.finpay.reporting.entity.ReportStatus;
import com.finpay.reporting.exception.ReportNotFoundException;
import com.finpay.reporting.repository.ReportRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ReportingService {

    private final ReportRecordRepository reportRecordRepository;
    private final DataAggregatorService dataAggregatorService;
    private final PdfReportService pdfReportService;
    private final ReportingService self;

    public ReportingService(ReportRecordRepository reportRecordRepository,
                            DataAggregatorService dataAggregatorService,
                            PdfReportService pdfReportService,
                            @Lazy ReportingService self) {
        this.reportRecordRepository = reportRecordRepository;
        this.dataAggregatorService = dataAggregatorService;
        this.pdfReportService = pdfReportService;
        this.self = self;
    }

    public ReportResponse generateReport(ReportRequest request, String userId) {
        LocalDate[] range = resolveDateRange(request);
        LocalDate fromDate = range[0];
        LocalDate toDate = range[1];

        String reportType = request.getReportType() != null
                ? request.getReportType().toUpperCase()
                : "CUSTOM";
        String fileType = request.getFileType() != null
                ? request.getFileType().toUpperCase()
                : "PDF";
        String reportName = request.getTitle() != null && !request.getTitle().isBlank()
                ? request.getTitle()
                : "FinPay " + reportType + " Report - " + LocalDate.now();

        ReportRecord record = ReportRecord.builder()
                .reportType(reportType)
                .reportName(reportName)
                .generatedBy(userId)
                .fileType(fileType)
                .fromDate(fromDate)
                .toDate(toDate)
                .status(ReportStatus.GENERATING)
                .build();

        record = reportRecordRepository.save(record);

        ReportRequest asyncRequest = copyRequest(request, reportType, fileType, fromDate, toDate, reportName);
        self.generateReportAsync(record.getId(), asyncRequest);

        return toResponse(record);
    }

    @Async
    public void generateReportAsync(UUID recordId, ReportRequest request) {
        ReportRecord record = reportRecordRepository.findById(recordId).orElse(null);
        if (record == null) {
            log.error("Report record not found for async generation: {}", recordId);
            return;
        }

        try {
            List<DailySummaryData> data = dataAggregatorService
                    .getDateRangeSummary(record.getFromDate(), record.getToDate());

            String filePath;
            if ("EXCEL".equalsIgnoreCase(request.getFileType())) {
                filePath = pdfReportService.generateExcelReport(
                        record.getReportName(), data, record.getId().toString());
            } else {
                filePath = pdfReportService.generatePdfReport(
                        record.getReportName(), data, record.getId().toString());
            }

            long fileSize = Files.size(Path.of(filePath));
            record.setFilePath(filePath);
            record.setFileSizeBytes(fileSize);
            record.setStatus(ReportStatus.COMPLETED);
            reportRecordRepository.save(record);
            log.info("Report generated: {}", record.getId());
        } catch (Exception ex) {
            log.error("Report generation failed for {}: {}", record.getId(), ex.getMessage(), ex);
            record.setStatus(ReportStatus.FAILED);
            record.setErrorMessage(ex.getMessage());
            reportRecordRepository.save(record);
        }
    }

    public List<ReportResponse> getMyReports(String userId) {
        return reportRecordRepository.findByGeneratedByOrderByGeneratedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public byte[] downloadReport(UUID reportId) {
        ReportRecord record = reportRecordRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found: " + reportId));

        if (record.getFilePath() == null || record.getStatus() != ReportStatus.COMPLETED) {
            throw new IllegalStateException("Report file is not ready for download");
        }

        try {
            return Files.readAllBytes(Path.of(record.getFilePath()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read report file: " + e.getMessage(), e);
        }
    }

    public ReportResponse getReportById(UUID id) {
        ReportRecord record = reportRecordRepository.findById(id)
                .orElseThrow(() -> new ReportNotFoundException("Report not found: " + id));
        return toResponse(record);
    }

    public ReportResponse generateDailySummary(String userId) {
        LocalDate today = LocalDate.now();
        List<DailySummaryData> data = List.of(dataAggregatorService.getDailySummary(today));
        String reportName = "FinPay DAILY Report - " + today;

        ReportRecord record = ReportRecord.builder()
                .reportType("DAILY")
                .reportName(reportName)
                .generatedBy(userId)
                .fileType("PDF")
                .fromDate(today)
                .toDate(today)
                .status(ReportStatus.GENERATING)
                .build();
        record = reportRecordRepository.save(record);

        try {
            String filePath = pdfReportService.generatePdfReport(
                    reportName, data, record.getId().toString());
            record.setFilePath(filePath);
            record.setFileSizeBytes(Files.size(Path.of(filePath)));
            record.setStatus(ReportStatus.COMPLETED);
            reportRecordRepository.save(record);
            log.info("Daily summary report generated: {}", record.getId());
        } catch (Exception ex) {
            record.setStatus(ReportStatus.FAILED);
            record.setErrorMessage(ex.getMessage());
            reportRecordRepository.save(record);
            throw new IllegalStateException("Failed to generate daily summary: " + ex.getMessage(), ex);
        }

        return toResponse(record);
    }

    private LocalDate[] resolveDateRange(ReportRequest request) {
        LocalDate today = LocalDate.now();
        String type = request.getReportType() != null
                ? request.getReportType().toUpperCase()
                : "CUSTOM";

        return switch (type) {
            case "DAILY" -> new LocalDate[]{today, today};
            case "WEEKLY" -> new LocalDate[]{today.minusDays(6), today};
            case "MONTHLY" -> new LocalDate[]{today.minusDays(29), today};
            case "CUSTOM" -> {
                if (request.getFromDate() == null || request.getToDate() == null) {
                    throw new IllegalArgumentException(
                            "fromDate and toDate are required for CUSTOM reports");
                }
                if (request.getFromDate().isAfter(request.getToDate())) {
                    throw new IllegalArgumentException("fromDate must be on or before toDate");
                }
                yield new LocalDate[]{request.getFromDate(), request.getToDate()};
            }
            default -> throw new IllegalArgumentException(
                    "Invalid reportType. Use DAILY, WEEKLY, MONTHLY, or CUSTOM");
        };
    }

    private ReportRequest copyRequest(ReportRequest request, String reportType, String fileType,
                                      LocalDate fromDate, LocalDate toDate, String title) {
        ReportRequest copy = new ReportRequest();
        copy.setReportType(reportType);
        copy.setFileType(fileType);
        copy.setFromDate(fromDate);
        copy.setToDate(toDate);
        copy.setTitle(title != null ? title : request.getTitle());
        return copy;
    }

    private ReportResponse toResponse(ReportRecord record) {
        return ReportResponse.builder()
                .id(record.getId())
                .reportType(record.getReportType())
                .reportName(record.getReportName())
                .fileType(record.getFileType())
                .status(record.getStatus())
                .fileSizeBytes(record.getFileSizeBytes())
                .fromDate(record.getFromDate())
                .toDate(record.getToDate())
                .generatedAt(record.getGeneratedAt())
                .downloadUrl(record.getId() != null ? "/reports/" + record.getId() + "/download" : null)
                .build();
    }
}
