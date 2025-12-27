package com.example.minisocialnetworkapplication.core.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Extension functions cho ViewModel để dễ dàng sử dụng CacheSyncUtil
 */

/**
 * Sync cache và invalidate paging source trong một lần gọi
 * Sử dụng khi cần cập nhật UI ngay lập tức sau khi có thay đổi
 */
fun ViewModel.syncCacheAndRefresh(
    cacheSyncUtil: CacheSyncUtil,
    postRepository: com.example.minisocialnetworkapplication.core.domain.repository.PostRepository,
    userId: String? = null,
    onSuccess: (Int) -> Unit = {},
    onError: (String) -> Unit = {}
) {
    viewModelScope.launch {
        try {
            val result = if (userId != null) {
                // Chỉ refresh author info (nhanh)
                cacheSyncUtil.refreshAuthorInfoInCache(userId)
            } else {
                // Sync toàn bộ posts (chậm hơn)
                cacheSyncUtil.syncPostsFromFirebase(limit = 50)
            }

            when (result) {
                is Result.Success -> {
                    Timber.d("Cache synced successfully: ${result.data} items updated")
                    postRepository.invalidatePagingSource()
                    onSuccess(result.data)
                }
                is Result.Error -> {
                    Timber.e("Cache sync failed: ${result.message}")
                    onError(result.message ?: "Unknown error")
                }
                is Result.Loading -> {}
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during cache sync")
            onError(e.message ?: "Unknown error")
        }
    }
}

/**
 * Xóa cache và trigger refresh
 * Sử dụng khi cần reset hoàn toàn
 */
fun ViewModel.clearCacheAndRefresh(
    cacheSyncUtil: CacheSyncUtil,
    postRepository: com.example.minisocialnetworkapplication.core.domain.repository.PostRepository,
    onComplete: () -> Unit = {}
) {
    viewModelScope.launch {
        try {
            val result = cacheSyncUtil.clearAllCache()
            when (result) {
                is Result.Success -> {
                    Timber.d("Cache cleared successfully")
                    postRepository.invalidatePagingSource()
                    onComplete()
                }
                is Result.Error -> {
                    Timber.e("Failed to clear cache: ${result.message}")
                    onComplete()
                }
                is Result.Loading -> {}
            }
        } catch (e: Exception) {
            Timber.e(e, "Error clearing cache")
            onComplete()
        }
    }
}

/**
 * Quick sync - Chỉ dành cho update profile
 * Tự động chọn phương pháp tốt nhất (refresh author info)
 */
fun ViewModel.quickSyncAfterProfileUpdate(
    cacheSyncUtil: CacheSyncUtil,
    postRepository: com.example.minisocialnetworkapplication.core.domain.repository.PostRepository,
    userId: String,
    onComplete: (Boolean) -> Unit = {}
) {
    viewModelScope.launch {
        try {
            Timber.d("Quick syncing after profile update for user: $userId")

            // Refresh author info trong cache
            val refreshResult = cacheSyncUtil.refreshAuthorInfoInCache(userId)

            val success = when (refreshResult) {
                is Result.Success -> {
                    Timber.d("Quick sync completed: ${refreshResult.data} posts updated")
                    postRepository.invalidatePagingSource()
                    true
                }
                is Result.Error -> {
                    Timber.e("Quick sync failed: ${refreshResult.message}")
                    // Fallback: Clear cache để force reload
                    cacheSyncUtil.clearAllCache()
                    postRepository.invalidatePagingSource()
                    false
                }
                is Result.Loading -> false
            }

            onComplete(success)
        } catch (e: Exception) {
            Timber.e(e, "Error during quick sync")
            onComplete(false)
        }
    }
}

