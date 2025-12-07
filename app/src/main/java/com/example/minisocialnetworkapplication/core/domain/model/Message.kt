package com.example.minisocialnetworkapplication.core.domain.model

import com.google.firebase.Timestamp
import java.util.UUID

/**
 * Domain model for a chat message
 */
data class Message(
    val id: String = "",
    val localId: String = UUID.randomUUID().toString(), // For optimistic UI
    val conversationId: String = "",
    val sequenceId: Long = 0, // Auto-incrementing ID for unread count calculation
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatarUrl: String? = null,
    val type: MessageType = MessageType.TEXT,
    val content: String = "", // Text content for TEXT type
    val mediaUrls: List<String> = emptyList(), // URLs for IMAGE, VIDEO, AUDIO, FILE
    val fileName: String? = null, // For FILE type
    val fileSize: Long? = null, // For FILE type
    val duration: Int? = null, // For VIDEO, AUDIO (in seconds)
    val replyToMessageId: String? = null,
    val replyToMessage: ReplyMessage? = null, // Quoted message data
    val reactions: Map<String, List<String>> = emptyMap(), // emoji -> list of userIds
    val status: MessageStatus = MessageStatus.SENDING,
    val deliveredTo: List<String> = emptyList(),
    val seenBy: List<String> = emptyList(),
    val isRevoked: Boolean = false,
    val timestamp: Timestamp = Timestamp.now(),
    val createdAtLocal: Long = System.currentTimeMillis()
) {
    /**
     * Check if message is from current user
     */
    fun isOutgoing(currentUserId: String): Boolean = senderId == currentUserId

    /**
     * Check if message has been seen by recipient (for 1-on-1)
     */
    fun isSeenByRecipient(recipientId: String): Boolean = seenBy.contains(recipientId)

    /**
     * Get display text based on message state
     */
    fun getDisplayText(): String {
        return when {
            isRevoked -> "Message was unsent"
            type == MessageType.TEXT -> content
            type == MessageType.IMAGE -> "📷 Photo"
            type == MessageType.VIDEO -> "🎥 Video"
            type == MessageType.AUDIO -> "🎤 Voice message"
            type == MessageType.FILE -> "📎 ${fileName ?: "File"}"
            type == MessageType.STICKER -> "😊 Sticker"
            type == MessageType.GIF -> "GIF"
            type == MessageType.SYSTEM -> content
            else -> content
        }
    }
}

/**
 * Message types supported
 */
enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    STICKER,
    GIF,
    SYSTEM // For system messages like "User joined group"
}

/**
 * Message delivery/read status
 */
enum class MessageStatus {
    PENDING,   // Saved locally, waiting to send
    SENDING,   // Currently uploading
    SENT,      // Successfully uploaded to server
    DELIVERED, // Recipient received the message
    SEEN,      // Recipient read the message
    FAILED     // Failed to send
}

/**
 * Data for reply/quoted message
 */
data class ReplyMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: MessageType = MessageType.TEXT,
    val content: String = "" // Preview text or description
)
