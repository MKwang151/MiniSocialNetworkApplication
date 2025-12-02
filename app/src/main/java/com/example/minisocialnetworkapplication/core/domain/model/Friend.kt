package com.example.minisocialnetworkapplication.core.domain.model

data class Friend(
    val uid: String = "",
    val friendId: String = "",
    val friendName: String = "",
    val friendAvatarUrl: String? = ""
) {
    val id: String get() = uid
}
