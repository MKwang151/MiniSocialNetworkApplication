package com.example.minisocialnetworkapplication.core.di

import android.content.Context
import com.example.minisocialnetworkapplication.core.data.local.ConversationDao
import com.example.minisocialnetworkapplication.core.data.local.MessageDao
import com.example.minisocialnetworkapplication.core.data.local.ParticipantDao
import com.example.minisocialnetworkapplication.core.data.repository.ConversationRepositoryImpl
import com.example.minisocialnetworkapplication.core.data.repository.MessageRepositoryImpl
import com.example.minisocialnetworkapplication.core.domain.repository.ConversationRepository
import com.example.minisocialnetworkapplication.core.domain.repository.MessageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatModule {

    @Provides
    @Singleton
    fun provideConversationRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        conversationDao: ConversationDao,
        participantDao: ParticipantDao,
        storage: FirebaseStorage
    ): ConversationRepository {
        return ConversationRepositoryImpl(
            firestore = firestore,
            auth = auth,
            conversationDao = conversationDao,
            participantDao = participantDao,
            storage = storage
        )
    }

    @Provides
    @Singleton
    fun provideMessageRepository(
        firestore: FirebaseFirestore,
        storage: FirebaseStorage,
        auth: FirebaseAuth,
        messageDao: MessageDao,
        conversationDao: ConversationDao,
        @ApplicationContext context: Context
    ): MessageRepository {
        return MessageRepositoryImpl(
            firestore = firestore,
            storage = storage,
            auth = auth,
            messageDao = messageDao,
            conversationDao = conversationDao,
            context = context
        )
    }
}
