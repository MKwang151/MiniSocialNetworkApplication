package com.example.minisocialnetworkapplication.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PostEntity::class, 
        RemoteKeys::class,
        ConversationEntity::class,
        MessageEntity::class,
        ParticipantEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun remoteKeysDao(): RemoteKeysDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun participantDao(): ParticipantDao

    companion object {
        const val DATABASE_NAME = "mini_social_db"
    }
}
