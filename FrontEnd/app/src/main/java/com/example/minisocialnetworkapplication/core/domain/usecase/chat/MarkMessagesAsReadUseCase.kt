package com.example.minisocialnetworkapplication.core.domain.usecase.chat

import com.example.minisocialnetworkapplication.core.domain.repository.ConversationRepository
import com.example.minisocialnetworkapplication.core.domain.repository.MessageRepository
import com.example.minisocialnetworkapplication.core.util.Result
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case to mark messages as read
 */
class MarkMessagesAsReadUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(conversationId: String): Result<Unit> {
        Timber.d("MarkMessagesAsReadUseCase: START for conv=$conversationId")
        if (conversationId.isBlank()) {
            Timber.e("MarkMessagesAsReadUseCase: Invalid conversation ID")
            return Result.Error(Exception("Invalid conversation ID"))
        }
        // Update seenBy on individual messages
        Timber.d("MarkMessagesAsReadUseCase: calling messageRepository.markMessagesAsRead")
        messageRepository.markMessagesAsRead(conversationId)
        // Update lastReadSequenceId in participants subcollection (this is what affects unread count)
        Timber.d("MarkMessagesAsReadUseCase: calling conversationRepository.markConversationAsRead")
        val result = conversationRepository.markConversationAsRead(conversationId)
        Timber.d("MarkMessagesAsReadUseCase: DONE, result=$result")
        return result
    }
}
