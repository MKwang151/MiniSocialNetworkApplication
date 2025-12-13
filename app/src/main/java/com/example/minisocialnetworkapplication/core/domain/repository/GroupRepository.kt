package com.example.minisocialnetworkapplication.core.domain.repository

import com.example.minisocialnetworkapplication.core.domain.model.Group
import com.example.minisocialnetworkapplication.core.domain.model.GroupPrivacy
import com.example.minisocialnetworkapplication.core.util.Result
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    suspend fun createGroup(name: String, description: String, privacy: GroupPrivacy): Result<String>
    fun getGroupsForUser(userId: String): Flow<List<Group>>
    fun getAllGroups(): Flow<List<Group>>
    suspend fun getGroupDetails(groupId: String): Result<Group>
    
    // Membership
    suspend fun joinGroup(groupId: String): Result<Unit>
    suspend fun leaveGroup(groupId: String): Result<Unit>
    suspend fun isMember(groupId: String, userId: String): Result<Boolean>
    
    // Feed
    fun getGroupPosts(groupId: String): Flow<List<com.example.minisocialnetworkapplication.core.domain.model.Post>>
    
    // Invitations
    suspend fun sendInvitations(groupId: String, userIds: List<String>): Result<Unit>
    suspend fun respondToInvitation(invitationId: String, accept: Boolean): Result<Unit>
    fun getInvitationsForUser(userId: String): Flow<List<com.example.minisocialnetworkapplication.core.domain.model.GroupInvitation>>
}
