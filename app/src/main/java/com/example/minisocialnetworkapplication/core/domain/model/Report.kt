package com.example.minisocialnetworkapplication.core.domain.model

data class Report(
    val id: String = "",
    val postId: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val authorId: String = "",  // Post author being reported
    val groupId: String? = null, // Group ID if post is in a group
    val reason: String = "",
    val description: String = "",
    val status: ReportStatus = ReportStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ReportStatus {
    PENDING,
    REVIEWED,
    RESOLVED,
    DISMISSED
}
