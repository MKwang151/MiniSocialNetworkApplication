package com.example.minisocialnetworkapplication.core.domain.usecase.chat

import com.example.minisocialnetworkapplication.core.domain.repository.MessageRepository
import com.example.minisocialnetworkapplication.core.util.Result
import javax.inject.Inject

/**
 * Use case to mark messages as read
 */
class MarkMessagesAsReadUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(conversationId: String): Result<Unit> {
        if (conversationId.isBlank()) {
            return Result.Error(Exception("Invalid conversation ID"))
        }
        return messageRepository.markMessagesAsRead(conversationId)
    }
}
