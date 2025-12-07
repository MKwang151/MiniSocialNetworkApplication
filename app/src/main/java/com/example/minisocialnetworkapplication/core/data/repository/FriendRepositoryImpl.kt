package com.example.minisocialnetworkapplication.core.data.repository

import com.example.minisocialnetworkapplication.core.domain.model.Friend
import com.example.minisocialnetworkapplication.core.domain.model.FriendStatus
import com.example.minisocialnetworkapplication.core.domain.repository.FriendRepository
import com.example.minisocialnetworkapplication.core.util.Constants
import com.example.minisocialnetworkapplication.core.util.Result
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.tasks.asDeferred
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class FriendRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
): FriendRepository {

    override suspend fun getUserFriends(userId: String): Result<List<Friend>> {
        return try {
            val snapshot = firestore
                .friends(userId)
                .get()
                .await()

            val friendIds = snapshot.documents.map { it.id }
            val friends = getFriendsData(friendIds)

            Timber.d("Fetched ${friends.size} friends, uid=$userId")
            Result.Success(friends)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user's friends")
            Result.Error(e)
        }
    }

    override suspend fun getFriendRequests(): Result<List<Friend>> {
        val userId = auth.currentUser?.uid
            ?: return Result.Error(Exception("User not authenticated"))

        return try {
            val snapshot = firestore
                .receivedRequests(userId)
                .get()
                .await()

            val friendIds = snapshot.documents.map { it.id }
            val friends = getFriendsData(friendIds)

            Timber.d("Fetched ${friends.size} requests, uid=$userId")
            Result.Success(friends)
        }
        catch (e: Exception) {
            Timber.e(e, "Failed to get user's friend requests")
            Result.Error(e)
        }
    }

    override suspend fun sendFriendRequest(friendId: String): Result<Unit> {
        val userId = auth.currentUser?.uid
            ?: return Result.Error(Exception("User not authenticated"))

        if (friendId == userId) // also prevent error for `removeFriend`, `isFriend`, etc
            return Result.Error((Exception("Cannot send friend request to yourself")))

        return try {
            val receivedRequest = firestore
                .receivedRequests(userId)
                .document(friendId)

            // mutual request -> auto accept
            if (receivedRequest.get().await().exists()) {
                return acceptFriendRequest(friendId)   // accept their request
            }

            val batch = firestore.batch()

            val senderRef = firestore
                .sentRequests(userId) // current user is sender
                .document(friendId)

            if (senderRef.get().await().exists()) {
                return Result.Error(Exception("Uid=$userId already sent friend request to uid=$friendId"))
            }

            val receiverRef = firestore
                .receivedRequests(friendId)
                .document(userId)

            val friendData = mapOf("timestamp" to Timestamp.now())

            // Use SetOptions.merge() to avoid duplicates (should not happen but ok)
            batch.set(senderRef, friendData, SetOptions.merge())
            batch.set(receiverRef, friendData, SetOptions.merge())

            // Commit batch
            batch.commit().await()

            Timber.d("Uid=$userId sent friend request to uid=$friendId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user's friends")
            Result.Error(e)
        }
    }

    override suspend fun acceptFriendRequest(friendId: String): Result<Unit> {
        val userId = auth.currentUser?.uid
            ?: return Result.Error(Exception("User not authenticated"))

        return try {
            val batch = firestore.batch()

            val ownRef = firestore
                .friends(userId)
                .document(friendId)

            val friendRef = firestore
                .friends(friendId)
                .document(userId)

            val senderRef = firestore // current user is not sender
                .sentRequests(friendId)
                .document(userId)

            val receiverRef = firestore
                .receivedRequests(userId)
                .document(friendId)

            val friendData = mapOf("timestamp" to Timestamp.now())

            batch.set(ownRef, friendData)
            batch.set(friendRef, friendData)
            batch.delete(receiverRef)
            batch.delete(senderRef)

            batch.commit().await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user's friend requests")
            Result.Error(e)
        }
    }

    override suspend fun removeFriendRequest(friendId: String, isSender: Boolean): Result<Unit> {
        val userId = auth.currentUser?.uid
            ?: return Result.Error(Exception("User not authenticated"))

        return try {
            val batch = firestore.batch()

            val ownRef = firestore
                .userDoc(userId)
                .collection(
                    if (isSender) Constants.COLLECTION_SENT_REQUESTS
                    else Constants.COLLECTION_RECEIVED_REQUESTS
                )
                .document(friendId)

            val friendRef = firestore
                .userDoc(friendId)
                .collection(
                    if (isSender) Constants.COLLECTION_RECEIVED_REQUESTS
                    else Constants.COLLECTION_SENT_REQUESTS
                )
                .document(userId)

            batch.delete(ownRef)
            batch.delete(friendRef)

            batch.commit().await()

            Timber.d("Uid=$userId removed friend request ${if (isSender) "to" else "of"} uid=$friendId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user's friends")
            Result.Error(e)
        }
    }

    override suspend fun unfriend(friendId: String): Result<Unit> {
        val userId = auth.currentUser?.uid
            ?: return Result.Error(Exception("User not authenticated"))

        return try {
            val ownRef = firestore
                .friends(userId)
                .document(friendId)

            if (!ownRef.get().await().exists()) {
                return Result.Error(Exception("Uid=$userId haven't added uid=$friendId"))
            }

            val friendRef = firestore
                .friends(friendId)
                .document(userId)

            val batch = firestore.batch()
            batch.delete(ownRef)
            batch.delete(friendRef)

            batch.commit().await()

            Timber.d("Uid=$userId unfriended uid=$friendId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user's friends")
            Result.Error(e)
        }
    }

    override suspend fun getFriendStatus(friendId: String): Result<FriendStatus> {
        val userId = auth.currentUser?.uid
            ?: return Result.Error(Exception("User not authenticated"))

        return try {
            val userFriendRef = firestore
                .friends(userId)
                .document(friendId)

            val sentRef = firestore
                .sentRequests(userId)
                .document(friendId)

            val receivedRef = firestore
                .receivedRequests(userId)
                .document(friendId)

            // Run all 3 reads in parallel for speed
            val (friend, sent, received) = awaitAll(
                userFriendRef.get().asDeferred(),
                sentRef.get().asDeferred(),
                receivedRef.get().asDeferred()
            )

            val status = when {
                friend.exists() -> FriendStatus.FRIEND
                sent.exists() -> FriendStatus.REQUEST_SENT
                received.exists() -> FriendStatus.REQUEST_RECEIVED
                else -> FriendStatus.NONE
            }

            Result.Success(status)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user's friends")
            Result.Error(e)
        }
    }

    //
    // HELPER FUNCTIONS
    //
    
    private suspend fun getFriendsData(friendIds: List<String>): MutableList<Friend> {
        val chunks = friendIds.chunked(10) // whereIn limit
        val friends = mutableListOf<Friend>()

        for (chunk in chunks) {
            val snapshot = firestore
                .collection(Constants.COLLECTION_USERS)
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()
            friends += snapshot.toObjects(Friend::class.java)
        }

        return friends
    }

    private fun FirebaseFirestore.userDoc(userId: String) =
        this.collection(Constants.COLLECTION_USERS).document(userId)

    private fun FirebaseFirestore.friends(userId: String) =
        userDoc(userId).collection(Constants.COLLECTION_FRIENDS)

    private fun FirebaseFirestore.sentRequests(userId: String) =
        userDoc(userId).collection(Constants.COLLECTION_SENT_REQUESTS)

    private fun FirebaseFirestore.receivedRequests(userId: String) =
        userDoc(userId).collection(Constants.COLLECTION_RECEIVED_REQUESTS)
}