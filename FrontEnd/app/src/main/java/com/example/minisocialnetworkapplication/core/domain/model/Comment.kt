package com.example.minisocialnetworkapplication.core.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Comment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val text: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    // Reactions: emoji -> list of userIds who reacted
    val reactions: Map<String, List<String>> = emptyMap(),
    // Reply fields
    val replyToId: String? = null,
    val replyToAuthorName: String? = null,
    @get:PropertyName("isHidden")
    val isHidden: Boolean = false
)

