package com.example.minisocialnetworkapplication.core.data.repository

import com.example.minisocialnetworkapplication.core.data.local.ConversationDao
import com.example.minisocialnetworkapplication.core.data.local.ConversationEntity
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
    private val conversationDao: ConversationDao
) : ConversationRepository {

    companion object {
        private const val COLLECTION_CONVERSATIONS = "conversations"
        private const val COLLECTION_MESSAGES = "messages"
    }
    
    /**
     * Count unread messages in a conversation from Firestore
     */
    private suspend fun countUnreadMessages(
        conversationId: String,
        currentUserId: String,
        lastReadTimestamp: Long
    ): Int {
        return try {
            val query = if (lastReadTimestamp > 0) {
                // Count messages after lastReadTimestamp from others
                firestore.collection(COLLECTION_CONVERSATIONS)
                    .document(conversationId)
                    .collection(COLLECTION_MESSAGES)
                    .whereNotEqualTo("senderId", currentUserId)
                    .whereGreaterThan("timestamp", Timestamp(java.util.Date(lastReadTimestamp)))
                    .get()
                    .await()
            } else {
                // New conversation - count all messages from others
                firestore.collection(COLLECTION_CONVERSATIONS)
                    .document(conversationId)
                    .collection(COLLECTION_MESSAGES)
                    .whereNotEqualTo("senderId", currentUserId)
                    .get()
                    .await()
            }
            query.size()
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error counting unread messages for $conversationId")
            // Fallback: if there's a lastMessage from someone else, at least 1 unread
            1
        }
    }

    override fun getConversations(): Flow<List<Conversation>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // Note: Removed orderBy to avoid composite index requirement
        // Sorting is done in-memory instead
        val listener = firestore.collection(COLLECTION_CONVERSATIONS)
            .whereArrayContains("participantIds", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // On error, try to load from local cache
                    kotlinx.coroutines.runBlocking {
                        val localConversations = conversationDao.getAllConversationsSync()
                        trySend(localConversations.map { it.toDomainModel() })
                    }
                    return@addSnapshotListener
                }

                kotlinx.coroutines.runBlocking {
                    // Get local data first
                    val localConversations = conversationDao.getAllConversationsSync()
                    val localData = localConversations.associateBy { it.id }

                    // Process changes for unread count
                    snapshot?.documentChanges?.forEach { change ->
                        val data = change.document.data
                        val lastMessage = data["lastMessage"] as? Map<*, *>
                        val senderId = lastMessage?.get("senderId") as? String
                        val msgTimestamp = (lastMessage?.get("timestamp") as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
                        
                        when (change.type) {
                            com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                                // New conversation loaded
                                val existing = localData[change.document.id]
                                if (existing == null && senderId != null && senderId != currentUserId) {
                                    // Brand new conversation with message from others
                                    timber.log.Timber.d("New conversation ${change.document.id} - counting unread messages")
                                }
                            }
                            com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                // Existing conversation updated
                                val existing = localData[change.document.id]
                                if (senderId != null && senderId != currentUserId) {
                                    val lastRead = existing?.lastReadTimestamp ?: 0L
                                    if (msgTimestamp > lastRead) {
                                        timber.log.Timber.d("New message in ${change.document.id}, incrementing unread")
                                        conversationDao.incrementUnreadCount(change.document.id)
                                    }
                                }
                            }
                            else -> {}
                        }
                    }

                    // Refresh local data after increments
                    val updatedLocalConversations = conversationDao.getAllConversationsSync()
                    val updatedLocalData = updatedLocalConversations.associateBy { it.id }

                    val conversations = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val conv = parseConversation(doc.id, doc.data ?: emptyMap())
                            val localEntity = updatedLocalData[conv.id]
                            
                            // Calculate unread count
                            val unreadCount = if (localEntity != null) {
                                localEntity.unreadCount
                            } else {
                                // New conversation - count actual unread messages from Firestore
                                val lastMessage = doc.data?.get("lastMessage") as? Map<*, *>
                                val senderId = lastMessage?.get("senderId") as? String
                                if (senderId != null && senderId != currentUserId) {
                                    countUnreadMessages(conv.id, currentUserId, 0)
                                } else {
                                    0
                                }
                            }
                            
                            timber.log.Timber.d("Conversation ${conv.id}: unreadCount=$unreadCount")
                            conv.copy(
                                unreadCount = unreadCount,
                                isPinned = localEntity?.isPinned ?: false,
                                isMuted = localEntity?.isMuted ?: false
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }?.sortedByDescending { it.updatedAt.toDate() } ?: emptyList()

                    // Create entities preserving lastReadTimestamp from local
                    val entities = conversations.map { conv ->
                        val localEntity = updatedLocalData[conv.id]
                        ConversationEntity.fromDomainModel(conv).copy(
                            lastReadTimestamp = localEntity?.lastReadTimestamp ?: 0L
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
            val currentTimestamp = System.currentTimeMillis()
            conversationDao.markAsRead(conversationId, currentTimestamp)
            timber.log.Timber.d("Marked conversation $conversationId as read at $currentTimestamp")
            Result.Success(Unit)
        } catch (e: Exception) {
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
