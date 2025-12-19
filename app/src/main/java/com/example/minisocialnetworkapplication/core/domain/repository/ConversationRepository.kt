package com.example.minisocialnetworkapplication.core.domain.repository

import com.example.minisocialnetworkapplication.core.domain.model.Conversation
import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.core.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for conversation operations
 */
interface ConversationRepository {

    /**
     * Get all conversations for current user (real-time stream)
     */
    fun getConversations(): Flow<List<Conversation>>

    /**
     * Get a specific conversation by ID
     */
    suspend fun getConversation(conversationId: String): Result<Conversation>

    /**
     * Get or create a direct conversation with another user
     * Returns existing conversation if already exists
     */
    suspend fun getOrCreateDirectConversation(otherUserId: String): Result<Conversation>

    /**
     * Create a new group conversation
     */
    suspend fun createGroupConversation(
        name: String,
        participantIds: List<String>,
        avatarUrl: String? = null
    ): Result<Conversation>

    /**
     * Upload group avatar image
     */
    suspend fun uploadGroupAvatar(uri: android.net.Uri): Result<String>

    /**
     * Update conversation (pin, mute, etc.)
     */
    suspend fun updateConversation(
        conversationId: String,
        isPinned: Boolean? = null,
        isMuted: Boolean? = null
    ): Result<Unit>

    /**
     * Delete conversation (local only)
     */
    suspend fun deleteConversation(conversationId: String): Result<Unit>

    /**
     * Mark all messages in conversation as read
     */
    suspend fun markConversationAsRead(conversationId: String): Result<Unit>

    /**
     * Hide conversation for current user only.
     * Other participants will still see the conversation.
     */
    suspend fun hideConversationForUser(conversationId: String): Result<Unit>

    /**
     * Search conversations by name or participant
     */
    fun searchConversations(query: String): Flow<List<Conversation>>

    /**
     * Get conversation with specific user (for checking if exists)
     */
    suspend fun findDirectConversation(otherUserId: String): Conversation?
    /**
     * Add admin to conversation
     */
    suspend fun addAdmin(conversationId: String, userId: String): Result<Unit>

    /**
     * Remove admin from conversation
     */
    suspend fun removeAdmin(conversationId: String, userId: String): Result<Unit>

    /**
     * Remove participant from conversation (Kick)
     */
    suspend fun removeParticipant(conversationId: String, userId: String): Result<Unit>

    suspend fun addMembers(conversationId: String, memberIds: List<String>): Result<Unit>

    suspend fun createJoinRequests(conversationId: String, memberIds: List<String>): Result<Unit>

    fun getJoinRequests(conversationId: String): kotlinx.coroutines.flow.Flow<List<User>>

    suspend fun acceptJoinRequest(conversationId: String, userId: String): Result<Unit>

    suspend fun declineJoinRequest(conversationId: String, userId: String): Result<Unit>
    suspend fun leaveConversation(conversationId: String): Result<Unit>
    suspend fun deleteConversationPermanent(conversationId: String): Result<Unit>

    suspend fun updateGroupMetadata(
        conversationId: String,
        name: String? = null,
        avatarUrl: String? = null
    ): Result<Unit>
}
