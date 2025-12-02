package com.example.minisocialnetworkapplication.core.domain.usecase.user

import com.example.minisocialnetworkapplication.core.domain.model.Friend
import com.example.minisocialnetworkapplication.core.domain.repository.FriendRepository
import com.example.minisocialnetworkapplication.core.util.Result
import javax.inject.Inject

class FriendUseCase @Inject constructor(
    private val friendRepository: FriendRepository
) {
    suspend fun getFriends(userId: String): Result<List<Friend>>{
        return friendRepository.getFriends(userId)
    }

    fun addFriend(friendId: String): Result<Boolean> {
        return addFriend(friendId)
    }

    /**
    * Remove friend for current user
    * */
    fun removeFriend(friendId: String): Result<Boolean> {
        return removeFriend(friendId)
    }

    fun isFriend(friendId: String): Result<Boolean> {
        return isFriend(friendId)
    }
}