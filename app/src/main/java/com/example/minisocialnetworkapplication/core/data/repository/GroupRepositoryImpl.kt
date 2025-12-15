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
    private val storage: FirebaseStorage
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
                memberCount = 1,
                createdAt = System.currentTimeMillis()
            )

            val member = GroupMember(
                userId = currentUser.uid,
                groupId = groupId,
                role = GroupRole.CREATOR,
                joinedAt = System.currentTimeMillis()
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
                    "createdAt" to group.createdAt
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
        val listener = firestore.collectionGroup("members")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
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
                    if (groupIds.isEmpty()) {
                        trySend(emptyList())
                    } else {
                         firestore.collection("groups")
                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), groupIds.take(10)) 
                            .get()
                            .addOnSuccessListener { groupSnapshot ->
                                val groups = groupSnapshot.toObjects(Group::class.java)
                                trySend(groups)
                            }
                            .addOnFailureListener { e ->
                                Timber.e(e, "Error fetching groups details")
                                trySend(emptyList())
                            }
                    }
                }
            }
        
        awaitClose { listener.remove() }
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
                trySend(groups)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getGroupDetails(groupId: String): Result<Group> {
        return try {
            val snapshot = firestore.collection("groups").document(groupId).get().await()
            val group = snapshot.toObject(Group::class.java)
            if (group != null) {
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
                joinedAt = System.currentTimeMillis()
            )
            firestore.collection("groups").document(groupId)
                .collection("members").document(currentUser.uid)
                .set(member)
                .await()
            
             firestore.collection("groups").document(groupId)
                .update("memberCount", com.google.firebase.firestore.FieldValue.increment(1))
            
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
                        joinedAt = doc.getLong("joinedAt") ?: 0L
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
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }
    
    override fun getGroupsWhereUserIsAdmin(userId: String): Flow<List<Group>> = callbackFlow {
        try {
            val memberListener = firestore.collectionGroup("members")
                .whereEqualTo("userId", userId)
                .whereEqualTo("role", "ADMIN")
                .addSnapshotListener { memberSnapshot, error ->
                    if (error != null) {
                        Timber.e(error, "Error listening to user admin groups")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val groupIds = memberSnapshot?.documents?.mapNotNull { it.getString("groupId") } ?: emptyList()
                    
                    if (groupIds.isEmpty()) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    CoroutineScope(Dispatchers.IO).launch {
                        val groups = mutableListOf<Group>()
                        groupIds.chunked(10).forEach { chunk ->
                            val groupsSnapshot = firestore.collection("groups")
                                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                               .get().await()
                            groups.addAll(groupsSnapshot.toObjects(Group::class.java))
                        }
                        trySend(groups)
                    }
                }
            awaitClose { memberListener.remove() }
        } catch (e: Exception) {
            Timber.e(e, "Error in getGroupsWhereUserIsAdmin")
            trySend(emptyList())
            close(e)
        }
    }
    
    override fun getAllPostsFromUserGroups(userId: String): Flow<List<com.example.minisocialnetworkapplication.core.domain.model.Post>> = callbackFlow {
        try {
            val memberListener = firestore.collectionGroup("members")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { memberSnapshot, error ->
                    if (error != null) {
                        Timber.e(error, "Error listening to user groups for posts")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val groupIds = memberSnapshot?.documents?.mapNotNull { it.getString("groupId") } ?: emptyList()
                    
                    if (groupIds.isEmpty()) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    CoroutineScope(Dispatchers.IO).launch {
                        val posts = mutableListOf<com.example.minisocialnetworkapplication.core.domain.model.Post>()
                        groupIds.chunked(10).forEach { chunk ->
                            val postsSnapshot = firestore.collection("posts")
                                .whereIn("groupId", chunk)
                                .whereEqualTo("approvalStatus", "APPROVED")
                                .orderBy("createdAt", Query.Direction.DESCENDING)
                                .get().await()
                            postsSnapshot.documents.mapNotNull { doc ->
                                doc.toObject(com.example.minisocialnetworkapplication.core.domain.model.Post::class.java)?.copy(id = doc.id)
                            }.also { posts.addAll(it) }
                        }
                        trySend(posts.sortedByDescending { it.createdAt })
                    }
                }
            awaitClose { memberListener.remove() }
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
                val isAdminInvite = invitation.inviterRole == GroupRole.ADMIN
                
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
                        role = GroupRole.MEMBER
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
                createdAt = System.currentTimeMillis()
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
                
                val requests = snapshot?.toObjects(com.example.minisocialnetworkapplication.core.domain.model.JoinRequest::class.java) ?: emptyList()
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
                joinedAt = System.currentTimeMillis()
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
}
