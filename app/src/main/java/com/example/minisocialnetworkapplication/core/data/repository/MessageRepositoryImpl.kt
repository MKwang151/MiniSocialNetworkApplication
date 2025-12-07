package com.example.minisocialnetworkapplication.core.data.repository

import android.content.Context
import android.net.Uri
import com.example.minisocialnetworkapplication.core.data.local.ConversationDao
import com.example.minisocialnetworkapplication.core.data.local.MessageDao
import com.example.minisocialnetworkapplication.core.data.local.MessageEntity
import com.example.minisocialnetworkapplication.core.domain.model.Message
import com.example.minisocialnetworkapplication.core.domain.model.MessageStatus
import com.example.minisocialnetworkapplication.core.domain.model.MessageType
import com.example.minisocialnetworkapplication.core.domain.model.ReplyMessage
import com.example.minisocialnetworkapplication.core.domain.repository.MessageRepository
import com.example.minisocialnetworkapplication.core.util.ImageCompressor
import com.example.minisocialnetworkapplication.core.util.Result
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    @ApplicationContext private val context: Context
) : MessageRepository {

    companion object {
        private const val COLLECTION_CONVERSATIONS = "conversations"
        private const val COLLECTION_MESSAGES = "messages"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_TYPING = "typing"
    }

    override fun getMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_CONVERSATIONS)
            .document(conversationId)
            .collection(COLLECTION_MESSAGES)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        parseMessage(doc.id, conversationId, doc.data ?: emptyMap())
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                val entities = messages.map { MessageEntity.fromDomainModel(it) }
                kotlinx.coroutines.runBlocking {
                    messageDao.insertAll(entities)
                }

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getMessage(conversationId: String, messageId: String): Result<Message> {
        return try {
            val local = messageDao.getMessageById(messageId)
            if (local != null) {
                return Result.Success(local.toDomainModel())
            }

            val doc = firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(COLLECTION_MESSAGES)
                .document(messageId)
                .get()
                .await()

            if (!doc.exists()) {
                return Result.Error(Exception("Message not found"))
            }

            val message = parseMessage(doc.id, conversationId, doc.data ?: emptyMap())
            messageDao.insertMessage(MessageEntity.fromDomainModel(message))
            Result.Success(message)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun sendTextMessage(
        conversationId: String,
        text: String,
        replyToMessageId: String?
    ): Result<Message> {
        timber.log.Timber.d("sendTextMessage: START conversationId=$conversationId, text=$text")
        return try {
            val currentUser = auth.currentUser 
                ?: return Result.Error(Exception("Not authenticated"))

            timber.log.Timber.d("sendTextMessage: currentUser=${currentUser.uid}")

            val userDoc = firestore.collection(COLLECTION_USERS)
                .document(currentUser.uid)
                .get()
                .await()

            val userName = userDoc.getString("name") ?: "Unknown"
            val userAvatarUrl = userDoc.getString("avatarUrl")

            timber.log.Timber.d("sendTextMessage: userName=$userName")

            var replyMessage: ReplyMessage? = null
            if (replyToMessageId != null) {
                val replyDoc = firestore.collection(COLLECTION_CONVERSATIONS)
                    .document(conversationId)
                    .collection(COLLECTION_MESSAGES)
                    .document(replyToMessageId)
                    .get()
                    .await()
                
                if (replyDoc.exists()) {
                    replyMessage = ReplyMessage(
                        id = replyToMessageId,
                        senderId = replyDoc.getString("senderId") ?: "",
                        senderName = replyDoc.getString("senderName") ?: "",
                        type = try { MessageType.valueOf(replyDoc.getString("type") ?: "TEXT") } catch (e: Exception) { MessageType.TEXT },
                        content = replyDoc.getString("content") ?: ""
                    )
                }
            }

            val localId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val message = Message(
                id = localId,
                localId = localId,
                conversationId = conversationId,
                senderId = currentUser.uid,
                senderName = userName,
                senderAvatarUrl = userAvatarUrl,
                type = MessageType.TEXT,
                content = text,
                replyToMessageId = replyToMessageId,
                replyToMessage = replyMessage,
                status = MessageStatus.SENDING,
                timestamp = Timestamp.now(),
                createdAtLocal = now
            )

            messageDao.insertMessage(MessageEntity.fromDomainModel(message))
            timber.log.Timber.d("sendTextMessage: inserted to local DB")

            val messageRef = firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(COLLECTION_MESSAGES)
                .document()

            val messageData = hashMapOf(
                "senderId" to currentUser.uid,
                "senderName" to userName,
                "senderAvatarUrl" to userAvatarUrl,
                "type" to MessageType.TEXT.name,
                "content" to text,
                "replyToMessageId" to replyToMessageId,
                "replyToSenderId" to replyMessage?.senderId,
                "replyToSenderName" to replyMessage?.senderName,
                "replyToType" to replyMessage?.type?.name,
                "replyToContent" to replyMessage?.content,
                "timestamp" to FieldValue.serverTimestamp()
            )

            messageRef.set(messageData).await()
            timber.log.Timber.d("sendTextMessage: sent to Firestore, messageId=${messageRef.id}")

            messageDao.updateMessageStatusByLocalId(localId, MessageStatus.SENT.name)
            timber.log.Timber.d("sendTextMessage: about to call updateConversationLastMessage")
            updateConversationLastMessage(conversationId, text, MessageType.TEXT, currentUser.uid, userName)
            timber.log.Timber.d("sendTextMessage: SUCCESS")

            Result.Success(message.copy(id = messageRef.id, status = MessageStatus.SENT))
        } catch (e: Exception) {
            timber.log.Timber.e(e, "sendTextMessage: FAILED")
            Result.Error(e)
        }
    }

    override suspend fun sendMediaMessage(
        conversationId: String,
        type: MessageType,
        mediaUris: List<Uri>,
        caption: String?,
        replyToMessageId: String?
    ): Result<Message> {
        return try {
            val currentUser = auth.currentUser 
                ?: return Result.Error(Exception("Not authenticated"))

            val userDoc = firestore.collection(COLLECTION_USERS)
                .document(currentUser.uid)
                .get()
                .await()

            val userName = userDoc.getString("name") ?: "Unknown"
            val userAvatarUrl = userDoc.getString("avatarUrl")

            val localId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val message = Message(
                id = localId,
                localId = localId,
                conversationId = conversationId,
                senderId = currentUser.uid,
                senderName = userName,
                senderAvatarUrl = userAvatarUrl,
                type = type,
                content = caption ?: "",
                status = MessageStatus.SENDING,
                timestamp = Timestamp.now(),
                createdAtLocal = now
            )

            messageDao.insertMessage(MessageEntity.fromDomainModel(message))

            // Upload media files
            val mediaUrls = mutableListOf<String>()
            for (uri in mediaUris) {
                val fileName = "${UUID.randomUUID()}.jpg"
                val ref = storage.reference.child("chat_media/$conversationId/$localId/$fileName")

                // Use ImageCompressor for images
                if (type == MessageType.IMAGE) {
                    val compressedFile = ImageCompressor.compressImage(context, uri)
                    if (compressedFile != null) {
                        ref.putFile(Uri.fromFile(compressedFile)).await()
                    } else {
                        ref.putFile(uri).await()
                    }
                } else {
                    ref.putFile(uri).await()
                }

                val downloadUrl = ref.downloadUrl.await().toString()
                mediaUrls.add(downloadUrl)
            }

            val messageRef = firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(COLLECTION_MESSAGES)
                .document()

            val messageData = hashMapOf(
                "senderId" to currentUser.uid,
                "senderName" to userName,
                "senderAvatarUrl" to userAvatarUrl,
                "type" to type.name,
                "content" to caption,
                "mediaUrls" to mediaUrls,
                "replyToMessageId" to replyToMessageId,
                "timestamp" to FieldValue.serverTimestamp()
            )

            messageRef.set(messageData).await()
            messageDao.updateMessageStatusByLocalId(localId, MessageStatus.SENT.name)

            val previewText = when (type) {
                MessageType.IMAGE -> "📷 Photo"
                MessageType.VIDEO -> "🎥 Video"
                MessageType.AUDIO -> "🎤 Voice message"
                MessageType.FILE -> "📎 File"
                else -> caption ?: ""
            }
            updateConversationLastMessage(conversationId, previewText, type, currentUser.uid, userName)

            Result.Success(message.copy(id = messageRef.id, status = MessageStatus.SENT, mediaUrls = mediaUrls))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun retryMessage(conversationId: String, localId: String): Result<Message> {
        return try {
            val messageEntity = messageDao.getMessageByLocalId(localId)
                ?: return Result.Error(Exception("Message not found"))

            val message = messageEntity.toDomainModel()

            if (message.type == MessageType.TEXT) {
                sendTextMessage(conversationId, message.content, message.replyToMessageId)
            } else {
                Result.Error(Exception("Cannot retry media messages"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteMessageLocal(conversationId: String, messageId: String): Result<Unit> {
        return try {
            messageDao.deleteMessageById(messageId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun revokeMessage(conversationId: String, messageId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser 
                ?: return Result.Error(Exception("Not authenticated"))

            val messageDoc = firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(COLLECTION_MESSAGES)
                .document(messageId)
                .get()
                .await()

            if (!messageDoc.exists()) {
                return Result.Error(Exception("Message not found"))
            }

            val senderId = messageDoc.getString("senderId")
            if (senderId != currentUser.uid) {
                return Result.Error(Exception("You can only unsend your own messages"))
            }

            val timestamp = messageDoc.getTimestamp("timestamp")
            val fifteenMinutesAgo = System.currentTimeMillis() - (15 * 60 * 1000)
            if (timestamp != null && timestamp.toDate().time < fifteenMinutesAgo) {
                return Result.Error(Exception("Can only unsend messages within 15 minutes"))
            }

            firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(COLLECTION_MESSAGES)
                .document(messageId)
                .update(
                    "isRevoked", true,
                    "content", "Message was unsent"
                )
                .await()

            messageDao.revokeMessage(messageId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun markMessagesAsRead(conversationId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser 
                ?: return Result.Error(Exception("Not authenticated"))

            val unreadMessages = firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(COLLECTION_MESSAGES)
                .whereNotEqualTo("senderId", currentUser.uid)
                .get()
                .await()

            val batch = firestore.batch()
            unreadMessages.documents.forEach { doc ->
                val seenBy = (doc.get("seenBy") as? List<*>)?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()
                if (!seenBy.contains(currentUser.uid)) {
                    seenBy.add(currentUser.uid)
                    batch.update(doc.reference, "seenBy", seenBy)
                }
            }
            batch.commit().await()

            conversationDao.resetUnreadCount(conversationId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun addReaction(
        conversationId: String,
        messageId: String,
        emoji: String
    ): Result<Unit> {
        return try {
            val currentUser = auth.currentUser 
                ?: return Result.Error(Exception("Not authenticated"))

            firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(COLLECTION_MESSAGES)
                .document(messageId)
                .update("reactions.$emoji", FieldValue.arrayUnion(currentUser.uid))
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun removeReaction(
        conversationId: String,
        messageId: String,
        emoji: String
    ): Result<Unit> {
        return try {
            val currentUser = auth.currentUser 
                ?: return Result.Error(Exception("Not authenticated"))

            firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(COLLECTION_MESSAGES)
                .document(messageId)
                .update("reactions.$emoji", FieldValue.arrayRemove(currentUser.uid))
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun setTypingStatus(conversationId: String, isTyping: Boolean): Result<Unit> {
        return try {
            val currentUser = auth.currentUser 
                ?: return Result.Error(Exception("Not authenticated"))

            val typingRef = firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(COLLECTION_TYPING)
                .document(currentUser.uid)

            if (isTyping) {
                typingRef.set(
                    hashMapOf(
                        "isTyping" to true,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                typingRef.delete().await()
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun observeTypingStatus(conversationId: String): Flow<List<String>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid

        val listener = firestore.collection(COLLECTION_CONVERSATIONS)
            .document(conversationId)
            .collection(COLLECTION_TYPING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val typingUserIds = snapshot?.documents
                    ?.filter { it.id != currentUserId && it.getBoolean("isTyping") == true }
                    ?.map { it.id }
                    ?: emptyList()

                trySend(typingUserIds)
            }

        awaitClose { listener.remove() }
    }

    override fun getPendingMessages(): Flow<List<Message>> {
        return messageDao.getPendingMessages().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun searchMessages(conversationId: String, query: String): Flow<List<Message>> {
        return messageDao.searchMessages(conversationId, query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    private suspend fun updateConversationLastMessage(
        conversationId: String,
        text: String,
        type: MessageType,
        senderId: String,
        senderName: String
    ) {
        try {
            timber.log.Timber.d("updateConversationLastMessage: conversationId=$conversationId, text=$text")
            
            val lastMessage = hashMapOf(
                "text" to text,
                "type" to type.name,
                "senderId" to senderId,
                "senderName" to senderName,
                "timestamp" to FieldValue.serverTimestamp()
            )

            firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .update(
                    "lastMessage", lastMessage,
                    "updatedAt", FieldValue.serverTimestamp()
                )
                .await()
            
            timber.log.Timber.d("updateConversationLastMessage: SUCCESS")
        } catch (e: Exception) {
            timber.log.Timber.e(e, "updateConversationLastMessage: FAILED")
        }
    }

    private fun parseMessage(id: String, conversationId: String, data: Map<String, Any?>): Message {
        val type = try {
            MessageType.valueOf(data["type"] as? String ?: "TEXT")
        } catch (e: Exception) {
            MessageType.TEXT
        }

        val mediaUrls = (data["mediaUrls"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val deliveredTo = (data["deliveredTo"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val seenBy = (data["seenBy"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

        @Suppress("UNCHECKED_CAST")
        val reactions = (data["reactions"] as? Map<String, List<String>>) ?: emptyMap()

        val replyToMessageId = data["replyToMessageId"] as? String
        val replyMessage = if (replyToMessageId != null) {
            ReplyMessage(
                id = replyToMessageId,
                senderId = data["replyToSenderId"] as? String ?: "",
                senderName = data["replyToSenderName"] as? String ?: "",
                type = try { MessageType.valueOf(data["replyToType"] as? String ?: "TEXT") } catch (e: Exception) { MessageType.TEXT },
                content = data["replyToContent"] as? String ?: ""
            )
        } else null

        return Message(
            id = id,
            localId = id,
            conversationId = conversationId,
            senderId = data["senderId"] as? String ?: "",
            senderName = data["senderName"] as? String ?: "",
            senderAvatarUrl = data["senderAvatarUrl"] as? String,
            type = type,
            content = data["content"] as? String ?: "",
            mediaUrls = mediaUrls,
            replyToMessageId = replyToMessageId,
            replyToMessage = replyMessage,
            reactions = reactions,
            status = MessageStatus.SENT,
            deliveredTo = deliveredTo,
            seenBy = seenBy,
            isRevoked = data["isRevoked"] as? Boolean ?: false,
            timestamp = data["timestamp"] as? Timestamp ?: Timestamp.now()
        )
    }
}
