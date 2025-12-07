package com.example.minisocialnetworkapplication.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.minisocialnetworkapplication.core.domain.model.Conversation
import com.example.minisocialnetworkapplication.core.domain.model.ConversationType
import com.example.minisocialnetworkapplication.core.domain.model.LastMessage
import com.example.minisocialnetworkapplication.core.domain.model.MessageType
import com.google.firebase.Timestamp
import org.json.JSONArray

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val type: String, // DIRECT, GROUP
    val name: String?, // null for direct, group name for group
    val avatarUrl: String?,
    val participantIds: String, // JSON array of user IDs
    val lastMessageText: String?,
    val lastMessageType: String?,
    val lastMessageSenderId: String?,
    val lastMessageSenderName: String?,
    val lastMessageTime: Long?,
    val lastMessageSequenceId: Long = 0, // SequenceId of last message
    val unreadCount: Int = 0, // Cached value, will be calculated from participants
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
) {
    /**
     * Convert to domain model
     * Note: unreadCount will be provided externally from Participant join
     */
    fun toDomainModel(unreadCount: Int = this.unreadCount): Conversation {
        val participantIdsList = try {
            val jsonArray = JSONArray(participantIds)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }

        val lastMessage = if (lastMessageText != null || lastMessageType != null) {
            LastMessage(
                text = lastMessageText ?: "",
                type = try { MessageType.valueOf(lastMessageType ?: "TEXT") } catch (e: Exception) { MessageType.TEXT },
                senderId = lastMessageSenderId ?: "",
                senderName = lastMessageSenderName ?: "",
                sequenceId = lastMessageSequenceId,
                timestamp = lastMessageTime?.let { Timestamp(java.util.Date(it)) } ?: Timestamp.now()
            )
        } else null

        return Conversation(
            id = id,
            type = try { ConversationType.valueOf(type) } catch (e: Exception) { ConversationType.DIRECT },
            name = name,
            avatarUrl = avatarUrl,
            participantIds = participantIdsList,
            lastMessage = lastMessage,
            unreadCount = unreadCount,
            isPinned = isPinned,
            isMuted = isMuted,
            createdAt = Timestamp(java.util.Date(createdAt)),
            updatedAt = Timestamp(java.util.Date(updatedAt))
        )
    }

    companion object {
        /**
         * Create entity from domain model
         */
        fun fromDomainModel(conversation: Conversation): ConversationEntity {
            val participantIdsJson = JSONArray(conversation.participantIds).toString()

            return ConversationEntity(
                id = conversation.id,
                type = conversation.type.name,
                name = conversation.name,
                avatarUrl = conversation.avatarUrl,
                participantIds = participantIdsJson,
                lastMessageText = conversation.lastMessage?.text,
                lastMessageType = conversation.lastMessage?.type?.name,
                lastMessageSenderId = conversation.lastMessage?.senderId,
                lastMessageSenderName = conversation.lastMessage?.senderName,
                lastMessageTime = conversation.lastMessage?.timestamp?.toDate()?.time,
                lastMessageSequenceId = conversation.lastMessage?.sequenceId ?: 0,
                unreadCount = conversation.unreadCount,
                isPinned = conversation.isPinned,
                isMuted = conversation.isMuted,
                createdAt = conversation.createdAt.toDate().time,
                updatedAt = conversation.updatedAt.toDate().time
            )
        }
    }
}
