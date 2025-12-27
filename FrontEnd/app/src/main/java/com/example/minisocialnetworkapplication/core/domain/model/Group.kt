package com.example.minisocialnetworkapplication.core.domain.model

import com.google.firebase.Timestamp

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
    val createdAt: Timestamp = Timestamp.now(),
    val status: String = STATUS_ACTIVE
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_BANNED = "BANNED"
    }
}

enum class GroupPrivacy {
    PUBLIC, PRIVATE
}

enum class GroupPostingPermission {
    EVERYONE, ADMIN_ONLY
}
