package com.example.minisocialnetworkapplication.core.domain.model

import com.google.firebase.firestore.PropertyName

data class Notification(
    val id: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.GROUP_INVITATION,
    val title: String = "",
    val message: String = "",
    val data: Map<String, String> = emptyMap(),
    @get:PropertyName("read") @set:PropertyName("read")
    var isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class NotificationType {
    GROUP_INVITATION, FRIEND_REQUEST, FRIEND_ACCEPTED, POST_LIKE, COMMENT, MENTION, NEW_POST, NEW_MESSAGE, GROUP_METADATA_UPDATE, SYSTEM_WARNING
}

