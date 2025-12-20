package com.example.minisocialnetworkapplication.core.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PostDao {

    @Query("SELECT * FROM posts WHERE approvalStatus = 'APPROVED' AND isHidden = 0 ORDER BY createdAt DESC")
    fun getAllPosts(): PagingSource<Int, PostEntity>

    @Query("SELECT * FROM posts WHERE approvalStatus = 'APPROVED' AND isHidden = 0 ORDER BY createdAt DESC")
    fun getPagingSource(): PagingSource<Int, PostEntity>

    @Query("SELECT * FROM posts WHERE authorId = :userId AND isHidden = 0 ORDER BY createdAt DESC")
    fun getUserPosts(userId: String): PagingSource<Int, PostEntity>

    @Query("SELECT * FROM posts WHERE authorId = :userId AND isHidden = 0 ORDER BY createdAt DESC")
    fun getUserPostsPagingSource(userId: String): PagingSource<Int, PostEntity>

    @Query("SELECT * FROM posts WHERE id = :postId")
    suspend fun getPost(postId: String): PostEntity?

    @Query("SELECT * FROM posts WHERE id = :postId")
    suspend fun getPostById(postId: String): PostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    @Update
    suspend fun update(post: PostEntity)

    @Query("UPDATE posts SET likedByMe = :liked, likeCount = :likeCount WHERE id = :postId")
    suspend fun updateLike(postId: String, liked: Boolean, likeCount: Int)

    @Query("UPDATE posts SET likedByMe = :liked WHERE id = :postId")
    suspend fun updateLikeStatus(postId: String, liked: Boolean)

    @Query("UPDATE posts SET commentCount = :commentCount WHERE id = :postId")
    suspend fun updateCommentCount(postId: String, commentCount: Int)

    @Query("DELETE FROM posts")
    suspend fun clearAll()

    @Query("DELETE FROM posts WHERE isSyncPending = 0")
    suspend fun clearSyncedPosts()

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    @Query("UPDATE posts SET authorName = :newName, authorAvatarUrl = :newAvatarUrl WHERE authorId = :userId")
    suspend fun updateAuthorInfo(userId: String, newName: String, newAvatarUrl: String?): Int

    @Query("UPDATE posts SET text = :newText WHERE id = :postId")
    suspend fun updatePostText(postId: String, newText: String)

    @Query("UPDATE posts SET mediaUrls = :mediaUrls WHERE id = :postId")
    suspend fun updatePostMediaUrls(postId: String, mediaUrls: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)
}
