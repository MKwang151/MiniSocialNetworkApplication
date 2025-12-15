package com.example.minisocialnetworkapplication.core.domain.model

data class GroupInvitation(
    val id: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val groupAvatarUrl: String? = null,
    val inviterId: String = "",
    val inviterName: String = "",
    val inviterRole: GroupRole? = null,  // ADMIN or MEMBER - affects join behavior for private groups
    val inviteeId: String = "",
    val status: InvitationStatus = InvitationStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

enum class InvitationStatus {
    PENDING, ACCEPTED, DECLINED
}
