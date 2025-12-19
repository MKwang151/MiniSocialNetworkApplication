package com.example.minisocialnetworkapplication.core.data.repository

import com.example.minisocialnetworkapplication.core.domain.model.*
import com.example.minisocialnetworkapplication.core.domain.repository.AdminRepository
import com.example.minisocialnetworkapplication.core.domain.repository.AdminStats
import com.example.minisocialnetworkapplication.core.domain.repository.DailyStat
import com.example.minisocialnetworkapplication.core.util.Constants
import com.example.minisocialnetworkapplication.core.util.Result
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.AggregateSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AdminRepository {

    override fun getAllUsers(): Flow<Result<List<User>>> = callbackFlow {
        val subscription = firestore.collection(Constants.COLLECTION_USERS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                val users = snapshot?.toObjects(User::class.java) ?: emptyList()
                trySend(Result.Success(users))
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun banUser(userId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update("status", User.STATUS_BANNED)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun unbanUser(userId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update("status", User.STATUS_ACTIVE)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getAllPosts(): Flow<Result<List<Post>>> = callbackFlow {
        val subscription = firestore.collection(Constants.COLLECTION_POSTS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()
                trySend(Result.Success(posts))
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .delete()
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun hidePost(postId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .update("isHidden", true)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun restorePost(postId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .update("isHidden", false)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .collection("comments")
                .document(commentId)
                .delete()
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun hideComment(postId: String, commentId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .collection("comments")
                .document(commentId)
                .update("isHidden", true)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun restoreComment(postId: String, commentId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .collection("comments")
                .document(commentId)
                .update("isHidden", false)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getAllGroups(): Flow<Result<List<Group>>> = callbackFlow {
        val subscription = firestore.collection("groups")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                val groups = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Group(
                            id = doc.getString("id") ?: "",
                            name = doc.getString("name") ?: "",
                            description = doc.getString("description") ?: "",
                            avatarUrl = doc.getString("avatarUrl"),
                            coverUrl = doc.getString("coverUrl"),
                            ownerId = doc.getString("ownerId") ?: "",
                            privacy = try { GroupPrivacy.valueOf(doc.getString("privacy") ?: "PUBLIC") } catch (e: Exception) { GroupPrivacy.PUBLIC },
                            postingPermission = try { GroupPostingPermission.valueOf(doc.getString("postingPermission") ?: "EVERYONE") } catch (e: Exception) { GroupPostingPermission.EVERYONE },
                            requirePostApproval = doc.getBoolean("requirePostApproval") ?: false,
                            memberCount = doc.getLong("memberCount") ?: 0L,
                            createdAt = (doc.getTimestamp("createdAt") ?: com.google.firebase.Timestamp(java.util.Date(doc.getLong("createdAt") ?: 0L))) ?: com.google.firebase.Timestamp.now(),
                            status = doc.getString("status") ?: Group.STATUS_ACTIVE
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing group: ${doc.id}")
                        null
                    }
                } ?: emptyList()
                trySend(Result.Success(groups))
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun banGroup(groupId: String): Result<Unit> {
        return try {
            firestore.collection("groups")
                .document(groupId)
                .update("status", Group.STATUS_BANNED)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun unbanGroup(groupId: String): Result<Unit> {
        return try {
            firestore.collection("groups")
                .document(groupId)
                .update("status", Group.STATUS_ACTIVE)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getAllReports(): Flow<Result<List<Report>>> = callbackFlow {
        val subscription = firestore.collection("reports")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                val reports = snapshot?.toObjects(Report::class.java) ?: emptyList()
                trySend(Result.Success(reports))
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun resolveReport(reportId: String, action: String): Result<Unit> {
        return try {
            val status = when (action) {
                "RESOLVED" -> ReportStatus.RESOLVED
                "DISMISSED" -> ReportStatus.DISMISSED
                else -> ReportStatus.REVIEWED
            }
            firestore.collection("reports")
                .document(reportId)
                .update("status", status)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getDashboardStats(): Result<AdminStats> {
        return try {
            val totalUsers = firestore.collection(Constants.COLLECTION_USERS).count().get(AggregateSource.SERVER).await().count
            val totalPosts = firestore.collection(Constants.COLLECTION_POSTS).count().get(AggregateSource.SERVER).await().count
            val totalReports = firestore.collection("reports").count().get(AggregateSource.SERVER).await().count
            val bannedUsers = firestore.collection(Constants.COLLECTION_USERS).whereEqualTo("status", User.STATUS_BANNED).count().get(AggregateSource.SERVER).await().count
            val totalGroups = firestore.collection("groups").count().get(AggregateSource.SERVER).await().count
            val bannedGroups = firestore.collection("groups").whereEqualTo("status", Group.STATUS_BANNED).count().get(AggregateSource.SERVER).await().count
            val activeGroups = totalGroups - bannedGroups
            
            // New items today
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            val todayTimestamp = Timestamp(todayStart)
            
            val newUsersToday = firestore.collection(Constants.COLLECTION_USERS)
                .whereGreaterThanOrEqualTo("createdAt", todayTimestamp)
                .count().get(AggregateSource.SERVER).await().count
            
            val postsToday = firestore.collection(Constants.COLLECTION_POSTS)
                .whereGreaterThanOrEqualTo("createdAt", todayTimestamp)
                .count().get(AggregateSource.SERVER).await().count

            val groupsToday = firestore.collection("groups")
                .whereGreaterThanOrEqualTo("createdAt", todayTimestamp)
                .count().get(AggregateSource.SERVER).await().count

            val reportsToday = firestore.collection("reports")
                .whereGreaterThanOrEqualTo("createdAt", todayTimestamp)
                .count().get(AggregateSource.SERVER).await().count

            // Trend data for last 7 days
            val userGrowthTrend = mutableListOf<DailyStat>()
            val postGrowthTrend = mutableListOf<DailyStat>()
            val groupGrowthTrend = mutableListOf<DailyStat>()
            val dateFormat = java.text.SimpleDateFormat("dd/MM", Locale.getDefault())
            val cal = Calendar.getInstance()
            
            for (i in 6 downTo 0) {
                cal.time = Date()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.time
                
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.time
                
                val dateStr = dateFormat.format(start)
                
                // Fetch daily counts efficiently
                val userDaily = firestore.collection(Constants.COLLECTION_USERS)
                    .whereGreaterThanOrEqualTo("createdAt", Timestamp(start))
                    .whereLessThanOrEqualTo("createdAt", Timestamp(end))
                    .count().get(AggregateSource.SERVER).await().count
                    
                val postDaily = firestore.collection(Constants.COLLECTION_POSTS)
                    .whereGreaterThanOrEqualTo("createdAt", Timestamp(start))
                    .whereLessThanOrEqualTo("createdAt", Timestamp(end))
                    .count().get(AggregateSource.SERVER).await().count

                val groupDaily = firestore.collection("groups")
                    .whereGreaterThanOrEqualTo("createdAt", Timestamp(start))
                    .whereLessThanOrEqualTo("createdAt", Timestamp(end))
                    .count().get(AggregateSource.SERVER).await().count
                
                userGrowthTrend.add(DailyStat(dateStr, userDaily))
                postGrowthTrend.add(DailyStat(dateStr, postDaily))
                groupGrowthTrend.add(DailyStat(dateStr, groupDaily))
            }

            Result.Success(AdminStats(
                totalUsers = totalUsers,
                totalPosts = totalPosts,
                totalReports = totalReports,
                bannedUsers = bannedUsers,
                totalGroups = totalGroups,
                activeGroups = activeGroups,
                newUsersToday = newUsersToday,
                postsToday = postsToday,
                reportsToday = reportsToday,
                groupsToday = groupsToday,
                userGrowthTrend = userGrowthTrend,
                postGrowthTrend = postGrowthTrend,
                groupGrowthTrend = groupGrowthTrend
            ))
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch dashboard stats")
            Result.Error(e)
        }
    }
}
