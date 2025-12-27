package com.example.minisocialnetworkapplication.core.domain.model

import com.google.firebase.Timestamp

/**
 * Participant role in a conversation
 */
enum class ParticipantRole {
    MEMBER,
    ADMIN,
    OWNER
}

/**
 * Domain model for a conversation participant
 * Tracks user's read status in a conversation
 */
data class Participant(
    val id: String = "",
    val conversationId: String = "",
    val userId: String = "",
    val role: ParticipantRole = ParticipantRole.MEMBER,
    val joinedAt: Timestamp = Timestamp.now(),
    val lastReadMessageId: Long = 0 // ID of last message read by this user
)
