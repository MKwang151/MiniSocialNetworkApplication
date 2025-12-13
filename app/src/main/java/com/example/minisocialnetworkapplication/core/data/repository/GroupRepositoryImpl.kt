package com.example.minisocialnetworkapplication.core.data.repository

import com.example.minisocialnetworkapplication.core.domain.model.Group
import com.example.minisocialnetworkapplication.core.domain.model.GroupMember
import com.example.minisocialnetworkapplication.core.domain.model.GroupPrivacy
import com.example.minisocialnetworkapplication.core.domain.model.GroupRole
import com.example.minisocialnetworkapplication.core.domain.repository.GroupRepository
import com.example.minisocialnetworkapplication.core.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class GroupRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : GroupRepository {

    override suspend fun createGroup(name: String, description: String, privacy: GroupPrivacy): Result<String> {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return Result.Error(Exception("User not logged in"))
        }

        return try {
            val groupId = firestore.collection("groups").document().id
            val group = Group(
                id = groupId,
                name = name,
                description = description,
                ownerId = currentUser.uid,
                privacy = privacy,
                memberCount = 1,
                createdAt = System.currentTimeMillis()
            )

            val member = GroupMember(
                userId = currentUser.uid,
                groupId = groupId,
                role = GroupRole.ADMIN,
                joinedAt = System.currentTimeMillis()
            )

            // Batch write: Create group doc and add creator as member
            firestore.runBatch { batch ->
                val groupRef = firestore.collection("groups").document(groupId)
                batch.set(groupRef, group)

                val memberRef = firestore.collection("groups").document(groupId)
                    .collection("members").document(currentUser.uid)
                batch.set(memberRef, member)
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
                    Timber.e(error, "Error listening to user groups - Check Firestore rules are deployed!")
                    // Send empty list instead of closing the flow for permission errors
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Timber.w("PERMISSION_DENIED: Firestore rules may not be deployed. Returning empty list.")
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
                        // Fetch actual groups by document ID
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
                    Timber.e(error, "Error listening to all groups - Check Firestore rules are deployed!")
                    // Send empty list instead of closing for permission errors
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Timber.w("PERMISSION_DENIED: Firestore rules may not be deployed. Returning empty list.")
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
            val member = GroupMember(
                userId = currentUser.uid,
                groupId = groupId,
                role = GroupRole.MEMBER,
                joinedAt = System.currentTimeMillis()
            )
            // Save to /groups/{groupId}/members/{userId}
            firestore.collection("groups").document(groupId)
                .collection("members").document(currentUser.uid)
                .set(member)
                .await()
            
            // Increment member count (transaction ideally, but simpler for MVP)
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
                
            // Decrement member count
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

    override fun getGroupPosts(groupId: String): Flow<List<com.example.minisocialnetworkapplication.core.domain.model.Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .whereEqualTo("groupId", groupId)
            .whereEqualTo("approvalStatus", "APPROVED") // Only show approved posts
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                 if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val posts = snapshot?.toObjects(com.example.minisocialnetworkapplication.core.domain.model.Post::class.java) ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }
    
    override suspend fun sendInvitations(groupId: String, userIds: List<String>): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.Error(Exception("User not logged in"))
        return try {
            // Get group details for invitation
            val groupDoc = firestore.collection("groups").document(groupId).get().await()
            val group = groupDoc.toObject(Group::class.java) 
                ?: return Result.Error(Exception("Group not found"))
            
            // Get current user details
            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
            val userName = userDoc.getString("name") ?: "Someone"
            
            // Create invitations for each user
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
                    inviteeId = userId,
                    status = com.example.minisocialnetworkapplication.core.domain.model.InvitationStatus.PENDING
                )
                batch.set(invitationRef, invitation)
                
                // Also create a notification
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
            
            // Update invitation status
            firestore.collection("group_invitations").document(invitationId)
                .update("status", if (accept) com.example.minisocialnetworkapplication.core.domain.model.InvitationStatus.ACCEPTED 
                                   else com.example.minisocialnetworkapplication.core.domain.model.InvitationStatus.DECLINED)
                .await()
            
            // If accepted, add user to group
            if (accept) {
                val member = com.example.minisocialnetworkapplication.core.domain.model.GroupMember(
                    userId = currentUser.uid,
                    groupId = invitation.groupId,
                    role = com.example.minisocialnetworkapplication.core.domain.model.GroupRole.MEMBER
                )
                firestore.collection("groups").document(invitation.groupId)
                    .collection("members").document(currentUser.uid)
                    .set(member)
                    .await()
                
                // Increment member count
                firestore.collection("groups").document(invitation.groupId)
                    .update("memberCount", com.google.firebase.firestore.FieldValue.increment(1))
                    .await()
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
}
