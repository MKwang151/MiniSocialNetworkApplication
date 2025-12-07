package com.example.minisocialnetworkapplication.core.data.repository

import com.example.minisocialnetworkapplication.core.data.local.ConversationDao
import com.example.minisocialnetworkapplication.core.data.local.ConversationEntity
import com.example.minisocialnetworkapplication.core.data.local.ParticipantDao
import com.example.minisocialnetworkapplication.core.domain.model.Conversation
import com.example.minisocialnetworkapplication.core.domain.model.ConversationType
import com.example.minisocialnetworkapplication.core.domain.model.LastMessage
import com.example.minisocialnetworkapplication.core.domain.model.MessageType
import com.example.minisocialnetworkapplication.core.domain.repository.ConversationRepository
import com.example.minisocialnetworkapplication.core.util.Result
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val conversationDao: ConversationDao,
    private val participantDao: ParticipantDao
) : ConversationRepository {

    companion object {
        private const val COLLECTION_CONVERSATIONS = "conversations"
        private const val COLLECTION_MESSAGES = "messages"
        private const val COLLECTION_PARTICIPANTS = "participants"
    }
    
    /**
     * Get user's lastReadSequenceId for a conversation from Firestore participants subcollection
     */
    private suspend fun getLastReadSequenceId(conversationId: String, userId: String): Long {
        return try {
            val doc = firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(COLLECTION_PARTICIPANTS)
                .document(userId)
                .get()
                .await()
            val result = doc.getLong("lastReadSequenceId") ?: 0L
            timber.log.Timber.d("getLastReadSequenceId: conv=$conversationId, user=$userId, exists=${doc.exists()}, result=$result, data=${doc.data}")
            result
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error getting lastReadSequenceId for $conversationId/$userId")
            0L
        }
    }

    override fun getConversations(): Flow<List<Conversation>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(COLLECTION_CONVERSATIONS)
            .whereArrayContains("participantIds", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    kotlinx.coroutines.runBlocking {
                        val localConversations = conversationDao.getAllConversationsSync()
                        trySend(localConversations.map { it.toDomainModel() })
                    }
                    return@addSnapshotListener
                }

                kotlinx.coroutines.runBlocking {
                    val conversations = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val conv = parseConversation(doc.id, doc.data ?: emptyMap())
                            
                            // Get lastMessageSequenceId from Firestore
                            val lastMessage = doc.data?.get("lastMessage") as? Map<*, *>
                            val lastMsgSequenceId = (lastMessage?.get("sequenceId") as? Long) ?: 0L
                            
                            // Get user's lastReadSequenceId from participants subcollection
                            val lastReadSeqId = getLastReadSequenceId(conv.id, currentUserId)
                            
                            // Calculate unread count
                            val unreadCount = if (lastMsgSequenceId > lastReadSeqId) {
                                (lastMsgSequenceId - lastReadSeqId).toInt()
                            } else {
                                0
                            }
                            
                            timber.log.Timber.d("Conversation ${conv.id}: lastMsgSeq=$lastMsgSequenceId, lastReadSeq=$lastReadSeqId, unread=$unreadCount")
                            
                            conv.copy(
                                unreadCount = unreadCount
                            )
                        } catch (e: Exception) {
                            timber.log.Timber.e(e, "Error parsing conversation ${doc.id}")
                            null
                        }
                    }?.sortedByDescending { it.updatedAt.toDate() } ?: emptyList()

                    // Save to local DB
                    val entities = conversations.map { conv ->
                        val lastMessage = conv.lastMessage
                        ConversationEntity(
                            id = conv.id,
                            type = conv.type.name,
                            name = conv.name,
                            avatarUrl = conv.avatarUrl,
                            participantIds = org.json.JSONArray(conv.participantIds).toString(),
                            lastMessageText = lastMessage?.text,
                            lastMessageType = lastMessage?.type?.name,
                            lastMessageSenderId = lastMessage?.senderId,
                            lastMessageSenderName = lastMessage?.senderName,
                            lastMessageTime = lastMessage?.timestamp?.toDate()?.time,
                            lastMessageSequenceId = lastMessage?.sequenceId ?: 0L,
                            unreadCount = conv.unreadCount,
                            isPinned = conv.isPinned,
                            isMuted = conv.isMuted,
                            createdAt = conv.createdAt.toDate().time,
                            updatedAt = conv.updatedAt.toDate().time
                        )
                    }
                    conversationDao.insertAll(entities)

                    trySend(conversations)
                }
            }

        awaitClose { listener.remove() }
    }


    override suspend fun getConversation(conversationId: String): Result<Conversation> {
        return try {
            val local = conversationDao.getConversationById(conversationId)
            if (local != null) {
                return Result.Success(local.toDomainModel())
            }

            val doc = firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .get()
                .await()

            if (!doc.exists()) {
                return Result.Error(Exception("Conversation not found"))
            }

            val conversation = parseConversation(doc.id, doc.data ?: emptyMap())
            conversationDao.insertConversation(ConversationEntity.fromDomainModel(conversation))
            Result.Success(conversation)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getOrCreateDirectConversation(otherUserId: String): Result<Conversation> {
        return try {
            val currentUserId = auth.currentUser?.uid 
                ?: return Result.Error(Exception("Not authenticated"))

            val existingDoc = firestore.collection(COLLECTION_CONVERSATIONS)
                .whereEqualTo("type", ConversationType.DIRECT.name)
                .whereArrayContains("participantIds", currentUserId)
                .get()
                .await()

            val existing = existingDoc.documents.find { doc ->
                val participants = doc.get("participantIds") as? List<*>
                participants?.contains(otherUserId) == true && participants.size == 2
            }

            if (existing != null) {
                val conversation = parseConversation(existing.id, existing.data ?: emptyMap())
                return Result.Success(conversation)
            }

            val conversationId = firestore.collection(COLLECTION_CONVERSATIONS).document().id

            val conversationData = hashMapOf(
                "type" to ConversationType.DIRECT.name,
                "participantIds" to listOf(currentUserId, otherUserId),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .set(conversationData)
                .await()

            val conversation = Conversation(
                id = conversationId,
                type = ConversationType.DIRECT,
                participantIds = listOf(currentUserId, otherUserId),
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            conversationDao.insertConversation(ConversationEntity.fromDomainModel(conversation))
            Result.Success(conversation)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun createGroupConversation(
        name: String,
        participantIds: List<String>,
        avatarUrl: String?
    ): Result<Conversation> {
        return try {
            val currentUserId = auth.currentUser?.uid 
                ?: return Result.Error(Exception("Not authenticated"))

            val conversationId = firestore.collection(COLLECTION_CONVERSATIONS).document().id
            val allParticipants = (participantIds + currentUserId).distinct()

            val conversationData = hashMapOf(
                "type" to ConversationType.GROUP.name,
                "name" to name,
                "avatarUrl" to avatarUrl,
                "participantIds" to allParticipants,
                "adminIds" to listOf(currentUserId),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .set(conversationData)
                .await()

            val conversation = Conversation(
                id = conversationId,
                type = ConversationType.GROUP,
                name = name,
                avatarUrl = avatarUrl,
                participantIds = allParticipants,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            conversationDao.insertConversation(ConversationEntity.fromDomainModel(conversation))
            Result.Success(conversation)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateConversation(
        conversationId: String,
        isPinned: Boolean?,
        isMuted: Boolean?
    ): Result<Unit> {
        return try {
            isPinned?.let { conversationDao.updatePinnedStatus(conversationId, it) }
            isMuted?.let { conversationDao.updateMutedStatus(conversationId, it) }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> {
        return try {
            conversationDao.deleteConversationById(conversationId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun markConversationAsRead(conversationId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid 
                ?: return Result.Error(Exception("Not authenticated"))
            
            // Get current conversation to get lastMessageSequenceId
            val localConv = conversationDao.getConversationById(conversationId)
            val lastMsgSeqId = localConv?.lastMessageSequenceId ?: 0L
            
            // Update local participant's lastReadSequenceId
            participantDao.updateLastRead(conversationId, currentUserId, lastMsgSeqId)
            
            // Update conversation cached unread count
            conversationDao.markAsRead(conversationId)
            
            // Sync to Firestore participants subcollection
            firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(COLLECTION_PARTICIPANTS)
                .document(currentUserId)
                .set(mapOf(
                    "lastReadSequenceId" to lastMsgSeqId,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
                .await()
            
            timber.log.Timber.d("Marked conversation $conversationId as read, lastReadSeqId=$lastMsgSeqId")
            Result.Success(Unit)
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error marking conversation as read")
            Result.Error(e)
        }
    }

    override fun searchConversations(query: String): Flow<List<Conversation>> {
        return conversationDao.searchConversations(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun findDirectConversation(otherUserId: String): Conversation? {
        val entity = conversationDao.findDirectConversationWithUser(otherUserId)
        return entity?.toDomainModel()
    }

    private fun parseConversation(id: String, data: Map<String, Any?>): Conversation {
        val type = try {
            ConversationType.valueOf(data["type"] as? String ?: "DIRECT")
        } catch (e: Exception) {
            ConversationType.DIRECT
        }

        val participantIds = (data["participantIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

        val lastMessageData = data["lastMessage"] as? Map<*, *>
        timber.log.Timber.d("parseConversation: id=$id, lastMessageData=$lastMessageData")
        
        val lastMessage = if (lastMessageData != null) {
            LastMessage(
                text = lastMessageData["text"] as? String ?: "",
                type = try {
                    MessageType.valueOf(lastMessageData["type"] as? String ?: "TEXT")
                } catch (e: Exception) {
                    MessageType.TEXT
                },
                senderId = lastMessageData["senderId"] as? String ?: "",
                senderName = lastMessageData["senderName"] as? String ?: "",
                sequenceId = (lastMessageData["sequenceId"] as? Long) ?: 0L,
                timestamp = lastMessageData["timestamp"] as? Timestamp ?: Timestamp.now()
            )
        } else null
        
        timber.log.Timber.d("parseConversation: parsed lastMessage=$lastMessage")

        return Conversation(
            id = id,
            type = type,
            name = data["name"] as? String,
            avatarUrl = data["avatarUrl"] as? String,
            participantIds = participantIds,
            lastMessage = lastMessage,
            createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
            updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now()
        )
    }
}
