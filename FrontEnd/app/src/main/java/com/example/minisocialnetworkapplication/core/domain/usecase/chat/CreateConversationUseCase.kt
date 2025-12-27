package com.example.minisocialnetworkapplication.core.domain.usecase.chat

import com.example.minisocialnetworkapplication.core.domain.model.Conversation
import com.example.minisocialnetworkapplication.core.domain.repository.ConversationRepository
import com.example.minisocialnetworkapplication.core.util.Result
import javax.inject.Inject

/**
 * Use case to create or get existing conversation
 */
class CreateConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    /**
     * Create or get existing direct conversation with another user
     */
    suspend fun createDirectConversation(otherUserId: String): Result<Conversation> {
        if (otherUserId.isBlank()) {
            return Result.Error(Exception("Invalid user ID"))
        }
        return conversationRepository.getOrCreateDirectConversation(otherUserId)
    }

    /**
     * Create a new group conversation
     */
    suspend fun createGroupConversation(
        name: String,
        participantIds: List<String>,
        avatarUrl: String? = null
    ): Result<Conversation> {
        // Validation
        if (name.isBlank()) {
            return Result.Error(Exception("Group name cannot be empty"))
        }
        if (name.length > 100) {
            return Result.Error(Exception("Group name is too long (max 100 characters)"))
        }
        if (participantIds.size < 2) {
            return Result.Error(Exception("Group must have at least 2 other members"))
        }
        if (participantIds.size > 255) {
            return Result.Error(Exception("Group cannot have more than 256 members"))
        }

        return conversationRepository.createGroupConversation(name.trim(), participantIds, avatarUrl)
    }
}
