package com.example.minisocialnetworkapplication.core.domain.model

import com.google.firebase.Timestamp

/**
 * Domain model for a conversation (chat)
 */
data class Conversation(
    val id: String = "",
    val type: ConversationType = ConversationType.DIRECT,
    val name: String? = null, // null for direct, group name for group
    val avatarUrl: String? = null, // null for direct (use other user's avatar), custom for group
    val participantIds: List<String> = emptyList(),
    val lastMessage: LastMessage? = null,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val pinnedMessageIds: List<String> = emptyList(), // IDs of pinned messages
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

enum class ConversationType {
    DIRECT,  // 1-on-1 chat
    GROUP    // Group chat
}

/**
 * Preview of the last message in conversation list
 */
data class LastMessage(
    val text: String = "",
    val type: MessageType = MessageType.TEXT,
    val senderId: String = "",
    val senderName: String = "",
    val sequenceId: Long = 0, // Sequential ID for unread count calculation
    val timestamp: Timestamp = Timestamp.now()
) {
    /**
     * Get preview text for conversation list
     */
    fun getPreviewText(currentUserId: String): String {
        val prefix = if (senderId == currentUserId) "You: " else ""
        return when (type) {
            MessageType.TEXT -> "$prefix$text"
            MessageType.IMAGE -> "${prefix}📷 Photo"
            MessageType.VIDEO -> "${prefix}🎥 Video"
            MessageType.AUDIO -> "${prefix}🎤 Voice message"
            MessageType.FILE -> "${prefix}📎 File"
            MessageType.STICKER -> "${prefix}😊 Sticker"
            MessageType.GIF -> "${prefix}GIF"
            MessageType.SYSTEM -> text
        }
    }
}
