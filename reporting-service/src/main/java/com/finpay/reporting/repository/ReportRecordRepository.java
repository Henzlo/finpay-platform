package com.finpay.reporting.repository;

import com.finpay.reporting.entity.ReportRecord;
import com.finpay.reporting.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportRecordRepository extends JpaRepository<ReportRecord, UUID> {

    List<ReportRecord> findByGeneratedByOrderByGeneratedAtDesc(String userId);

    List<ReportRecord> findByReportTypeOrderByGeneratedAtDesc(String reportType);

    List<ReportRecord> findByStatusOrderByGeneratedAtDesc(ReportStatus status);
}
