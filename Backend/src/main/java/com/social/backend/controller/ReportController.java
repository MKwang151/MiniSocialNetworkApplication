package com.social.backend.controller;

import com.social.backend.model.dto.response.ApiResponse;
import com.social.backend.model.entity.ReportEntity;
import com.social.backend.security.SecurityUtils;
import com.social.backend.service.ReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@SecurityRequirement(name = "Bearer Authentication")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ApiResponse<String> createReport(@RequestBody ReportEntity report) {
        String uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return ApiResponse.error(401, "Unauthorized");
        }
        report.setReporterId(uid);
        String id = reportService.createReport(report);
        return ApiResponse.success(id, "Report created");
    }

    @GetMapping
    public ApiResponse<List<ReportEntity>> getReports(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(reportService.getReports(limit));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateReportStatus(@PathVariable String id, @RequestParam String status) {
        reportService.updateReportStatus(id, status);
        return ApiResponse.success(null, "Report status updated");
    }
}
