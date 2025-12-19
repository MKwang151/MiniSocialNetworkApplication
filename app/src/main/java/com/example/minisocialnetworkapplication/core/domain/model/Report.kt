package com.example.minisocialnetworkapplication.core.domain.model

import com.google.firebase.Timestamp

data class Report(
    val id: String = "",
    val targetId: String = "",     // ID of what's being reported (postId, userId, groupId)
    val targetType: String = TYPE_POST, // "POST", "USER", "GROUP"
    val reporterId: String = "",
    val reporterName: String = "",
    val authorId: String = "",      // Author of the reported content (post owner, user reported, group owner)
    val groupId: String? = null,    // Contextual Group ID of the post (if applicable)
    val reason: String = "",
    val description: String = "",
    val status: ReportStatus = ReportStatus.PENDING,
    val createdAt: Timestamp = Timestamp.now()
) {
    companion object {
        const val TYPE_POST = "POST"
        const val TYPE_USER = "USER"
        const val TYPE_GROUP = "GROUP"
    }
}

enum class ReportStatus {
    PENDING,
    REVIEWED,
    RESOLVED,
    DISMISSED
}
