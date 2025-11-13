package com.example.minisocialnetworkapplication.core.domain.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val bio: String? = null,
    val fcmToken: String? = null,
    val createdAt: Timestamp = Timestamp.now()
)

