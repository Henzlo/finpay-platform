package com.finpay.reporting.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReportRequest {

    private String reportType;

    private String fileType = "PDF";

    private LocalDate fromDate;

    private LocalDate toDate;

    private String title;
}
