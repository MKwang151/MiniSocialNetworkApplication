package com.example.minisocialnetworkapplication.core.domain.model

import com.google.firebase.Timestamp

data class JoinRequest(
    val id: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String? = null,
    val inviterId: String? = null,      // If invited by someone (null = direct join request)
    val inviterName: String? = null,
    val inviterRole: GroupRole? = null, // MEMBER or ADMIN who invited
    val status: JoinRequestStatus = JoinRequestStatus.PENDING,
    val createdAt: Timestamp = Timestamp.now()
)

enum class JoinRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}
