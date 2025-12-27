package com.example.minisocialnetworkapplication.core.domain.repository

import com.example.minisocialnetworkapplication.core.domain.model.Comment
import com.example.minisocialnetworkapplication.core.domain.model.Group
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.core.domain.model.Report
import com.example.minisocialnetworkapplication.core.util.Result
import kotlinx.coroutines.flow.Flow

interface AdminRepository {
    // User Management
    fun getAllUsers(): Flow<Result<List<User>>>
    suspend fun banUser(userId: String): Result<Unit>
    suspend fun unbanUser(userId: String): Result<Unit>
    
    // Content Moderation
    fun getAllPosts(): Flow<Result<List<Post>>>
    suspend fun deletePost(postId: String): Result<Unit>
    suspend fun hidePost(postId: String): Result<Unit>
    suspend fun restorePost(postId: String): Result<Unit>
    
    suspend fun deleteComment(postId: String, commentId: String): Result<Unit>
    suspend fun hideComment(postId: String, commentId: String): Result<Unit>
    suspend fun restoreComment(postId: String, commentId: String): Result<Unit>
    
    // Group Management
    fun getAllGroups(): Flow<Result<List<Group>>>
    suspend fun banGroup(groupId: String): Result<Unit>
    suspend fun unbanGroup(groupId: String): Result<Unit>
    
    // Report Management
    fun getAllReports(): Flow<Result<List<Report>>>
    suspend fun resolveReport(reportId: String, status: String): Result<Unit>
    suspend fun sendWarning(userId: String, content: String, type: String, targetId: String? = null, groupId: String? = null): Result<Unit>
    
    // Stats
    suspend fun getDashboardStats(): Result<AdminStats>
}

data class AdminStats(
    val totalUsers: Long = 0,
    val totalPosts: Long = 0,
    val totalReports: Long = 0,
    val bannedUsers: Long = 0,
    val totalGroups: Long = 0,
    val activeGroups: Long = 0,
    val newUsersToday: Long = 0,
    val postsToday: Long = 0,
    val reportsToday: Long = 0,
    val groupsToday: Long = 0,
    val userGrowthTrend: List<DailyStat> = emptyList(),
    val postGrowthTrend: List<DailyStat> = emptyList(),
    val groupGrowthTrend: List<DailyStat> = emptyList()
)

data class DailyStat(
    val date: String,
    val count: Long
)
