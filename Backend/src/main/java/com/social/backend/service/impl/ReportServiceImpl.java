package com.social.backend.service.impl;

import com.social.backend.model.entity.ReportEntity;
import com.social.backend.repository.ReportRepository;
import com.social.backend.service.ReportService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;

    public ReportServiceImpl(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Override
    public String createReport(ReportEntity report) {
        String id = UUID.randomUUID().toString();
        report.setId(id);
        try {
            reportRepository.save(id, report).get();
            return id;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error creating report", e);
        }
    }

    @Override
    public List<ReportEntity> getReports(int limit) {
        try {
            return reportRepository.findAll(limit).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error fetching reports", e);
        }
    }

    @Override
    public void updateReportStatus(String reportId, String status) {
        try {
            ReportEntity report = reportRepository.findById(reportId).get();
            if (report != null) {
                report.setStatus(status);
                reportRepository.save(reportId, report).get();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error updating report", e);
        }
    }
}
