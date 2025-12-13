package com.example.minisocialnetworkapplication.core.domain.model

data class Group(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val ownerId: String = "",
    val privacy: GroupPrivacy = GroupPrivacy.PUBLIC,
    val postingPermission: GroupPostingPermission = GroupPostingPermission.EVERYONE,
    val requirePostApproval: Boolean = false,
    val memberCount: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class GroupPrivacy {
    PUBLIC, PRIVATE
}

enum class GroupPostingPermission {
    EVERYONE, ADMIN_ONLY
}
