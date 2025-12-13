package com.example.minisocialnetworkapplication.core.domain.model

data class GroupMember(
    val userId: String = "",
    val groupId: String = "",
    val role: GroupRole = GroupRole.MEMBER,
    val joinedAt: Long = System.currentTimeMillis()
)

enum class GroupRole {
    ADMIN, MODERATOR, MEMBER
}
