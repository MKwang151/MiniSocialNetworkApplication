package com.example.minisocialnetworkapplication.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.google.firebase.Timestamp

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val text: String,
    val mediaUrls: String, // JSON array string
    val likeCount: Int,
    val commentCount: Int,
    val likedByMe: Boolean,
    val createdAt: Long,
    val isSyncPending: Boolean = false
) {
    fun toPost(): Post {
        return Post(
            id = id,
            authorId = authorId,
            authorName = authorName,
            authorAvatarUrl = authorAvatarUrl,
            text = text,
            mediaUrls = mediaUrls.split(",").filter { it.isNotBlank() },
            likeCount = likeCount,
            commentCount = commentCount,
            likedByMe = likedByMe,
            createdAt = Timestamp(createdAt / 1000, ((createdAt % 1000) * 1000000).toInt()),
            isSyncPending = isSyncPending
        )
    }

    companion object {
        fun fromPost(post: Post, likedByMe: Boolean = false): PostEntity {
            return PostEntity(
                id = post.id,
                authorId = post.authorId,
                authorName = post.authorName,
                authorAvatarUrl = post.authorAvatarUrl,
                text = post.text,
                mediaUrls = post.mediaUrls.joinToString(","),
                likeCount = post.likeCount,
                commentCount = post.commentCount,
                likedByMe = likedByMe,
                createdAt = post.createdAt.toDate().time,
                isSyncPending = post.isSyncPending
            )
        }
    }
}

