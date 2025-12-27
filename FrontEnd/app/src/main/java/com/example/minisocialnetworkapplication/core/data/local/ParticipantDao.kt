package com.example.minisocialnetworkapplication.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ParticipantDao {
    
    /**
     * Insert or update participant
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ParticipantEntity)
    
    /**
     * Insert or update multiple participants
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(participants: List<ParticipantEntity>)
    
    /**
     * Get participant by conversation and user
     */
    @Query("SELECT * FROM participants WHERE conversationId = :conversationId AND userId = :userId")
    suspend fun getParticipant(conversationId: String, userId: String): ParticipantEntity?
    
    /**
     * Get all participants for a conversation
     */
    @Query("SELECT * FROM participants WHERE conversationId = :conversationId")
    fun getParticipantsForConversation(conversationId: String): Flow<List<ParticipantEntity>>
    
    /**
     * Get all conversations for a user
     */
    @Query("SELECT conversationId FROM participants WHERE userId = :userId")
    suspend fun getConversationIdsForUser(userId: String): List<String>
    
    /**
     * Update lastReadSequenceId for a participant
     */
    @Query("UPDATE participants SET lastReadSequenceId = :sequenceId WHERE conversationId = :conversationId AND userId = :userId")
    suspend fun updateLastRead(conversationId: String, userId: String, sequenceId: Long)
    
    /**
     * Delete participant
     */
    @Query("DELETE FROM participants WHERE conversationId = :conversationId AND userId = :userId")
    suspend fun deleteParticipant(conversationId: String, userId: String)
    
    /**
     * Delete all participants for a conversation
     */
    @Query("DELETE FROM participants WHERE conversationId = :conversationId")
    suspend fun deleteAllForConversation(conversationId: String)
    
    /**
     * Clear all participants (for logout)
     */
    @Query("DELETE FROM participants")
    suspend fun clearAll()
}
