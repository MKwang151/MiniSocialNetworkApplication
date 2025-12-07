package com.example.minisocialnetworkapplication.core.domain.usecase.chat

import com.example.minisocialnetworkapplication.core.domain.model.Message
import com.example.minisocialnetworkapplication.core.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get messages for a conversation
 */
class GetMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(conversationId: String): Flow<List<Message>> {
        return messageRepository.getMessages(conversationId)
    }
}
