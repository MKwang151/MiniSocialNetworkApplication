package com.social.backend.service;

import com.social.backend.model.entity.ReportEntity;

import java.util.List;

public interface ReportService {
    String createReport(ReportEntity report);
    List<ReportEntity> getReports(int limit);
    void updateReportStatus(String reportId, String status);
}
