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
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val groupIds = snapshot.documents.map { it.reference.parent.parent?.id }.filterNotNull()
                    if (groupIds.isEmpty()) {
                        trySend(emptyList())
                    } else {
                        // Fetch actual groups
                         firestore.collection("groups")
                            .whereIn("id", groupIds.take(10)) 
                            .get()
                            .addOnSuccessListener { groupSnapshot ->
                                val groups = groupSnapshot.toObjects(Group::class.java)
                                trySend(groups)
                            }
                            .addOnFailureListener { e ->
                                Timber.e(e, "Error fetching groups details")
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
                    close(error)
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
}
