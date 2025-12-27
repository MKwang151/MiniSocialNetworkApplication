package com.example.minisocialnetworkapplication.core.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    /**
     * Get all conversations ordered by pinned first, then by last message time
     */
    @Query("""
        SELECT * FROM conversations 
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun getAllConversations(): Flow<List<ConversationEntity>>

    /**
     * Get all conversations synchronously (for fallback when Firestore fails)
     */
    @Query("""
        SELECT * FROM conversations 
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    suspend fun getAllConversationsSync(): List<ConversationEntity>

    /**
     * Get a specific conversation by ID
     */
    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getConversationById(conversationId: String): ConversationEntity?

    /**
     * Get conversation by ID as Flow (for real-time updates)
     */
    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    fun observeConversation(conversationId: String): Flow<ConversationEntity?>

    /**
     * Find direct conversation with a specific user
     */
    @Query("""
        SELECT * FROM conversations 
        WHERE type = 'DIRECT' 
        AND participantIds LIKE '%' || :userId || '%'
        LIMIT 1
    """)
    suspend fun findDirectConversationWithUser(userId: String): ConversationEntity?

    /**
     * Search conversations by name
     */
    @Query("""
        SELECT * FROM conversations 
        WHERE name LIKE '%' || :query || '%'
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun searchConversations(query: String): Flow<List<ConversationEntity>>

    /**
     * Insert a new conversation or replace if exists
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    /**
     * Insert multiple conversations
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(conversations: List<ConversationEntity>)

    /**
     * Update a conversation
     */
    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    /**
     * Update unread count
     */
    @Query("UPDATE conversations SET unreadCount = :count WHERE id = :conversationId")
    suspend fun updateUnreadCount(conversationId: String, count: Int)

    /**
     * Increment unread count
     */
    @Query("UPDATE conversations SET unreadCount = unreadCount + 1 WHERE id = :conversationId")
    suspend fun incrementUnreadCount(conversationId: String)

    /**
     * Mark as read - reset cached unread count
     * Actual read status is tracked in ParticipantEntity
     */
    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun markAsRead(conversationId: String)

    /**
     * Reset unread count to 0
     */
    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun resetUnreadCount(conversationId: String)

    /**
     * Update pinned status
     */
    @Query("UPDATE conversations SET isPinned = :isPinned WHERE id = :conversationId")
    suspend fun updatePinnedStatus(conversationId: String, isPinned: Boolean)

    /**
     * Update muted status
     */
    @Query("UPDATE conversations SET isMuted = :isMuted WHERE id = :conversationId")
    suspend fun updateMutedStatus(conversationId: String, isMuted: Boolean)

    /**
     * Delete a conversation
     */
    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)

    /**
     * Delete conversation by ID
     */
    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversationById(conversationId: String)

    /**
     * Clear all conversations (for logout)
     */
    @Query("DELETE FROM conversations")
    suspend fun clearAll()

    /**
     * Get total unread count across all conversations
     */
    @Query("SELECT SUM(unreadCount) FROM conversations WHERE isMuted = 0")
    fun getTotalUnreadCount(): Flow<Int?>
}
