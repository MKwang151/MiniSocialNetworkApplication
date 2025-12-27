package com.example.minisocialnetworkapplication.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_keys")
data class RemoteKeys(
    @PrimaryKey
    val postId: String,
    val prevKey: String?,
    val nextKey: String?
)

