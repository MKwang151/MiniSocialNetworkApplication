package com.example.minisocialnetworkapplication.core.domain.model

data class GroupInvitation(
    val id: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val groupAvatarUrl: String? = null,
    val inviterId: String = "",
    val inviterName: String = "",
    val inviteeId: String = "",
    val status: InvitationStatus = InvitationStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

enum class InvitationStatus {
    PENDING, ACCEPTED, DECLINED
}
