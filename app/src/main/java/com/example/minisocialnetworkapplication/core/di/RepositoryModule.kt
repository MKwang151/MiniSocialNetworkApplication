package com.example.minisocialnetworkapplication.core.di

import com.example.minisocialnetworkapplication.core.data.repository.AuthRepositoryImpl
import com.example.minisocialnetworkapplication.core.data.repository.CommentRepositoryImpl
import com.example.minisocialnetworkapplication.core.data.repository.FriendRepositoryImpl
import com.example.minisocialnetworkapplication.core.data.repository.PostRepositoryImpl
import com.example.minisocialnetworkapplication.core.data.repository.UserRepositoryImpl
import com.example.minisocialnetworkapplication.core.domain.repository.AuthRepository
import com.example.minisocialnetworkapplication.core.domain.repository.CommentRepository
import com.example.minisocialnetworkapplication.core.domain.repository.FriendRepository
import com.example.minisocialnetworkapplication.core.domain.repository.PostRepository
import com.example.minisocialnetworkapplication.core.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindPostRepository(
        postRepositoryImpl: PostRepositoryImpl
    ): PostRepository

    @Binds
    @Singleton
    abstract fun bindCommentRepository(
        commentRepositoryImpl: CommentRepositoryImpl
    ): CommentRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindFriendRepository(
        friendRepositoryImpl: FriendRepositoryImpl
    ): FriendRepository
    @Binds
    @Singleton
    abstract fun bindGroupRepository(
        groupRepositoryImpl: com.example.minisocialnetworkapplication.core.data.repository.GroupRepositoryImpl
    ): com.example.minisocialnetworkapplication.core.domain.repository.GroupRepository
    
    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: com.example.minisocialnetworkapplication.core.data.repository.NotificationRepositoryImpl
    ): com.example.minisocialnetworkapplication.core.domain.repository.NotificationRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(
        reportRepositoryImpl: com.example.minisocialnetworkapplication.core.data.repository.ReportRepositoryImpl
    ): com.example.minisocialnetworkapplication.core.domain.repository.ReportRepository

    @Binds
    @Singleton
    abstract fun bindAdminRepository(
        adminRepositoryImpl: com.example.minisocialnetworkapplication.core.data.repository.AdminRepositoryImpl
    ): com.example.minisocialnetworkapplication.core.domain.repository.AdminRepository
}

