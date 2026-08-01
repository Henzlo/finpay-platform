package com.finpay.reporting.dto;

import com.finpay.reporting.entity.ReportStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReportResponse {

    private UUID id;
    private String reportType;
    private String reportName;
    private String fileType;
    private ReportStatus status;
    private Long fileSizeBytes;
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalDateTime generatedAt;
    private String downloadUrl;
}
