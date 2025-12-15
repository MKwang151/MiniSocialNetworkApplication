package com.example.minisocialnetworkapplication.core.data.remote

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.minisocialnetworkapplication.core.data.local.AppDatabase
import com.example.minisocialnetworkapplication.core.data.local.PostEntity
import com.example.minisocialnetworkapplication.core.data.local.RemoteKeys
import com.example.minisocialnetworkapplication.core.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import timber.log.Timber

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val database: AppDatabase,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : RemoteMediator<Int, PostEntity>() {

    private val postDao = database.postDao()
    private val remoteKeysDao = database.remoteKeysDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        return try {
            val currentUserId = auth.currentUser?.uid

            // Determine the page to load
            val loadKey = when (loadType) {
                LoadType.REFRESH -> {
                    Timber.d("LoadType.REFRESH")
                    null
                }
                LoadType.PREPEND -> {
                    Timber.d("LoadType.PREPEND")
                    return MediatorResult.Success(endOfPaginationReached = true)
                }
                LoadType.APPEND -> {
                    Timber.d("LoadType.APPEND")
                    val remoteKey = getRemoteKeyForLastItem(state)
                    if (remoteKey?.nextKey == null) {
                        return MediatorResult.Success(endOfPaginationReached = true)
                    }
                    remoteKey.nextKey
                }
            }

            // Build Firestore query
            var query = firestore.collection(Constants.COLLECTION_POSTS)
                .orderBy(Constants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .limit(state.config.pageSize.toLong())

            // Add cursor for pagination
            if (loadKey != null) {
                val lastDoc = firestore.collection(Constants.COLLECTION_POSTS)
                    .document(loadKey)
                    .get()
                    .await()
                if (lastDoc.exists()) {
                    query = query.startAfter(lastDoc)
                }
            }

            // Fetch posts from Firestore
            val snapshot = query.get().await()
            
            // Get list of groups user is member of (for filtering private group posts)
            val userGroupIds = if (currentUserId != null) {
                try {
                    firestore.collectionGroup("members")
                        .whereEqualTo("userId", currentUserId)
                        .get()
                        .await()
                        .documents
                        .mapNotNull { it.getString("groupId") }
                        .toSet()
                } catch (e: Exception) {
                    Timber.e(e, "Error fetching user group memberships")
                    emptySet()
                }
            } else {
                emptySet()
            }
            
            // Get set of private group IDs for filtering
            val privateGroupIds = mutableSetOf<String>()
            val groupIdsToCheck = snapshot.documents.mapNotNull { it.getString("groupId") }.distinct()
            if (groupIdsToCheck.isNotEmpty()) {
                try {
                    // Check group privacy in batches of 10
                    groupIdsToCheck.chunked(10).forEach { chunk ->
                        val groupsSnapshot = firestore.collection("groups")
                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                            .get()
                            .await()
                        groupsSnapshot.documents.forEach { doc ->
                            val privacy = doc.getString("privacy")
                            if (privacy == "PRIVATE") {
                                privateGroupIds.add(doc.id)
                            }
                        }
                    }
                    Timber.d("Found ${privateGroupIds.size} private groups from ${groupIdsToCheck.size} groups to check")
                } catch (e: Exception) {
                    Timber.e(e, "Error fetching group privacy info")
                }
            }
            
            val posts = snapshot.documents.mapNotNull { doc ->
                try {
                    val authorId = doc.getString(Constants.FIELD_AUTHOR_ID) ?: return@mapNotNull null
                    val authorName = doc.getString("authorName") ?: ""
                    val authorAvatarUrl = doc.getString("authorAvatarUrl")
                    val text = doc.getString("text") ?: ""
                    val mediaUrls = (doc.get("mediaUrls") as? List<*>)
                        ?.filterIsInstance<String>()
                        ?: emptyList()
                    val likeCount = doc.getLong(Constants.FIELD_LIKE_COUNT)?.toInt() ?: 0
                    val commentCount = doc.getLong(Constants.FIELD_COMMENT_COUNT)?.toInt() ?: 0
                    val createdAt = doc.getTimestamp(Constants.FIELD_CREATED_AT)?.toDate()?.time
                        ?: System.currentTimeMillis()

                    // Group fields
                    val groupId = doc.getString("groupId")
                    val groupName = doc.getString("groupName")
                    val groupAvatarUrl = doc.getString("groupAvatarUrl")
                    val approvalStatus = doc.getString("approvalStatus") ?: "APPROVED"

                    // Filter: Hide posts from private groups if user is not a member
                    if (groupId != null && privateGroupIds.contains(groupId) && !userGroupIds.contains(groupId)) {
                        Timber.d("Filtering out post from private group: $groupId")
                        return@mapNotNull null
                    }
                    
                    // Filter: Hide pending/rejected posts from feed (only show APPROVED)
                    if (approvalStatus != "APPROVED") {
                        Timber.d("Filtering out non-approved post: ${doc.id}, status: $approvalStatus")
                        return@mapNotNull null
                    }

                    // Check if liked by current user
                    val likedByMe = if (currentUserId != null) {
                        checkIfLiked(doc.id, currentUserId)
                    } else {
                        false
                    }

                    PostEntity(
                        id = doc.id,
                        authorId = authorId,
                        authorName = authorName,
                        authorAvatarUrl = authorAvatarUrl,
                        text = text,
                        mediaUrls = mediaUrls.joinToString(","),
                        likeCount = likeCount,
                        commentCount = commentCount,
                        likedByMe = likedByMe,
                        createdAt = createdAt,
                        isSyncPending = false,
                        groupId = groupId,
                        groupName = groupName,
                        groupAvatarUrl = groupAvatarUrl,
                        approvalStatus = approvalStatus
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing post: ${doc.id}")
                    null
                }
            }

            val endOfPaginationReached = posts.isEmpty() || posts.size < state.config.pageSize

            // Update database
            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    // Clear only remote keys, not posts
                    // Posts will be updated with REPLACE strategy in insertAll
                    remoteKeysDao.clearAll()
                }

                // Create remote keys
                val prevKey = if (loadKey == null) null else posts.firstOrNull()?.id
                val nextKey = if (endOfPaginationReached) null else posts.lastOrNull()?.id

                val remoteKeys = posts.map { post ->
                    RemoteKeys(
                        postId = post.id,
                        prevKey = prevKey,
                        nextKey = nextKey
                    )
                }

                remoteKeysDao.insertAll(remoteKeys)
                // insertAll uses REPLACE strategy, so it will update existing posts
                // and pending posts will be kept
                postDao.insertAll(posts)
            }

            Timber.d("Loaded ${posts.size} posts, endOfPaginationReached=$endOfPaginationReached")
            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)

        } catch (e: Exception) {
            Timber.e(e, "Error loading posts from Firestore")
            MediatorResult.Error(e)
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, PostEntity>): RemoteKeys? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()?.let { post ->
            remoteKeysDao.getRemoteKeyByPostId(post.id)
        }
    }

    private suspend fun checkIfLiked(postId: String, userId: String): Boolean {
        return try {
            val likeId = "${userId}_$postId"
            val likeDoc = firestore.collection(Constants.COLLECTION_LIKES)
                .document(likeId)
                .get()
                .await()
            likeDoc.exists()
        } catch (e: Exception) {
            Timber.e(e, "Error checking like status for post $postId")
            false
        }
    }
}

