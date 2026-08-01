package com.finpay.reporting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "report_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String reportType;

    private String reportName;

    private String generatedBy;

    private String filePath;

    private String fileType;

    private Long fileSizeBytes;

    private LocalDate fromDate;

    private LocalDate toDate;

    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    @Column(length = 1000)
    private String errorMessage;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime generatedAt;
}
