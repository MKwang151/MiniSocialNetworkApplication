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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.google.firebase.firestore.FieldPath

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val conversationDao: ConversationDao,
    private val participantDao: ParticipantDao,
    private val storage: com.google.firebase.storage.FirebaseStorage
) : ConversationRepository {

    companion object {
        private const val COLLECTION_CONVERSATIONS = "conversations"
        private const val COLLECTION_MESSAGES = "messages"
        private const val COLLECTION_PARTICIPANTS = "participants"
        private const val COLLECTION_JOIN_REQUESTS = "join_requests"
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

    // In-memory cache for lastReadSequenceId per conversation
    private val lastReadSeqIdCache = mutableMapOf<String, Long>()
    
    // In-memory cache for deletedAt timestamp per conversation (for soft delete)
    private val deletedAtCache = mutableMapOf<String, com.google.firebase.Timestamp?>()
    
    // In-memory cache for isPinned per conversation (per-user)
    private val isPinnedCache = mutableMapOf<String, Boolean>()
    
    /**
     * Get user's deletedAt timestamp for a conversation
     */
    private suspend fun getDeletedAtForUser(conversationId: String, userId: String): com.google.firebase.Timestamp? {
        return try {
            val doc = firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(COLLECTION_PARTICIPANTS)
                .document(userId)
                .get()
                .await()
            doc.getTimestamp("deletedAt")
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get user's isPinned status for a conversation
     */
    private suspend fun getIsPinnedForUser(conversationId: String, userId: String): Boolean {
        return try {
            val doc = firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(COLLECTION_PARTICIPANTS)
                .document(userId)
                .get()
                .await()
            doc.getBoolean("isPinned") ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    override fun getConversations(): Flow<List<Conversation>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // Cached conversations list for re-calculating when participants change
        var cachedConversationDocs: List<com.google.firebase.firestore.DocumentSnapshot> = emptyList()
        
        // Function to process and emit conversations with current cache
        fun processAndEmit() {
            kotlinx.coroutines.runBlocking {
                val conversations = cachedConversationDocs.mapNotNull { doc ->
                    try {
                        val conv = parseConversation(doc.id, doc.data ?: emptyMap())
                        
                        // Get lastMessage timestamp from Firestore
                        val lastMessage = doc.data?.get("lastMessage") as? Map<*, *>
                        val lastMsgTimestamp = lastMessage?.get("timestamp") as? com.google.firebase.Timestamp
                        val lastMsgSequenceId = (lastMessage?.get("sequenceId") as? Long) ?: 0L
                        
                        // Check if user has soft-deleted this conversation
                        val deletedAt = deletedAtCache[conv.id] ?: getDeletedAtForUser(conv.id, currentUserId)
                        deletedAtCache[conv.id] = deletedAt
                        
                        // If deleted and no new message after deletion, skip
                        if (deletedAt != null) {
                            val hasNewMessage = lastMsgTimestamp != null && 
                                lastMsgTimestamp.toDate().after(deletedAt.toDate())
                            if (!hasNewMessage) {
                                return@mapNotNull null // Skip - no new messages since deletion
                            }
                            // Has new message - clear deletion when entering chat
                        }
                        
                        // Get isPinned from cache or fetch
                        val isPinned = isPinnedCache[conv.id] ?: getIsPinnedForUser(conv.id, currentUserId)
                        isPinnedCache[conv.id] = isPinned
                        
                        // Get cached lastReadSequenceId
                        val lastReadSeqId = lastReadSeqIdCache[conv.id] ?: 0L
                        
                        // Calculate unread count
                        val unreadCount = if (lastMsgSequenceId > lastReadSeqId) {
                            (lastMsgSequenceId - lastReadSeqId).toInt()
                        } else {
                            0
                        }
                        
                        conv.copy(unreadCount = unreadCount, isPinned = isPinned)
                    } catch (e: Exception) {
                        timber.log.Timber.e(e, "Error parsing conversation ${doc.id}")
                        null
                    }
                }
                // Sort: pinned first, then by updatedAt descending
                .sortedWith(compareByDescending<Conversation> { it.isPinned }
                    .thenByDescending { it.updatedAt.toDate() }
                )
                
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
                if (entities.isNotEmpty()) {
                    conversationDao.insertAll(entities)
                }
                
                trySend(conversations)
            }
        }
        
        
        // Listeners for each conversation's participants subcollection (current user's doc)
        val participantListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()
        
        // Function to set up participant listeners for current conversations
        fun setupParticipantListeners(convIds: List<String>) {
            // Remove listeners for conversations no longer in list
            val currentIds = convIds.toSet()
            participantListeners.keys.filter { it !in currentIds }.forEach { oldConvId ->
                participantListeners[oldConvId]?.remove()
                participantListeners.remove(oldConvId)
            }
            
            // Add listeners for new conversations
            convIds.filter { it !in participantListeners.keys }.forEach { convId ->
                val listener = firestore.collection(COLLECTION_CONVERSATIONS)
                    .document(convId)
                    .collection(COLLECTION_PARTICIPANTS)
                    .document(currentUserId)
                    .addSnapshotListener { doc, error ->
                        if (error != null || doc == null) return@addSnapshotListener
                        
                        val lastReadSeqId = doc.getLong("lastReadSequenceId") ?: 0L
                        val oldValue = lastReadSeqIdCache[convId]
                        if (oldValue != lastReadSeqId) {
                            timber.log.Timber.d("Participant listener: conv=$convId, lastReadSeqId updated $oldValue -> $lastReadSeqId")
                            lastReadSeqIdCache[convId] = lastReadSeqId
                            processAndEmit()
                        }
                    }
                participantListeners[convId] = listener
            }
        }
        
        // Modify the conversations listener to also set up participant listeners
        val conversationsListener = firestore.collection(COLLECTION_CONVERSATIONS)
            .whereArrayContains("participantIds", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    kotlinx.coroutines.runBlocking {
                        val localConversations = conversationDao.getAllConversationsSync()
                        trySend(localConversations.map { it.toDomainModel() })
                    }
                    return@addSnapshotListener
                }
                
                cachedConversationDocs = snapshot?.documents ?: emptyList()
                val convIds = cachedConversationDocs.map { it.id }
                
                // Initial fetch of lastReadSequenceId for new conversations only
                kotlinx.coroutines.runBlocking {
                    for (doc in cachedConversationDocs) {
                        val convId = doc.id
                        if (!lastReadSeqIdCache.containsKey(convId)) {
                            // Fetch initial value
                            val seqId = getLastReadSequenceId(convId, currentUserId)
                            lastReadSeqIdCache[convId] = seqId
                        }
                    }
                }
                
                // Set up real-time listeners for participant docs
                setupParticipantListeners(convIds)
                
                processAndEmit()
            }

        awaitClose { 
            conversationsListener.remove()
            participantListeners.values.forEach { it.remove() }
        }
    }


    override suspend fun getConversation(conversationId: String): Result<Conversation> {
        return try {
            // Always fetch from Firestore first to get fresh pinnedMessageIds
            val doc = firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .get()
                .await()

            if (!doc.exists()) {
                // Fallback to local cache if document not found
                val local = conversationDao.getConversationById(conversationId)
                if (local != null) {
                    return Result.Success(local.toDomainModel())
                }
                return Result.Error(Exception("Conversation not found"))
            }

            val conversation = parseConversation(doc.id, doc.data ?: emptyMap())
            conversationDao.insertConversation(ConversationEntity.fromDomainModel(conversation))
            Result.Success(conversation)
        } catch (e: Exception) {
            // On error, fallback to local cache
            val local = conversationDao.getConversationById(conversationId)
            if (local != null) {
                return Result.Success(local.toDomainModel())
            }
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
                "creatorId" to currentUserId,
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
                creatorId = currentUserId,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            conversationDao.insertConversation(ConversationEntity.fromDomainModel(conversation))
            Result.Success(conversation)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun uploadGroupAvatar(uri: android.net.Uri): Result<String> {
        return try {
            val filename = "group_avatars/${java.util.UUID.randomUUID()}"
            val ref = storage.reference.child(filename)
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.Success(downloadUrl)
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to upload group avatar")
            Result.Error(e)
        }
    }

    override suspend fun updateConversation(
        conversationId: String,
        isPinned: Boolean?,
        isMuted: Boolean?
    ): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid 
                ?: return Result.Error(Exception("Not authenticated"))
            
            timber.log.Timber.d("PIN_DEBUG: updateConversation called - convId=$conversationId, isPinned=$isPinned, isMuted=$isMuted")
            
            // Build update map for participant document
            val updates = mutableMapOf<String, Any>()
            isPinned?.let { updates["isPinned"] = it }
            isMuted?.let { updates["isMuted"] = it }
            
            if (updates.isNotEmpty()) {
                // Update participant's settings in Firestore
                firestore.collection(COLLECTION_CONVERSATIONS)
                    .document(conversationId)
                    .collection(COLLECTION_PARTICIPANTS)
                    .document(currentUserId)
                    .set(updates, com.google.firebase.firestore.SetOptions.merge())
                    .await()
                
                timber.log.Timber.d("PIN_DEBUG: Updated Firestore participants/$currentUserId with $updates")
                
                // Update in-memory cache immediately
                isPinned?.let { isPinnedCache[conversationId] = it }
            }
            
            // Also update local cache
            isPinned?.let { conversationDao.updatePinnedStatus(conversationId, it) }
            isMuted?.let { conversationDao.updateMutedStatus(conversationId, it) }
            
            timber.log.Timber.d("PIN_DEBUG: Updated local Room DB and isPinnedCache")
            Result.Success(Unit)
        } catch (e: Exception) {
            timber.log.Timber.e(e, "PIN_DEBUG: updateConversation error")
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
            
            // Get lastMessage.sequenceId directly from Firestore (avoid stale local cache)
            val conversationDoc = firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .get()
                .await()
            
            val lastMessageData = conversationDoc.get("lastMessage") as? Map<*, *>
            val lastMsgSeqId = (lastMessageData?.get("sequenceId") as? Long) ?: 0L
            
            timber.log.Timber.d("markConversationAsRead: got lastMsgSeqId=$lastMsgSeqId from Firestore")
            
            // Optimistic cache update for instant UI feedback
            lastReadSeqIdCache[conversationId] = lastMsgSeqId
            
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

    override suspend fun hideConversationForUser(conversationId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid 
                ?: return Result.Error(Exception("Not authenticated"))
            
            // Store deletedAt timestamp in participants subcollection
            // Conversation will reappear if new messages arrive after this time
            firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(COLLECTION_PARTICIPANTS)
                .document(currentUserId)
                .set(mapOf(
                    "deletedAt" to FieldValue.serverTimestamp()
                ), com.google.firebase.firestore.SetOptions.merge())
                .await()
            
            // Also delete from local cache
            conversationDao.deleteConversationById(conversationId)
            
            timber.log.Timber.d("Soft deleted conversation $conversationId for user $currentUserId")
            Result.Success(Unit)
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error hiding conversation")
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

        // Parse pinned message IDs
        val pinnedMessageIds = (data["pinnedMessageIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val adminIds = (data["adminIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        timber.log.Timber.d("parseConversation: id=$id, pinnedMessageIds=$pinnedMessageIds, adminIds=$adminIds")

        return Conversation(
            id = id,
            type = type,
            name = data["name"] as? String,
            avatarUrl = data["avatarUrl"] as? String,
            participantIds = participantIds,
            adminIds = adminIds,
            lastMessage = lastMessage,
            pinnedMessageIds = pinnedMessageIds,
            creatorId = data["creatorId"] as? String,
            createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
            updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now()
        )
    }
    override suspend fun addAdmin(conversationId: String, userId: String): Result<Unit> {
    // ... rest of implementation stays same
        return try {
            firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .update("adminIds", FieldValue.arrayUnion(userId))
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun removeAdmin(conversationId: String, userId: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .update("adminIds", FieldValue.arrayRemove(userId))
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun removeParticipant(conversationId: String, userId: String): Result<Unit> {
        return try {
            // Remove from both participantIds and adminIds (if present)
            val updates = mapOf(
                "participantIds" to FieldValue.arrayRemove(userId),
                "adminIds" to FieldValue.arrayRemove(userId)
            )
            
            firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .update(updates)
                .await()
                
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun addMembers(conversationId: String, memberIds: List<String>): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .update("participantIds", FieldValue.arrayUnion(*memberIds.toTypedArray()))
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun createJoinRequests(conversationId: String, memberIds: List<String>): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid 
                ?: return Result.Error(Exception("Not authenticated"))
                
            val batch = firestore.batch()
            val collectionRef = firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(COLLECTION_JOIN_REQUESTS)

            memberIds.forEach { userId ->
                val docRef = collectionRef.document(userId)
                val data = mapOf(
                    "requestedBy" to currentUserId,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, data)
            }
            
            batch.commit().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getJoinRequests(conversationId: String): Flow<List<com.example.minisocialnetworkapplication.core.domain.model.User>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_CONVERSATIONS)
            .document(conversationId)
            .collection(COLLECTION_JOIN_REQUESTS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    timber.log.Timber.e(error, "Error listening for join requests")
                    return@addSnapshotListener
                }
                
                val userIds = snapshot?.documents?.map { it.id } ?: emptyList()
                if (userIds.isEmpty()) {
                    trySend(emptyList())
                } else {
                    // Fetch users
                    // Note: Ideally we should use a cached repository or user flow, but for now simple fetch
                    launch(kotlinx.coroutines.Dispatchers.IO) {
                         try {
                             val chunks = userIds.chunked(10)
                             val users = mutableListOf<com.example.minisocialnetworkapplication.core.domain.model.User>()
                             
                             for (chunk in chunks) {
                                 val userSnapshots = firestore.collection("users")
                                     .whereIn(FieldPath.documentId(), chunk)
                                     .get()
                                     .await()
                                 users.addAll(userSnapshots.toObjects(com.example.minisocialnetworkapplication.core.domain.model.User::class.java))
                             }
                             trySend(users)
                         } catch (e: Exception) {
                             timber.log.Timber.e(e, "Error fetching join request users")
                         }
                    }
                }
            }
            
        awaitClose { listener.remove() }
    }

    override suspend fun acceptJoinRequest(conversationId: String, userId: String): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val convRef = firestore.collection(COLLECTION_CONVERSATIONS).document(conversationId)
            val requestRef = convRef.collection(COLLECTION_JOIN_REQUESTS).document(userId)
            
            batch.update(convRef, "participantIds", FieldValue.arrayUnion(userId))
            batch.delete(requestRef)
            
            batch.commit().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun declineJoinRequest(conversationId: String, userId: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(COLLECTION_JOIN_REQUESTS)
                .document(userId)
                .delete()
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    override suspend fun leaveConversation(conversationId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid 
                ?: return Result.Error(Exception("Not authenticated"))
                
            val updates = mapOf(
                "participantIds" to FieldValue.arrayRemove(currentUserId),
                "adminIds" to FieldValue.arrayRemove(currentUserId)
            )
            
            firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .update(updates)
                .await()
                
            conversationDao.deleteConversationById(conversationId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteConversationPermanent(conversationId: String): Result<Unit> {
        return try {
            // Note: This only deletes the main document. Subcollections remain but are inaccessible via normal queries.
            // Ideally use a Cloud Function for recursive delete or recursive delete from client.
            firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .delete()
                .await()
                
            conversationDao.deleteConversationById(conversationId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
