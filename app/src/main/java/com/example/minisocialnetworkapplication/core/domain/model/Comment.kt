package com.example.minisocialnetworkapplication.core.domain.model

import com.google.firebase.Timestamp

data class Comment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val text: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

