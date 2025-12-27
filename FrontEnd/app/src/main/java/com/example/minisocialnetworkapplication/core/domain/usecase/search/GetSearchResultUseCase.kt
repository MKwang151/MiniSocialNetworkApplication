package com.example.minisocialnetworkapplication.core.domain.usecase.search

import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.core.domain.repository.UserRepository
import com.example.minisocialnetworkapplication.core.util.Result
import javax.inject.Inject

class GetSearchResultUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend fun searchUser(query: String): Result<List<User>> {
        return userRepository.searchUsers(query)
    }
}