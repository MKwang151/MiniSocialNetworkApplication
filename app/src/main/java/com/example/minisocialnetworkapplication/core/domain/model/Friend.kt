package com.example.minisocialnetworkapplication.core.domain.model

enum class FriendStatus {
    FRIEND,
    REQUEST_SENT,
    REQUEST_RECEIVED,
    NONE
}

data class Friend(
    val friendId: String = "",
    val friendName: String = "",
    val friendAvatarUrl: String? = ""
)
