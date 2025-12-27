package com.example.minisocialnetworkapplication.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RemoteKeysDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(remoteKeys: List<RemoteKeys>)

    @Query("SELECT * FROM remote_keys WHERE postId = :postId")
    suspend fun remoteKeysPostId(postId: String): RemoteKeys?

    @Query("SELECT * FROM remote_keys WHERE postId = :postId")
    suspend fun getRemoteKeyByPostId(postId: String): RemoteKeys?

    @Query("DELETE FROM remote_keys")
    suspend fun clearAll()
}

