package com.example.minisocialnetworkapplication.core.domain.usecase.user

import com.example.minisocialnetworkapplication.core.domain.model.Friend
import com.example.minisocialnetworkapplication.core.domain.repository.FriendRepository
import com.example.minisocialnetworkapplication.core.util.Result
import javax.inject.Inject

class FriendUseCase @Inject constructor(
    private val friendRepository: FriendRepository
) {
    suspend fun getUserFriends(userId: String): Result<List<Friend>>{
        return friendRepository.getUserFriends(userId)
    }

    suspend fun addFriend(friendId: String): Result<Unit> {
        return friendRepository.addFriend(friendId)
    }

    /**
    * Remove friend for current user
    * */
    suspend fun removeFriend(friendId: String): Result<Unit> {
        return friendRepository.removeFriend(friendId)
    }

    suspend fun isFriend(friendId: String): Result<Boolean> {
        return friendRepository.isFriend(friendId)
    }
}