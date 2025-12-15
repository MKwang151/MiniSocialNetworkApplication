package com.example.minisocialnetworkapplication.core.domain.repository

import com.example.minisocialnetworkapplication.core.domain.model.Report
import com.example.minisocialnetworkapplication.core.util.Result

interface ReportRepository {
    suspend fun submitReport(report: Report): Result<String>
    suspend fun getReportsForPost(postId: String): Result<List<Report>>
    suspend fun getReportsForGroup(groupId: String): Result<List<Report>>
}
