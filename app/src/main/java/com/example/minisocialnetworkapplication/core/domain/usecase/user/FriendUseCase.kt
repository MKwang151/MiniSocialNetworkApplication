package com.example.minisocialnetworkapplication.core.domain.usecase.user

import com.example.minisocialnetworkapplication.core.domain.model.Friend
import com.example.minisocialnetworkapplication.core.domain.model.FriendStatus
import com.example.minisocialnetworkapplication.core.domain.repository.FriendRepository
import com.example.minisocialnetworkapplication.core.util.Result
import javax.inject.Inject

class FriendUseCase @Inject constructor(
    private val friendRepository: FriendRepository
) {
    suspend fun getUserFriends(userId: String): Result<List<Friend>> {
        return friendRepository.getUserFriends(userId)
    }

    suspend fun getFriendRequests(): Result<List<Friend>> {
        return friendRepository.getFriendRequests()
    }

    suspend fun sendFriendRequest(friendId: String): Result<Unit> {
        return friendRepository.sendFriendRequest(friendId)
    }

    suspend fun acceptFriendRequest(friendId: String): Result<Unit> {
        return friendRepository.acceptFriendRequest(friendId)
    }

    suspend fun removeFriendRequest(friendId: String, isSender: Boolean): Result<Unit> {
        return friendRepository.removeFriendRequest(friendId, isSender)
    }

    suspend fun removeFriend(friendId: String): Result<Unit> {
        return friendRepository.unfriend(friendId)
    }

    suspend fun getFriendStatus(friendId: String): Result<FriendStatus> {
        return friendRepository.getFriendStatus(friendId)
    }
}