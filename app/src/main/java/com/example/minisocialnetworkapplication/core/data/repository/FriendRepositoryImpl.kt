package com.example.minisocialnetworkapplication.core.data.repository

import com.example.minisocialnetworkapplication.core.domain.model.Friend
import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.core.domain.repository.FriendRepository
import com.example.minisocialnetworkapplication.core.domain.repository.UserRepository
import com.example.minisocialnetworkapplication.core.util.Constants
import com.example.minisocialnetworkapplication.core.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class FriendRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
): FriendRepository {

    override suspend fun getUserFriends(userId: String): Result<List<Friend>> {
        return try {
            val querySnapshot = firestore
                .collection(Constants.COLLECTION_FRIENDS)
                .whereEqualTo("uid", userId)
                .get()
                .await()

            val friendIds = querySnapshot.documents.mapNotNull { doc ->
                Friend(
                    uid = userId,
                    friendId = doc.getString("friendId") ?: return@mapNotNull null,
                    friendName = doc.getString("friendName") ?: "",
                    friendAvatarUrl = doc.getString("friendAvatarUrl") ?: ""
                )
            }

            Timber.d("Fetched ${friendIds.size}, uid = $userId")
            Result.Success(friendIds)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user's friends")
            Result.Error(e)
        }
    }

    override suspend fun addFriend(friendId: String): Result<Unit> {
        val userId = auth.currentUser?.uid
            ?: return Result.Error(Exception("User not authenticated"))

        if (friendId == userId) // also prevent error for `removeFriend` and `isFriend`
            return Result.Error((Exception("User cannot be their own friend")))

        return try {
            // adding A-B and B-A
            val docRefOwn = firestore
                .collection("friends")
                .document("${userId}_$friendId")

            if (docRefOwn.get().await().exists()) {
                return Result.Error(Exception("Uid $userId already added uid $friendId"))
            }

            val docRefFriend = firestore
                .collection("friends")
                .document("${friendId}_$userId")

            val userData = userRepository.getCurrentUser().getOrNull()
                ?: return Result.Error(Exception("Error getting current user"))
            val friendData = userRepository.getUser(friendId).getOrNull()
                ?: return Result.Error(Exception("Error getting uid $friendId data"))

            val batch = firestore.batch()
            // Use SetOptions.merge() to avoid duplicates
            batch.set(
                docRefOwn,
                createFriend(userData, friendData),
                SetOptions.merge())
            batch.set(docRefFriend,
                createFriend(friendData, userData),
                SetOptions.merge())

            // Commit batch
            batch.commit().await()

            Timber.d("Uid $userId added uid $friendId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user's friends")
            Result.Error(e)
        }
    }

    override suspend fun removeFriend(friendId: String): Result<Unit> {
        val userId = auth.currentUser?.uid
            ?: return Result.Error(Exception("User not authenticated"))

        return try {
            val docRefOwn = firestore
                .collection("friends")
                .document("${userId}_$friendId")

            if (!docRefOwn.get().await().exists()) {
                return Result.Error(Exception("Uid $userId haven't added uid $friendId"))
            }

            val docRefFriend = firestore
                .collection("friends")
                .document("${friendId}_$userId")

            val batch = firestore.batch()
            batch.delete(docRefOwn)
            batch.delete(docRefFriend)

            batch.commit().await()

            Timber.d("Uid $userId unfriended uid $friendId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user's friends")
            Result.Error(e)
        }
    }

    override suspend fun isFriend(friendId: String): Result<Boolean> {
        val userId = auth.currentUser?.uid
            ?: return Result.Error(Exception("User not authenticated"))

        return try {
            val document = firestore
                .collection("friends")
                .document("${userId}_$friendId")
                .get()
                .await()

            Result.Success(document.exists())
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user's friends")
            Result.Error(e)
        }
    }

    override suspend fun updateFriendProfile(): Result<Unit> {
        val user = userRepository.getCurrentUser().getOrNull()
            ?: return Result.Error(Exception("User not authenticated"))

        return try {
            val snapshot = firestore
                .collection("friends")
                .whereEqualTo("friendId", user.id)
                .get()
                .await()

            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.update(doc.reference, mapOf(
                    "friendName" to user.name,
                    "friendAvatar" to user.name
                ))
            }

            batch.commit().await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update uid ${user.id} friend profile")
            Result.Error(e)
        }
    }

    fun createFriend(user: User, friend: User): Friend {
        return Friend(
            uid = user.id,
            friendId = friend.id,
            friendName = friend.name,
            friendAvatarUrl = friend.avatarUrl
        )
    }
}