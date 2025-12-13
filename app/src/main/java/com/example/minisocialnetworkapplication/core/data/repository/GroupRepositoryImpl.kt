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
            return Result.Error("User not logged in")
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
            Result.Error(e.message ?: "Failed to create group")
        }
    }

    override fun getGroupsForUser(userId: String): Flow<List<Group>> = callbackFlow {
        // Query groups where 'members' subcollection contains userId. 
        // Note: Collection Group queries are needed for this efficiently, or duplicate data.
        // For simplicity in MVP without Collection Group Index:
        // Option A: Store 'groupIds' in User document (best for reads)
        // Option B: Query all groups and filter client side (BAD)
        // Option C: Use 'members' Collection Group query: collectionGroup("members").whereEqualTo("userId", userId)
        
        // We'll use Option C (Collection Group Query) - requires Index in Console usually.
        // Failing that, we rely on the parent ID being the group ID.
        
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
                        // Firestore 'in' query has limit of 10. For scalability need multiple queries or architecture change.
                        // For MVP, we fetch individually or use 'in' chunks.
                        // Let's optimize: Just fetch the groups associated.
                         firestore.collection("groups")
                            .whereIn("id", groupIds.take(10)) // Limit to 10 for now due to Firestore limitation
                            .get()
                            .addOnSuccessListener { groupSnapshot ->
                                val groups = groupSnapshot.toObjects(Group::class.java)
                                trySend(groups)
                            }
                            .addOnFailureListener { e ->
                                Timber.e(e, "Error fetching groups details")
                                // Don't close flow, just log? Or retry?
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
                Result.Error("Group not found")
            }
        } catch (e: Exception) {
             Result.Error(e.message ?: "Error details")
        }
    }
}
