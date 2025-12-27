package com.example.minisocialnetworkapplication.core.data.repository

import android.net.Uri
import com.example.minisocialnetworkapplication.core.domain.model.Group
import com.example.minisocialnetworkapplication.core.domain.model.GroupMember
import com.example.minisocialnetworkapplication.core.domain.model.GroupPrivacy
import com.example.minisocialnetworkapplication.core.domain.model.GroupRole
import com.example.minisocialnetworkapplication.core.domain.repository.GroupRepository
import com.example.minisocialnetworkapplication.core.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class GroupRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
    private val database: com.example.minisocialnetworkapplication.core.data.local.AppDatabase
) : GroupRepository {

    override suspend fun createGroup(name: String, description: String, privacy: GroupPrivacy, avatarUri: Uri?): Result<String> {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return Result.Error(Exception("User not logged in"))
        }

        return try {
            val groupId = firestore.collection("groups").document().id
            
            var avatarUrl: String? = null
            if (avatarUri != null) {
                try {
                    val avatarRef = storage.reference.child("group_avatars/$groupId-avatar.jpg")
                    avatarRef.putFile(avatarUri).await()
                    avatarUrl = avatarRef.downloadUrl.await().toString()
                    Timber.d("Group avatar uploaded: $avatarUrl")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to upload group avatar, continuing without avatar")
                }
            }
            
            val group = Group(
                id = groupId,
                name = name,
                description = description,
                avatarUrl = avatarUrl,
                ownerId = currentUser.uid,
                privacy = privacy,
                requirePostApproval = privacy == GroupPrivacy.PRIVATE,
                memberCount = 1,
                createdAt = com.google.firebase.Timestamp.now()
            )

            val member = GroupMember(
                userId = currentUser.uid,
                groupId = groupId,
                role = GroupRole.CREATOR,
                joinedAt = com.google.firebase.Timestamp.now()
            )

            firestore.runBatch { batch ->
                val groupRef = firestore.collection("groups").document(groupId)
                val groupData = hashMapOf(
                    "id" to group.id,
                    "name" to group.name,
                    "description" to group.description,
                    "avatarUrl" to group.avatarUrl,
                    "coverUrl" to group.coverUrl,
                    "ownerId" to group.ownerId,
                    "privacy" to group.privacy.name,
                    "postingPermission" to group.postingPermission.name,
                    "requirePostApproval" to group.requirePostApproval,
                    "memberCount" to group.memberCount,
                    "createdAt" to group.createdAt,
                    "status" to Group.STATUS_ACTIVE
                )
                batch.set(groupRef, groupData)

                val memberRef = firestore.collection("groups").document(groupId)
                    .collection("members").document(currentUser.uid)
                val memberData = hashMapOf(
                    "userId" to member.userId,
                    "groupId" to member.groupId,
                    "role" to member.role.name,
                    "joinedAt" to member.joinedAt
                )
                batch.set(memberRef, memberData)
            }.await()

            Result.Success(groupId)
        } catch (e: Exception) {
            Timber.e(e, "Error creating group")
            Result.Error(e)
        }
    }

    override fun getGroupsForUser(userId: String): Flow<List<Group>> = callbackFlow {
        // Early return if user is not signed in (prevents PERMISSION_DENIED during logout)
        if (auth.currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        // Track active group listeners for cleanup
        val groupListeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
        var currentGroupIds = emptyList<String>()
        
        val listener = firestore.collectionGroup("members")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                // Check if still signed in
                if (auth.currentUser == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (error != null) {
                    Timber.e(error, "Error listening to user groups")
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        trySend(emptyList())
                    } else {
                        close(error)
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val groupIds = snapshot.documents.map { it.reference.parent.parent?.id }.filterNotNull()
                    
                    // Only re-setup listeners if group membership changed
                    if (groupIds.sorted() != currentGroupIds.sorted()) {
                        currentGroupIds = groupIds
                        
                        // Remove old group listeners
                        groupListeners.forEach { it.remove() }
                        groupListeners.clear()
                        
                        if (groupIds.isEmpty()) {
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                        
                        // Create snapshot listener for groups
                        val allGroupsMap = java.util.concurrent.ConcurrentHashMap<String, Group>()
                        
                        groupIds.chunked(10).forEach { chunk ->
                            val groupListener = firestore.collection("groups")
                                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                                .addSnapshotListener { groupSnapshot, groupError ->
                                    if (groupError != null) {
                                        Timber.e(groupError, "Error listening to groups")
                                        return@addSnapshotListener
                                    }
                                    
                                    // Update groups from this chunk
                                    groupSnapshot?.documents?.forEach { doc ->
                                        val group = doc.toObject(Group::class.java)
                                        if (group != null) {
                                            allGroupsMap[doc.id] = group
                                        }
                                    }
                                    
                                    // Remove deleted groups
                                    groupSnapshot?.documentChanges?.forEach { change ->
                                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                            allGroupsMap.remove(change.document.id)
                                        }
                                    }
                                    
                                    // Send updated groups list
                                    val groups = allGroupsMap.values.toList()
                                    if (!isClosedForSend) {
                                        trySend(groups)
                                    }
                                }
                            groupListeners.add(groupListener)
                        }
                    }
                }
            }
        
        awaitClose { 
            listener.remove()
            groupListeners.forEach { it.remove() }
        }
    }

    override fun getAllGroups(): Flow<List<Group>> = callbackFlow {
        val listener = firestore.collection("groups")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error listening to all groups")
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        trySend(emptyList())
                    } else {
                        close(error)
                    }
                    return@addSnapshotListener
                }
                val groups = snapshot?.toObjects(Group::class.java) ?: emptyList()
                // Filter banned groups in memory to avoid needing a composite index for (status, createdAt)
                val activeGroups = groups.filter { it.status != Group.STATUS_BANNED }
                trySend(activeGroups)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getGroupDetails(groupId: String): Result<Group> {
        return try {
            val snapshot = firestore.collection("groups").document(groupId).get().await()
            val group = snapshot.toObject(Group::class.java)
            if (group != null) {
                // Remove hardcoded admin check, rely on Firestore rules for true security.
                // Repository just returns whatever it can read.
                
                // Sync member count if it's wrong (self-healing)
                val actualMembersCount = firestore.collection("groups").document(groupId)
                    .collection("members")
                    .count()
                    .get(com.google.firebase.firestore.AggregateSource.SERVER)
                    .await()
                    .count

                if (group.memberCount.toLong() != actualMembersCount) {
                    Timber.d("Mismatch in memberCount for group $groupId: stored=${group.memberCount}, actual=$actualMembersCount. Syncing...")
                    firestore.collection("groups").document(groupId)
                        .update("memberCount", actualMembersCount)
                        .await()
                    return Result.Success(group.copy(memberCount = actualMembersCount))
                }

                Result.Success(group)
            } else {
                Result.Error(Exception("Group not found"))
            }
        } catch (e: Exception) {
             Result.Error(e)
        }
    }

    override suspend fun joinGroup(groupId: String): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.Error(Exception("User not logged in"))
        return try {
            val groupDoc = firestore.collection("groups").document(groupId).get().await()
            val privacy = groupDoc.getString("privacy") ?: "PUBLIC"
            
            if (privacy == "PRIVATE") {
                val requestResult = createJoinRequest(groupId)
                return when (requestResult) {
                    is Result.Success -> Result.Success(Unit)
                    is Result.Error -> Result.Error(requestResult.exception)
                    else -> Result.Error(Exception("Unknown error"))
                }
            }
            
            val member = GroupMember(
                userId = currentUser.uid,
                groupId = groupId,
                role = GroupRole.MEMBER,
                joinedAt = com.google.firebase.Timestamp.now()
            )
            firestore.collection("groups").document(groupId)
                .collection("members").document(currentUser.uid)
                .set(member)
                .await()
            
             firestore.collection("groups").document(groupId)
                .update("memberCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun leaveGroup(groupId: String): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.Error(Exception("User not logged in"))
        return try {
            firestore.collection("groups").document(groupId)
                .collection("members").document(currentUser.uid)
                .delete()
                .await()
                
             firestore.collection("groups").document(groupId)
                .update("memberCount", com.google.firebase.firestore.FieldValue.increment(-1))
                
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun isMember(groupId: String, userId: String): Result<Boolean> {
        return try {
            val doc = firestore.collection("groups").document(groupId)
                .collection("members").document(userId)
                .get().await()
            Result.Success(doc.exists())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun getMemberRole(groupId: String, userId: String): Result<GroupRole?> {
        return try {
            val doc = firestore.collection("groups").document(groupId)
                .collection("members").document(userId)
                .get().await()
            
            if (!doc.exists()) {
                return Result.Success(null)
            }
            
            val roleString = doc.getString("role")
            val role = roleString?.let {
                try {
                    GroupRole.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            
            Result.Success(role)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getGroupMembers(groupId: String): Result<List<GroupMember>> {
        return try {
            val snapshot = firestore.collection("groups").document(groupId)
                .collection("members")
                .get().await()
            
            val members = snapshot.documents.mapNotNull { doc ->
                try {
                    GroupMember(
                        userId = doc.id,
                        groupId = groupId,
                        role = try {
                            GroupRole.valueOf(doc.getString("role") ?: "MEMBER")
                        } catch (e: Exception) {
                            GroupRole.MEMBER
                        },
                        joinedAt = (doc.getTimestamp("joinedAt") ?: com.google.firebase.Timestamp(java.util.Date(doc.getLong("joinedAt") ?: 0L))) 
                            ?: com.google.firebase.Timestamp.now()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            Result.Success(members)
        } catch (e: Exception) {
            Timber.e(e, "Error fetching group members")
            Result.Error(e)
        }
    }

    override fun getGroupPosts(groupId: String): Flow<List<com.example.minisocialnetworkapplication.core.domain.model.Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .whereEqualTo("groupId", groupId)
            .whereEqualTo("approvalStatus", "APPROVED")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                 if (error != null) {
                    Timber.e(error, "Error listening to group posts")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(com.example.minisocialnetworkapplication.core.domain.model.Post::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                // Extra safety: Filter out posts if group is banned (should be blocked by rules too)
                launch {
                    try {
                        val groupDoc = firestore.collection("groups").document(groupId).get().await()
                        val status = groupDoc.getString("status")
                        if (status == Group.STATUS_BANNED) {
                            trySend(emptyList())
                        } else {
                            trySend(posts)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error checking group status in getGroupPosts")
                        trySend(emptyList())
                    }
                }
            }
        awaitClose { listener.remove() }
    }
    
    override fun getGroupsWhereUserIsAdmin(userId: String): Flow<List<Group>> = callbackFlow {
        // Early return if user is not signed in
        if (auth.currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        // Track active group listeners for cleanup
        val groupListeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
        var currentGroupIds = emptyList<String>()
        
        try {
            // Query for both ADMIN and CREATOR roles
            val memberListener = firestore.collectionGroup("members")
                .whereEqualTo("userId", userId)
                .whereIn("role", listOf("ADMIN", "CREATOR"))
                .addSnapshotListener { memberSnapshot, error ->
                    if (auth.currentUser == null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (error != null) {
                        Timber.e(error, "Error listening to user admin groups")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val groupIds = memberSnapshot?.documents?.mapNotNull { it.getString("groupId") } ?: emptyList()
                    
                    // Only re-setup listeners if admin group membership changed
                    if (groupIds.sorted() != currentGroupIds.sorted()) {
                        currentGroupIds = groupIds
                        
                        // Remove old group listeners
                        groupListeners.forEach { it.remove() }
                        groupListeners.clear()
                        
                        if (groupIds.isEmpty()) {
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                        
                        // Create snapshot listener for admin groups
                        val allGroupsMap = java.util.concurrent.ConcurrentHashMap<String, Group>()
                        
                        groupIds.chunked(10).forEach { chunk ->
                            val groupListener = firestore.collection("groups")
                                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                                .addSnapshotListener { groupSnapshot, groupError ->
                                    if (groupError != null) {
                                        Timber.e(groupError, "Error listening to admin groups")
                                        return@addSnapshotListener
                                    }
                                    
                                    // Update groups from this chunk
                                    groupSnapshot?.documents?.forEach { doc ->
                                        val group = doc.toObject(Group::class.java)
                                        if (group != null) {
                                            allGroupsMap[doc.id] = group
                                        }
                                    }
                                    
                                    // Remove deleted groups
                                    groupSnapshot?.documentChanges?.forEach { change ->
                                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                            allGroupsMap.remove(change.document.id)
                                        }
                                    }
                                    
                                    // Send updated groups list
                                    val groups = allGroupsMap.values.toList()
                                    if (!isClosedForSend) {
                                        trySend(groups)
                                    }
                                }
                            groupListeners.add(groupListener)
                        }
                    }
                }
            
            awaitClose { 
                memberListener.remove()
                groupListeners.forEach { it.remove() }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in getGroupsWhereUserIsAdmin")
            trySend(emptyList())
            close(e)
        }
    }
    
    override fun getAllPostsFromUserGroups(userId: String): Flow<List<com.example.minisocialnetworkapplication.core.domain.model.Post>> = callbackFlow {
        // Early return if user is not signed in
        if (auth.currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        // Track active post listeners for cleanup
        val postListeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
        var currentGroupIds = emptyList<String>()
        
        try {
            val memberListener = firestore.collectionGroup("members")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { memberSnapshot, error ->
                    if (auth.currentUser == null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (error != null) {
                        Timber.e(error, "Error listening to user groups for posts")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val groupIds = memberSnapshot?.documents?.mapNotNull { it.getString("groupId") } ?: emptyList()
                    
                    // Only re-setup listeners if group membership changed
                    if (groupIds.sorted() != currentGroupIds.sorted()) {
                        currentGroupIds = groupIds
                        
                        // Remove old post listeners
                        postListeners.forEach { it.remove() }
                        postListeners.clear()
                        
                        if (groupIds.isEmpty()) {
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                        
                        // Create snapshot listener for posts from these groups
                        // Use chunked approach for whereIn limit of 10
                        val allPostsMap = java.util.concurrent.ConcurrentHashMap<String, com.example.minisocialnetworkapplication.core.domain.model.Post>()
                        
                        groupIds.chunked(10).forEach { chunk ->
                            val postListener = firestore.collection("posts")
                                .whereIn("groupId", chunk)
                                .whereEqualTo("approvalStatus", "APPROVED")
                                .orderBy("createdAt", Query.Direction.DESCENDING)
                                .addSnapshotListener { postsSnapshot, postError ->
                                    if (postError != null) {
                                        Timber.e(postError, "Error listening to group posts")
                                        return@addSnapshotListener
                                    }
                                    
                                    // Update posts from this chunk
                                    postsSnapshot?.documents?.forEach { doc ->
                                        val post = doc.toObject(com.example.minisocialnetworkapplication.core.domain.model.Post::class.java)?.copy(id = doc.id)
                                        if (post != null) {
                                            allPostsMap[doc.id] = post
                                        }
                                    }
                                    
                                    // Remove deleted posts
                                    postsSnapshot?.documentChanges?.forEach { change ->
                                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                            allPostsMap.remove(change.document.id)
                                        }
                                    }
                                    
                                    // Send updated posts list
                                    val sortedPosts = allPostsMap.values.toList()
                                        .sortedByDescending { it.createdAt }
                                    if (!isClosedForSend) {
                                        trySend(sortedPosts)
                                    }
                                }
                            postListeners.add(postListener)
                        }
                    }
                }
            
            awaitClose { 
                memberListener.remove()
                postListeners.forEach { it.remove() }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in getAllPostsFromUserGroups")
            trySend(emptyList())
            close(e)
        }
    }
    
    override suspend fun sendInvitations(groupId: String, userIds: List<String>): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.Error(Exception("User not logged in"))
        return try {
            val groupDoc = firestore.collection("groups").document(groupId).get().await()
            val group = groupDoc.toObject(Group::class.java) 
                ?: return Result.Error(Exception("Group not found"))
            
            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
            val userName = userDoc.getString("name") ?: "Someone"
            
            val memberDoc = firestore.collection("groups").document(groupId)
                .collection("members").document(currentUser.uid).get().await()
            val inviterRole = try {
                GroupRole.valueOf(memberDoc.getString("role") ?: "MEMBER")
            } catch (e: Exception) {
                GroupRole.MEMBER
            }
            
            val batch = firestore.batch()
            userIds.forEach { userId ->
                val invitationRef = firestore.collection("group_invitations").document()
                val invitation = com.example.minisocialnetworkapplication.core.domain.model.GroupInvitation(
                    id = invitationRef.id,
                    groupId = groupId,
                    groupName = group.name,
                    groupAvatarUrl = group.avatarUrl,
                    inviterId = currentUser.uid,
                    inviterName = userName,
                    inviterRole = inviterRole,
                    inviteeId = userId,
                    status = com.example.minisocialnetworkapplication.core.domain.model.InvitationStatus.PENDING
                )
                batch.set(invitationRef, invitation)
                
                val notificationRef = firestore.collection("notifications").document()
                val notification = com.example.minisocialnetworkapplication.core.domain.model.Notification(
                    id = notificationRef.id,
                    userId = userId,
                    type = com.example.minisocialnetworkapplication.core.domain.model.NotificationType.GROUP_INVITATION,
                    title = "Group Invitation",
                    message = "$userName invited you to join ${group.name}",
                    data = mapOf("groupId" to groupId, "invitationId" to invitationRef.id)
                )
                batch.set(notificationRef, notification)
            }
            batch.commit().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error sending invitations")
            Result.Error(e)
        }
    }
    
    override suspend fun respondToInvitation(invitationId: String, accept: Boolean): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.Error(Exception("User not logged in"))
        return try {
            val invitationDoc = firestore.collection("group_invitations").document(invitationId).get().await()
            val invitation = invitationDoc.toObject(com.example.minisocialnetworkapplication.core.domain.model.GroupInvitation::class.java)
                ?: return Result.Error(Exception("Invitation not found"))
            
            if (invitation.inviteeId != currentUser.uid) {
                return Result.Error(Exception("Not authorized"))
            }
            
            firestore.collection("group_invitations").document(invitationId)
                .update("status", if (accept) com.example.minisocialnetworkapplication.core.domain.model.InvitationStatus.ACCEPTED 
                                   else com.example.minisocialnetworkapplication.core.domain.model.InvitationStatus.DECLINED)
                .await()
            
            if (accept) {
                val groupDoc = firestore.collection("groups").document(invitation.groupId).get().await()
                val privacy = groupDoc.getString("privacy") ?: "PUBLIC"
                
                val isPrivateGroup = privacy == "PRIVATE"
                val isAdminInvite = invitation.inviterRole == GroupRole.ADMIN || invitation.inviterRole == GroupRole.CREATOR
                
                if (isPrivateGroup && !isAdminInvite) {
                    createJoinRequest(
                        groupId = invitation.groupId,
                        inviterId = invitation.inviterId,
                        inviterName = invitation.inviterName,
                        inviterRole = invitation.inviterRole
                    )
                    Timber.d("Created join request for member invite to private group")
                } else {
                    val member = GroupMember(
                        userId = currentUser.uid,
                        groupId = invitation.groupId,
                        role = GroupRole.MEMBER,
                        joinedAt = com.google.firebase.Timestamp.now()
                    )
                    firestore.collection("groups").document(invitation.groupId)
                        .collection("members").document(currentUser.uid)
                        .set(member)
                        .await()
                    
                    firestore.collection("groups").document(invitation.groupId)
                        .update("memberCount", com.google.firebase.firestore.FieldValue.increment(1))
                        .await()
                }
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error responding to invitation")
            Result.Error(e)
        }
    }
    
    override fun getInvitationsForUser(userId: String): Flow<List<com.example.minisocialnetworkapplication.core.domain.model.GroupInvitation>> = callbackFlow {
        val listener = firestore.collection("group_invitations")
            .whereEqualTo("inviteeId", userId)
            .whereEqualTo("status", com.example.minisocialnetworkapplication.core.domain.model.InvitationStatus.PENDING.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error listening to invitations")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val invitations = snapshot?.toObjects(com.example.minisocialnetworkapplication.core.domain.model.GroupInvitation::class.java) ?: emptyList()
                trySend(invitations)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createJoinRequest(
        groupId: String, 
        inviterId: String?, 
        inviterName: String?, 
        inviterRole: GroupRole?
    ): Result<String> {
        val currentUser = auth.currentUser ?: return Result.Error(Exception("User not logged in"))
        return try {
            val groupDoc = firestore.collection("groups").document(groupId).get().await()
            val groupName = groupDoc.getString("name") ?: ""
            
            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
            val userName = userDoc.getString("displayName") ?: currentUser.displayName ?: ""
            val userAvatarUrl = userDoc.getString("avatarUrl")
            
            val existingRequest = firestore.collection("join_requests")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("status", com.example.minisocialnetworkapplication.core.domain.model.JoinRequestStatus.PENDING.name)
                .get()
                .await()
            
            if (!existingRequest.isEmpty) {
                return Result.Error(Exception("Join request already pending"))
            }
            
            val requestRef = firestore.collection("join_requests").document()
            val joinRequest = com.example.minisocialnetworkapplication.core.domain.model.JoinRequest(
                id = requestRef.id,
                groupId = groupId,
                groupName = groupName,
                userId = currentUser.uid,
                userName = userName,
                userAvatarUrl = userAvatarUrl,
                inviterId = inviterId,
                inviterName = inviterName,
                inviterRole = inviterRole,
                status = com.example.minisocialnetworkapplication.core.domain.model.JoinRequestStatus.PENDING,
                createdAt = com.google.firebase.Timestamp.now()
            )
            
            requestRef.set(joinRequest).await()
            
            Timber.d("Join request created: ${requestRef.id} for group $groupId")
            Result.Success(requestRef.id)
        } catch (e: Exception) {
            Timber.e(e, "Error creating join request")
            Result.Error(e)
        }
    }
    
    override fun getJoinRequestsForGroup(groupId: String): Flow<List<com.example.minisocialnetworkapplication.core.domain.model.JoinRequest>> = callbackFlow {
        val listener = firestore.collection("join_requests")
            .whereEqualTo("groupId", groupId)
            .whereEqualTo("status", com.example.minisocialnetworkapplication.core.domain.model.JoinRequestStatus.PENDING.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error listening to join requests")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val request = doc.toObject(com.example.minisocialnetworkapplication.core.domain.model.JoinRequest::class.java)
                        request?.copy(
                            id = doc.id,
                            createdAt = (doc.getTimestamp("createdAt") ?: com.google.firebase.Timestamp(java.util.Date(doc.getLong("createdAt") ?: 0L))) 
                                ?: com.google.firebase.Timestamp.now()
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }
    
    override suspend fun approveJoinRequest(requestId: String): Result<Unit> {
        return try {
            val requestDoc = firestore.collection("join_requests").document(requestId).get().await()
            val request = requestDoc.toObject(com.example.minisocialnetworkapplication.core.domain.model.JoinRequest::class.java)
                ?: return Result.Error(Exception("Join request not found"))
            
            firestore.collection("join_requests").document(requestId)
                .update("status", com.example.minisocialnetworkapplication.core.domain.model.JoinRequestStatus.APPROVED.name)
                .await()
            
            val member = GroupMember(
                userId = request.userId,
                groupId = request.groupId,
                role = GroupRole.MEMBER,
                joinedAt = com.google.firebase.Timestamp.now()
            )
            firestore.collection("groups").document(request.groupId)
                .collection("members").document(request.userId)
                .set(member)
                .await()
            
            firestore.collection("groups").document(request.groupId)
                .update("memberCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            
            Timber.d("Join request approved: $requestId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error approving join request")
            Result.Error(e)
        }
    }
    
    override suspend fun rejectJoinRequest(requestId: String): Result<Unit> {
        return try {
            firestore.collection("join_requests").document(requestId)
                .update("status", com.example.minisocialnetworkapplication.core.domain.model.JoinRequestStatus.REJECTED.name)
                .await()
            
            Timber.d("Join request rejected: $requestId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error rejecting join request")
            Result.Error(e)
        }
    }
    
    // ============================================
    // Member Management (Admin Actions)
    // ============================================
    
    override suspend fun makeAdmin(groupId: String, userId: String): Result<Unit> {
        return try {
            firestore.collection("groups").document(groupId)
                .collection("members").document(userId)
                .update("role", GroupRole.ADMIN.name)
                .await()
            
            Timber.d("Made user $userId admin in group $groupId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error making user admin")
            Result.Error(e)
        }
    }
    
    override suspend fun removeMember(groupId: String, userId: String): Result<Unit> {
        return try {
            firestore.collection("groups").document(groupId)
                .collection("members").document(userId)
                .delete()
                .await()
            
            // Decrement member count
            firestore.collection("groups").document(groupId)
                .update("memberCount", com.google.firebase.firestore.FieldValue.increment(-1))
                .await()
            
            Timber.d("Removed user $userId from group $groupId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error removing member")
            Result.Error(e)
        }
    }
    
    override suspend fun dismissAdmin(groupId: String, userId: String): Result<Unit> {
        return try {
            firestore.collection("groups").document(groupId)
                .collection("members").document(userId)
                .update("role", GroupRole.MEMBER.name)
                .await()
            
            Timber.d("Dismissed admin $userId from group $groupId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error dismissing admin")
            Result.Error(e)
        }
    }
    
    // ================================
    // Post Approval Functions
    // ================================
    
    override suspend fun togglePostApproval(groupId: String, enabled: Boolean): Result<Unit> {
        return try {
            firestore.collection("groups").document(groupId)
                .update("requirePostApproval", enabled)
                .await()
            
            Timber.d("Set post approval for group $groupId to $enabled")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error toggling post approval")
            Result.Error(e)
        }
    }
    
    override fun getPendingPosts(groupId: String): Flow<List<com.example.minisocialnetworkapplication.core.domain.model.Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .whereEqualTo("groupId", groupId)
            .whereEqualTo("approvalStatus", "PENDING")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error listening to pending posts")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(com.example.minisocialnetworkapplication.core.domain.model.Post::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }
    
    override suspend fun approvePost(postId: String): Result<Unit> {
        return try {
            firestore.collection("posts").document(postId)
                .update("approvalStatus", "APPROVED")
                .await()
            
            Timber.d("Approved post $postId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error approving post")
            Result.Error(e)
        }
    }
    
    override suspend fun rejectPost(postId: String, reason: String?): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "approvalStatus" to "REJECTED"
            )
            if (reason != null) {
                updates["rejectionReason"] = reason
            }
            
            firestore.collection("posts").document(postId)
                .update(updates)
                .await()
            
            Timber.d("Rejected post $postId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error rejecting post")
            Result.Error(e)
        }
    }
    
    // Report Moderation
    override suspend fun getHiddenPostsForGroup(groupId: String): Result<List<com.example.minisocialnetworkapplication.core.domain.model.Post>> {
        return try {
            val snapshot = firestore.collection("posts")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("approvalStatus", "HIDDEN")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val posts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(com.example.minisocialnetworkapplication.core.domain.model.Post::class.java)?.copy(id = doc.id)
            }
            
            Timber.d("Fetched ${posts.size} hidden posts for group $groupId")
            Result.Success(posts)
        } catch (e: Exception) {
            Timber.e(e, "Error getting hidden posts")
            Result.Error(e)
        }
    }
    
    override suspend fun updatePostApprovalStatus(postId: String, status: String): Result<Unit> {
        return try {
            firestore.collection("posts").document(postId)
                .update("approvalStatus", status)
                .await()
            
            Timber.d("Updated post $postId approval status to $status")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating post approval status")
            Result.Error(e)
        }
    }

    override suspend fun hidePost(postId: String): Result<Unit> {
        return try {
            firestore.collection("posts").document(postId)
                .update("approvalStatus", "HIDDEN")
                .await()
            
            // Update Room cache to immediately hide from Feed
            try {
                database.postDao().updateApprovalStatus(postId, "HIDDEN")
                Timber.d("Updated Room cache for hidden post $postId")
            } catch (e: Exception) {
                Timber.w(e, "Failed to update Room cache for hidden post, will sync later")
            }
            
            Timber.d("Hidden post $postId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error hiding post")
            Result.Error(e)
        }
    }

    override suspend fun restorePost(postId: String): Result<Unit> {
        return try {
            firestore.collection("posts").document(postId)
                .update("approvalStatus", "APPROVED")
                .await()
            
            // Update Room cache to immediately show in Feed
            try {
                database.postDao().updateApprovalStatus(postId, "APPROVED")
                Timber.d("Updated Room cache for restored post $postId")
            } catch (e: Exception) {
                Timber.w(e, "Failed to update Room cache for restored post, will sync later")
            }
            
            Timber.d("Restored post $postId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error restoring post")
            Result.Error(e)
        }
    }
    
    override suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            // Delete the post document
            firestore.collection("posts").document(postId)
                .delete()
                .await()
            
            Timber.d("Deleted post $postId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting post")
            Result.Error(e)
        }
    }

    // ================================
    // Group Settings Functions
    // ================================

    override suspend fun updateGroup(
        groupId: String,
        name: String,
        description: String,
        privacy: com.example.minisocialnetworkapplication.core.domain.model.GroupPrivacy,
        avatarUri: Uri?
    ): Result<Unit> {
        return try {
            var avatarUrl: String? = null
            if (avatarUri != null) {
                val avatarRef = storage.reference.child("group_avatars/$groupId-avatar.jpg")
                avatarRef.putFile(avatarUri).await()
                avatarUrl = avatarRef.downloadUrl.await().toString()
            }

            val updates = mutableMapOf<String, Any>(
                "name" to name,
                "description" to description,
                "privacy" to privacy.name
            )
            if (avatarUrl != null) {
                updates["avatarUrl"] = avatarUrl
            }

            firestore.collection("groups").document(groupId)
                .update(updates)
                .await()

            Timber.d("Group updated: $groupId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating group")
            Result.Error(e)
        }
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> {
        return try {
            firestore.collection("groups").document(groupId).delete().await()
            Timber.d("Group document $groupId deleted. Cloud Function will handle cleanup.")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting group document")
            Result.Error(e)
        }
    }
}
