package com.example.minisocialnetworkapplication.core.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.minisocialnetworkapplication.core.domain.model.Participant
import com.example.minisocialnetworkapplication.core.domain.model.ParticipantRole
import com.google.firebase.Timestamp

@Entity(
    tableName = "participants",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["userId"]),
        Index(value = ["conversationId", "userId"], unique = true)
    ]
)
data class ParticipantEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val userId: String,
    val role: String, // MEMBER, ADMIN, OWNER
    val joinedAt: Long,
    val lastReadSequenceId: Long = 0 // Last message sequence ID read by this user
) {
    /**
     * Convert to domain model
     */
    fun toDomainModel(): Participant {
        return Participant(
            id = id,
            conversationId = conversationId,
            userId = userId,
            role = try { 
                ParticipantRole.valueOf(role) 
            } catch (e: Exception) { 
                ParticipantRole.MEMBER 
            },
            joinedAt = Timestamp(java.util.Date(joinedAt)),
            lastReadMessageId = lastReadSequenceId
        )
    }

    companion object {
        /**
         * Create entity from domain model
         */
        fun fromDomainModel(participant: Participant): ParticipantEntity {
            return ParticipantEntity(
                id = participant.id,
                conversationId = participant.conversationId,
                userId = participant.userId,
                role = participant.role.name,
                joinedAt = participant.joinedAt.toDate().time,
                lastReadSequenceId = participant.lastReadMessageId
            )
        }
    }
}
