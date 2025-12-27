package com.example.minisocialnetworkapplication.core.domain.model

import com.google.firebase.Timestamp

data class GroupMember(
    val userId: String = "",
    val groupId: String = "",
    val role: GroupRole = GroupRole.MEMBER,
    val joinedAt: Timestamp = Timestamp.now()
)

enum class GroupRole {
    CREATOR, ADMIN, MODERATOR, MEMBER
}
