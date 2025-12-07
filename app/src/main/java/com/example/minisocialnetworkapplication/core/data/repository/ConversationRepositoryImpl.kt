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
                    // FIRST: Check for new messages and set/increment unread count
                    snapshot?.documentChanges?.forEach { change ->
                        val data = change.document.data
                        val lastMessage = data["lastMessage"] as? Map<*, *>
                        val senderId = lastMessage?.get("senderId") as? String
                        
                        when (change.type) {
                            com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                                // Conversation first loaded - check if has unread message
                                if (senderId != null && senderId != currentUserId) {
                                    // Check if this conversation already exists locally
                                    val existing = conversationDao.getConversationById(change.document.id)
                                    if (existing == null) {
                                        // New conversation with message from someone else
                                        timber.log.Timber.d("New conversation ${change.document.id} has unread message, setting unreadCount=1")
                                        // Will be set when we insert below
                                    }
                                }
                            }
                            com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                if (senderId != null && senderId != currentUserId) {
                                    // New message from someone else, increment unread
                                    timber.log.Timber.d("Incrementing unread for conversation ${change.document.id}")
                                    conversationDao.incrementUnreadCount(change.document.id)
                                }
                            }
                            else -> {}
                        }
                    }

                    // Collect IDs of new conversations with unread messages
                    val newConversationIds = mutableSetOf<String>()
                    snapshot?.documentChanges?.forEach { change ->
                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            val data = change.document.data
                            val lastMessage = data["lastMessage"] as? Map<*, *>
                            val senderId = lastMessage?.get("senderId") as? String
                            if (senderId != null && senderId != currentUserId) {
                                val existing = conversationDao.getConversationById(change.document.id)
                                if (existing == null) {
                                    timber.log.Timber.d("New conversation ${change.document.id} has unread message")
                                    newConversationIds.add(change.document.id)
                                }
                            }
                        }
                    }

                    // THEN: Get local unread counts (now includes the increment)
                    val localConversations = conversationDao.getAllConversationsSync()
                    val localUnreadCounts = localConversations.associate { it.id to it.unreadCount }
                    val localPinned = localConversations.associate { it.id to it.isPinned }
                    val localMuted = localConversations.associate { it.id to it.isMuted }

                    val conversations = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val conv = parseConversation(doc.id, doc.data ?: emptyMap())
                            // For new conversations with unread, set unreadCount=1
                            // For existing ones, preserve local unread count
                            val unreadCount = when {
                                newConversationIds.contains(conv.id) -> 1
                                else -> localUnreadCounts[conv.id] ?: 0
                            }
                            timber.log.Timber.d("Conversation ${conv.id}: unreadCount=$unreadCount")
                            conv.copy(
                                unreadCount = unreadCount,
                                isPinned = localPinned[conv.id] ?: false,
                                isMuted = localMuted[conv.id] ?: false
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }?.sortedByDescending { it.updatedAt.toDate() } ?: emptyList()

                    val entities = conversations.map { ConversationEntity.fromDomainModel(it) }
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
            conversationDao.resetUnreadCount(conversationId)
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
