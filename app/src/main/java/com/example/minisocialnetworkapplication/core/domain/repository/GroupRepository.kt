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
}
