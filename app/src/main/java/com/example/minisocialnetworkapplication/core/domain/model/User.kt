package com.example.minisocialnetworkapplication.core.domain.model

import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val bio: String? = null,
    val fcmToken: String? = null,
    val isOnline: Boolean = false,
    val lastActive: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now()
) {
    // Alias for uid to make code more readable
    val id: String get() = uid
    
    /**
     * Get display text for user's online status
     */
    fun getStatusText(): String {
        if (isOnline) return "Active now"
        
        val lastActiveTime = lastActive?.toDate()?.time ?: return ""
        val now = System.currentTimeMillis()
        val diffMs = now - lastActiveTime
        
        return when {
            diffMs < TimeUnit.MINUTES.toMillis(1) -> "Active just now"
            diffMs < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
                "Active ${minutes}m ago"
            }
            diffMs < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
                "Active ${hours}h ago"
            }
            else -> {
                val days = TimeUnit.MILLISECONDS.toDays(diffMs)
                if (days == 1L) "Active yesterday" else "Active ${days}d ago"
            }
        }
    }
}

