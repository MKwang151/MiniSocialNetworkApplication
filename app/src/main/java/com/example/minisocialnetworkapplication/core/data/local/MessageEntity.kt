package com.example.minisocialnetworkapplication.core.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.minisocialnetworkapplication.core.domain.model.Message
import com.example.minisocialnetworkapplication.core.domain.model.MessageStatus
import com.example.minisocialnetworkapplication.core.domain.model.MessageType
import com.example.minisocialnetworkapplication.core.domain.model.ReplyMessage
import com.google.firebase.Timestamp
import org.json.JSONArray

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["localId"], unique = true),
        Index(value = ["status"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val localId: String, // UUID for optimistic UI
    val conversationId: String,
    val sequenceId: Long = 0, // Auto-incrementing ID
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String?,
    val type: String, // TEXT, IMAGE, VIDEO, AUDIO, FILE, STICKER, GIF, SYSTEM
    val content: String?, // text content
    val mediaUrls: String?, // JSON array of URLs
    val fileName: String?,
    val fileSize: Long?,
    val duration: Int?, // For VIDEO, AUDIO (in seconds)
    val replyToMessageId: String?,
    val replyToSenderId: String?,
    val replyToSenderName: String?,
    val replyToType: String?,
    val replyToContent: String?,
    val reactions: String?, // JSON object: {"❤️": ["user1", "user2"]}
    val status: String, // PENDING, SENDING, SENT, DELIVERED, SEEN, FAILED
    val deliveredTo: String?, // JSON array of userIds
    val seenBy: String?, // JSON array of userIds
    val isRevoked: Boolean = false,
    val timestamp: Long,
    val createdAtLocal: Long
) {
    /**
     * Convert to domain model
     */
    fun toDomainModel(): Message {
        val mediaUrlsList = parseJsonArray(mediaUrls)
        val deliveredToList = parseJsonArray(deliveredTo)
        val seenByList = parseJsonArray(seenBy)
        val reactionsMap = parseReactions(reactions)

        val replyMessage = if (replyToMessageId != null) {
            ReplyMessage(
                id = replyToMessageId,
                senderId = replyToSenderId ?: "",
                senderName = replyToSenderName ?: "",
                type = try { MessageType.valueOf(replyToType ?: "TEXT") } catch (e: Exception) { MessageType.TEXT },
                content = replyToContent ?: ""
            )
        } else null

        return Message(
            id = id,
            localId = localId,
            conversationId = conversationId,
            sequenceId = sequenceId,
            senderId = senderId,
            senderName = senderName,
            senderAvatarUrl = senderAvatarUrl,
            type = try { MessageType.valueOf(type) } catch (e: Exception) { MessageType.TEXT },
            content = content ?: "",
            mediaUrls = mediaUrlsList,
            fileName = fileName,
            fileSize = fileSize,
            duration = duration,
            replyToMessageId = replyToMessageId,
            replyToMessage = replyMessage,
            reactions = reactionsMap,
            status = try { MessageStatus.valueOf(status) } catch (e: Exception) { MessageStatus.SENT },
            deliveredTo = deliveredToList,
            seenBy = seenByList,
            isRevoked = isRevoked,
            timestamp = Timestamp(java.util.Date(timestamp)),
            createdAtLocal = createdAtLocal
        )
    }

    private fun parseJsonArray(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseReactions(json: String?): Map<String, List<String>> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val result = mutableMapOf<String, List<String>>()
            val jsonObject = org.json.JSONObject(json)
            jsonObject.keys().forEach { key ->
                val array = jsonObject.getJSONArray(key)
                val userIds = (0 until array.length()).map { array.getString(it) }
                result[key] = userIds
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    companion object {
        /**
         * Create entity from domain model
         */
        fun fromDomainModel(message: Message): MessageEntity {
            val mediaUrlsJson = if (message.mediaUrls.isNotEmpty()) {
                JSONArray(message.mediaUrls).toString()
            } else null

            val deliveredToJson = if (message.deliveredTo.isNotEmpty()) {
                JSONArray(message.deliveredTo).toString()
            } else null

            val seenByJson = if (message.seenBy.isNotEmpty()) {
                JSONArray(message.seenBy).toString()
            } else null

            val reactionsJson = if (message.reactions.isNotEmpty()) {
                val jsonObject = org.json.JSONObject()
                message.reactions.forEach { (emoji, userIds) ->
                    jsonObject.put(emoji, JSONArray(userIds))
                }
                jsonObject.toString()
            } else null

            return MessageEntity(
                id = message.id,
                localId = message.localId,
                conversationId = message.conversationId,
                sequenceId = message.sequenceId,
                senderId = message.senderId,
                senderName = message.senderName,
                senderAvatarUrl = message.senderAvatarUrl,
                type = message.type.name,
                content = message.content,
                mediaUrls = mediaUrlsJson,
                fileName = message.fileName,
                fileSize = message.fileSize,
                duration = message.duration,
                replyToMessageId = message.replyToMessageId,
                replyToSenderId = message.replyToMessage?.senderId,
                replyToSenderName = message.replyToMessage?.senderName,
                replyToType = message.replyToMessage?.type?.name,
                replyToContent = message.replyToMessage?.content,
                reactions = reactionsJson,
                status = message.status.name,
                deliveredTo = deliveredToJson,
                seenBy = seenByJson,
                isRevoked = message.isRevoked,
                timestamp = message.timestamp.toDate().time,
                createdAtLocal = message.createdAtLocal
            )
        }
    }
}
