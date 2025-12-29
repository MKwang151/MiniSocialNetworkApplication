package com.example.minisocialnetworkapplication.core.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    /**
     * Get all messages for a conversation ordered by timestamp
     */
    @Query("""
        SELECT * FROM messages 
        WHERE conversationId = :conversationId 
        ORDER BY timestamp DESC
    """)
    fun getMessages(conversationId: String): Flow<List<MessageEntity>>

    /**
     * Get messages for a conversation (paginated)
     */
    @Query("""
        SELECT * FROM messages 
        WHERE conversationId = :conversationId 
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getMessagesPaginated(conversationId: String, limit: Int, offset: Int): List<MessageEntity>

    /**
     * Get a specific message by ID
     */
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?

    /**
     * Get message by local ID (for optimistic UI)
     */
    @Query("SELECT * FROM messages WHERE localId = :localId")
    suspend fun getMessageByLocalId(localId: String): MessageEntity?

    /**
     * Get pending messages (for retry queue)
     */
    @Query("""
        SELECT * FROM messages 
        WHERE status IN ('PENDING', 'FAILED') 
        ORDER BY createdAtLocal ASC
    """)
    fun getPendingMessages(): Flow<List<MessageEntity>>

    /**
     * Get media messages (IMAGES and VIDEOS) for a conversation
     */
    @Query("""
        SELECT * FROM messages 
        WHERE conversationId = :conversationId 
        AND type IN ('IMAGE', 'VIDEO')
        ORDER BY timestamp DESC
    """)
    fun getMediaMessages(conversationId: String): Flow<List<MessageEntity>>

    /**
     * Search messages in a conversation
     */
    @Query("""
        SELECT * FROM messages 
        WHERE conversationId = :conversationId 
        AND content LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchMessages(conversationId: String, query: String): Flow<List<MessageEntity>>

    /**
     * Get the last message in a conversation
     */
    @Query("""
        SELECT * FROM messages 
        WHERE conversationId = :conversationId 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun getLastMessage(conversationId: String): MessageEntity?

    /**
     * Insert a new message
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    /**
     * Insert multiple messages
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    /**
     * Update a message
     */
    @Update
    suspend fun updateMessage(message: MessageEntity)

    /**
     * Update message status
     */
    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    /**
     * Update message status by local ID (don't update id to avoid conflict with Firestore listener)
     */
    @Query("UPDATE messages SET status = :status WHERE localId = :localId")
    suspend fun updateMessageStatusByLocalId(localId: String, status: String)

    /**
     * Mark message as revoked
     */
    @Query("UPDATE messages SET isRevoked = 1, content = 'Message was unsent' WHERE id = :messageId")
    suspend fun revokeMessage(messageId: String)

    /**
     * Update seen status
     */
    @Query("UPDATE messages SET seenBy = :seenBy, status = 'SEEN' WHERE id = :messageId")
    suspend fun updateSeenStatus(messageId: String, seenBy: String)

    /**
     * Delete a message
     */
    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    /**
     * Delete message by ID
     */
    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    /**
     * Delete all messages in a conversation
     */
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteAllMessagesInConversation(conversationId: String)

    /**
     * Clear all messages (for logout)
     */
    @Query("DELETE FROM messages")
    suspend fun clearAll()

    /**
     * Get unread message count in a conversation
     */
    @Query("""
        SELECT COUNT(*) FROM messages 
        WHERE conversationId = :conversationId 
        AND senderId != :currentUserId
        AND (seenBy IS NULL OR seenBy NOT LIKE '%' || :currentUserId || '%')
    """)
    suspend fun getUnreadCount(conversationId: String, currentUserId: String): Int
}
