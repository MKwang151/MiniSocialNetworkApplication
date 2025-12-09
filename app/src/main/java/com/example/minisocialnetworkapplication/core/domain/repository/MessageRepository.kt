package com.example.minisocialnetworkapplication.core.domain.repository

import android.net.Uri
import com.example.minisocialnetworkapplication.core.domain.model.Message
import com.example.minisocialnetworkapplication.core.domain.model.MessageType
import com.example.minisocialnetworkapplication.core.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for message operations
 */
interface MessageRepository {

    /**
     * Get messages for a conversation (real-time stream with pagination)
     */
    fun getMessages(conversationId: String): Flow<List<Message>>

    /**
     * Get all media messages (images) for a conversation (local)
     */
    fun getMediaMessages(conversationId: String): Flow<List<Message>>

    /**
     * Get a specific message by ID
     */
    suspend fun getMessage(conversationId: String, messageId: String): Result<Message>

    /**
     * Send a text message
     */
    suspend fun sendTextMessage(
        conversationId: String,
        text: String,
        replyToMessageId: String? = null
    ): Result<Message>

    /**
     * Send a media message (image, video, audio, file)
     */
    suspend fun sendMediaMessage(
        conversationId: String,
        type: MessageType,
        mediaUris: List<Uri>,
        caption: String? = null,
        replyToMessageId: String? = null
    ): Result<Message>

    /**
     * Retry sending a failed message
     */
    suspend fun retryMessage(conversationId: String, localId: String): Result<Message>

    /**
     * Delete message (local only - for "Delete for me")
     */
    suspend fun deleteMessageLocal(conversationId: String, messageId: String): Result<Unit>

    /**
     * Revoke/Unsend message (delete for everyone)
     * Only works within 15 minutes of sending
     */
    suspend fun revokeMessage(conversationId: String, messageId: String): Result<Unit>

    /**
     * Mark messages as read (update seenBy)
     */
    suspend fun markMessagesAsRead(conversationId: String): Result<Unit>

    /**
     * Add reaction to message
     */
    suspend fun addReaction(
        conversationId: String,
        messageId: String,
        emoji: String
    ): Result<Unit>

    /**
     * Remove reaction from message
     */
    suspend fun removeReaction(
        conversationId: String,
        messageId: String,
        emoji: String
    ): Result<Unit>

    /**
     * Set typing indicator
     */
    suspend fun setTypingStatus(conversationId: String, isTyping: Boolean): Result<Unit>

    /**
     * Observe typing status of other users in conversation
     */
    fun observeTypingStatus(conversationId: String): Flow<List<String>> // List of userIds who are typing

    /**
     * Get pending messages (failed/pending) for retry
     */
    fun getPendingMessages(): Flow<List<Message>>

    /**
     * Search messages in a conversation
     */
    fun searchMessages(conversationId: String, query: String): Flow<List<Message>>

    /**
     * Pin a message in conversation
     */
    suspend fun pinMessage(conversationId: String, messageId: String): Result<Unit>

    /**
     * Unpin a message in conversation
     */
    suspend fun unpinMessage(conversationId: String, messageId: String): Result<Unit>
}
