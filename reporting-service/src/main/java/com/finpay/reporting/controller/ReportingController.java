package com.finpay.reporting.controller;

import com.finpay.reporting.dto.ReportRequest;
import com.finpay.reporting.dto.ReportResponse;
import com.finpay.reporting.service.DataAggregatorService;
import com.finpay.reporting.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Reporting API")
public class ReportingController {

    private final ReportingService reportingService;
    private final DataAggregatorService dataAggregatorService;

    @PostMapping("/generate")
    @Operation(summary = "Generate report async")
    public ResponseEntity<ReportResponse> generateReport(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody ReportRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(reportingService.generateReport(request, userId));
    }

    @GetMapping("/my-reports")
    @Operation(summary = "Get my generated reports")
    public ResponseEntity<List<ReportResponse>> getMyReports(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(reportingService.getMyReports(userId));
    }

    @GetMapping("/daily-summary")
    @Operation(summary = "Quick daily summary report")
    public ResponseEntity<ReportResponse> generateDailySummary(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(reportingService.generateDailySummary(userId));
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get top level metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        return ResponseEntity.ok(dataAggregatorService.getTopMetrics());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get report status")
    public ResponseEntity<ReportResponse> getReportById(@PathVariable UUID id) {
        return ResponseEntity.ok(reportingService.getReportById(id));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download report file")
    public ResponseEntity<byte[]> downloadReport(@PathVariable UUID id) {
        ReportResponse meta = reportingService.getReportById(id);
        byte[] content = reportingService.downloadReport(id);

        String filename = meta.getReportName() != null
                ? meta.getReportName().replaceAll("\\s+", "_")
                : "report-" + id;
        String extension = "EXCEL".equalsIgnoreCase(meta.getFileType()) ? ".csv" : ".txt";
        MediaType mediaType = "EXCEL".equalsIgnoreCase(meta.getFileType())
                ? MediaType.parseMediaType("text/csv")
                : MediaType.TEXT_PLAIN;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + extension + "\"")
                .contentType(mediaType)
                .body(content);
    }
}
