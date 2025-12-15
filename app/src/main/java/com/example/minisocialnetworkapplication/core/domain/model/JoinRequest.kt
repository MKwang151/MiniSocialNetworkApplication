package com.example.minisocialnetworkapplication.core.domain.model

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
    val createdAt: Long = System.currentTimeMillis()
)

enum class JoinRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}
