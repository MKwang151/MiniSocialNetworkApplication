package com.example.minisocialnetworkapplication.core.domain.model

import com.google.firebase.Timestamp

data class Like(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

