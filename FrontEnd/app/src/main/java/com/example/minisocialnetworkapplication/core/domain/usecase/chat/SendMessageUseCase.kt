package com.example.minisocialnetworkapplication.core.domain.usecase.chat

import android.net.Uri
import com.example.minisocialnetworkapplication.core.domain.model.Message
import com.example.minisocialnetworkapplication.core.domain.model.MessageType
import com.example.minisocialnetworkapplication.core.domain.repository.MessageRepository
import com.example.minisocialnetworkapplication.core.util.Result
import javax.inject.Inject

/**
 * Use case to send a message (text or media)
 */
class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Send a text message
     */
    suspend fun sendText(
        conversationId: String,
        text: String,
        replyToMessageId: String? = null
    ): Result<Message> {
        // Validation
        if (text.isBlank()) {
            return Result.Error(Exception("Message cannot be empty"))
        }
        if (text.length > 5000) {
            return Result.Error(Exception("Message is too long (max 5000 characters)"))
        }

        return messageRepository.sendTextMessage(conversationId, text.trim(), replyToMessageId)
    }

    /**
     * Send media message (images, video, audio, file)
     */
    suspend fun sendMedia(
        conversationId: String,
        type: MessageType,
        mediaUris: List<Uri>,
        caption: String? = null,
        replyToMessageId: String? = null
    ): Result<Message> {
        // Validation
        if (mediaUris.isEmpty()) {
            return Result.Error(Exception("No media selected"))
        }
        if (type == MessageType.IMAGE && mediaUris.size > 10) {
            return Result.Error(Exception("Maximum 10 images per message"))
        }

        return messageRepository.sendMediaMessage(
            conversationId, 
            type, 
            mediaUris, 
            caption?.trim(), 
            replyToMessageId
        )
    }
}
