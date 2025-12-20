package com.example.minisocialnetworkapplication.core.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Post(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val text: String = "",
    val mediaUrls: List<String> = emptyList(),
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val likedByMe: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val isSyncPending: Boolean = false,
    // Group Features
    val groupId: String? = null,
    val groupName: String? = null,
    val groupAvatarUrl: String? = null,
    val approvalStatus: PostApprovalStatus = PostApprovalStatus.APPROVED,
    @get:PropertyName("isPinned")
    val isPinned: Boolean = false,
    val rejectionReason: String? = null,
    @get:PropertyName("isHidden")
    val isHidden: Boolean = false
) {
    fun toggleLike(): Post {
        return copy(
            likedByMe = !likedByMe,
            likeCount = if (likedByMe) likeCount - 1 else likeCount + 1
        )
    }
}

enum class PostApprovalStatus {
    APPROVED, PENDING, REJECTED
}

