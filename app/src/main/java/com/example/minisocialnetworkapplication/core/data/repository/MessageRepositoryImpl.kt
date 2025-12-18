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
        private const val COLLECTION_PARTICIPANTS = "participants"
    }

    override fun getMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid
        
        // Get user's deletedAt timestamp for this conversation
        var deletedAt: com.google.firebase.Timestamp? = null
        if (currentUserId != null) {
            try {
                val participantDoc = firestore.collection(COLLECTION_CONVERSATIONS)
                    .document(conversationId)
                    .collection(COLLECTION_PARTICIPANTS)
                    .document(currentUserId)
                    .get()
                    .await()
                deletedAt = participantDoc.getTimestamp("deletedAt")
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Error getting deletedAt for $conversationId")
            }
        }
        
        val listener = firestore.collection(COLLECTION_CONVERSATIONS)
            .document(conversationId)
            .collection(COLLECTION_MESSAGES)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        timber.log.Timber.w("getMessages: Permission denied (User likely logged out). Closing flow.")
                        close()
                    } else {
                        timber.log.Timber.e(error, "getMessages: Error - closing flow gracefully")
                        close() // Close gracefully without throwing
                    }
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val message = parseMessage(doc.id, conversationId, doc.data ?: emptyMap())
                        
                        // Filter out messages before deletedAt
                        if (deletedAt != null && message.timestamp.toDate().before(deletedAt.toDate())) {
                            return@mapNotNull null
                        }
                        
                        message
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

    override fun getMediaMessages(conversationId: String): Flow<List<Message>> {
        return messageDao.getMediaMessages(conversationId).map { entities ->
            entities.map { it.toDomainModel() }
        }
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

            // Delete media files from Firebase Storage if any
            @Suppress("UNCHECKED_CAST")
            val mediaUrls = messageDoc.get("mediaUrls") as? List<String> ?: emptyList()
            if (mediaUrls.isNotEmpty()) {
                timber.log.Timber.d("Revoking message with ${mediaUrls.size} media files")
                mediaUrls.forEach { url ->
                    try {
                        // Get storage reference from URL and delete
                        val storageRef = storage.getReferenceFromUrl(url)
                        storageRef.delete().await()
                        timber.log.Timber.d("Deleted media file: $url")
                    } catch (e: Exception) {
                        timber.log.Timber.e(e, "Failed to delete media file: $url")
                        // Continue with other files even if one fails
                    }
                }
            }

            // Update message in Firestore - clear mediaUrls and mark as revoked
            firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(COLLECTION_MESSAGES)
                .document(messageId)
                .update(
                    "isRevoked", true,
                    "content", "Message was unsent",
                    "mediaUrls", emptyList<String>()
                )
                .await()

            messageDao.revokeMessage(messageId)
            timber.log.Timber.d("Message $messageId revoked successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to revoke message")
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

            val messageRef = firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(COLLECTION_MESSAGES)
                .document(messageId)
            
            // First remove user from the array
            messageRef.update("reactions.$emoji", FieldValue.arrayRemove(currentUser.uid))
                .await()
            
            // Then check if array is now empty and delete the key
            val doc = messageRef.get().await()
            @Suppress("UNCHECKED_CAST")
            val reactions = doc.get("reactions") as? Map<String, List<String>> ?: emptyMap()
            val emojiUsers = reactions[emoji] ?: emptyList()
            
            if (emojiUsers.isEmpty()) {
                // Delete the empty key entirely
                timber.log.Timber.d("REACTION: Deleting empty key $emoji from reactions")
                messageRef.update("reactions.$emoji", FieldValue.delete()).await()
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            timber.log.Timber.e(e, "REACTION: removeReaction error")
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
        timber.log.Timber.d("observeTypingStatus: START for conv=$conversationId, currentUserId=$currentUserId")

        val listener = firestore.collection(COLLECTION_CONVERSATIONS)
            .document(conversationId)
            .collection(COLLECTION_TYPING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        timber.log.Timber.w("observeTypingStatus: Permission denied (User likely logged out). Closing flow.")
                        close()
                    } else {
                        timber.log.Timber.e(error, "observeTypingStatus: ERROR - closing flow gracefully")
                        close() // Close gracefully without throwing
                    }
                    return@addSnapshotListener
                }

                timber.log.Timber.d("observeTypingStatus: snapshot, docs=${snapshot?.documents?.size}")
                
                val typingUserIds = snapshot?.documents
                    ?.filter { 
                        val isTyping = it.getBoolean("isTyping") == true
                        val isNotMe = it.id != currentUserId
                        timber.log.Timber.d("observeTypingStatus: doc=${it.id}, isTyping=$isTyping, isNotMe=$isNotMe")
                        isNotMe && isTyping
                    }
                    ?.map { it.id }
                    ?: emptyList()

                timber.log.Timber.d("observeTypingStatus: typingUserIds=$typingUserIds")
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

    /**
     * Get next sequenceId for a conversation using atomic increment
     */
    private suspend fun getNextSequenceId(conversationId: String): Long {
        return try {
            val convRef = firestore.collection(COLLECTION_CONVERSATIONS).document(conversationId)
            val result = firestore.runTransaction { transaction ->
                val snapshot = transaction.get(convRef)
                val currentSeq = snapshot.getLong("lastSequenceId") ?: 0L
                val nextSeq = currentSeq + 1
                transaction.update(convRef, "lastSequenceId", nextSeq)
                nextSeq
            }.await()
            result
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error getting next sequenceId")
            System.currentTimeMillis() // Fallback to timestamp
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
            // Get next sequenceId
            val sequenceId = getNextSequenceId(conversationId)
            
            timber.log.Timber.d("updateConversationLastMessage: conversationId=$conversationId, text=$text, sequenceId=$sequenceId")
            
            val lastMessage = hashMapOf(
                "text" to text,
                "type" to type.name,
                "senderId" to senderId,
                "senderName" to senderName,
                "sequenceId" to sequenceId,
                "timestamp" to FieldValue.serverTimestamp()
            )

            firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .update(
                    "lastMessage", lastMessage,
                    "updatedAt", FieldValue.serverTimestamp()
                )
                .await()
            
            // Update sender's lastReadSequenceId so their own messages don't count as unread
            firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection("participants")
                .document(senderId)
                .set(mapOf(
                    "lastReadSequenceId" to sequenceId,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
                .await()
            
            // Redundant: Now handled by Firebase Cloud Function (onMessageCreate)
            // for better reliability and performance.
            timber.log.Timber.d("Message notification for conversation $conversationId will be handled by Cloud Functions")
            
            timber.log.Timber.d("updateConversationLastMessage: SUCCESS, updated sender's lastReadSequenceId=$sequenceId")
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
        val rawReactions = (data["reactions"] as? Map<String, List<String>>) ?: emptyMap()
        // Filter out empty arrays to clean up old data
        val reactions = rawReactions.filterValues { it.isNotEmpty() }

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
            sequenceId = (data["sequenceId"] as? Long) ?: 0L,
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

    override suspend fun pinMessage(conversationId: String, messageId: String): Result<Unit> {
        return try {
            timber.log.Timber.d("PIN: Attempting to pin message $messageId in conversation $conversationId")
            
            // Use set with merge to create the field if it doesn't exist
            val docRef = firestore.collection(COLLECTION_CONVERSATIONS).document(conversationId)
            
            // First check if document exists
            val doc = docRef.get().await()
            timber.log.Timber.d("PIN: Document exists: ${doc.exists()}")
            
            if (doc.exists()) {
                // Get current pinned messages
                val currentPinned = doc.get("pinnedMessageIds") as? List<String> ?: emptyList()
                val newPinned = currentPinned.toMutableList().apply {
                    if (!contains(messageId)) add(messageId)
                }
                
                docRef.update("pinnedMessageIds", newPinned).await()
                timber.log.Timber.d("PIN: Message $messageId pinned successfully. Total pinned: ${newPinned.size}")
            } else {
                timber.log.Timber.e("PIN: Conversation document $conversationId does not exist!")
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            timber.log.Timber.e(e, "PIN: Failed to pin message - ${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun unpinMessage(conversationId: String, messageId: String): Result<Unit> {
        return try {
            timber.log.Timber.d("UNPIN: Attempting to unpin message $messageId in conversation $conversationId")
            
            firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .update("pinnedMessageIds", com.google.firebase.firestore.FieldValue.arrayRemove(messageId))
                .await()
            timber.log.Timber.d("UNPIN: Message $messageId unpinned successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            timber.log.Timber.e(e, "UNPIN: Failed to unpin message - ${e.message}")
            Result.Error(e)
        }
    }
}
