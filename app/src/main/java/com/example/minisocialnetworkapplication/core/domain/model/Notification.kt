package com.example.minisocialnetworkapplication.core.domain.model

data class Notification(
    val id: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.GROUP_INVITATION,
    val title: String = "",
    val message: String = "",
    val data: Map<String, String> = emptyMap(),
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class NotificationType {
    GROUP_INVITATION, FRIEND_REQUEST, POST_LIKE, COMMENT, MENTION
}
