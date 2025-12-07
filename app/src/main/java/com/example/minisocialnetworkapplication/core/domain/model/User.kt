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
     * Get minutes since last active. Returns null if > 60 min or no lastActive
     */
    fun getMinutesAgo(): Int? {
        if (isOnline) return null
        val lastActiveTime = lastActive?.toDate()?.time ?: return null
        val diffMs = System.currentTimeMillis() - lastActiveTime
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs).toInt()
        return if (minutes in 1..59) minutes else null
    }
    
    /**
     * Check if should show status indicator (online or active within 24h)
     */
    fun shouldShowStatus(): Boolean {
        if (isOnline) return true
        val lastActiveTime = lastActive?.toDate()?.time ?: return false
        val diffMs = System.currentTimeMillis() - lastActiveTime
        return diffMs < TimeUnit.DAYS.toMillis(1)
    }
    
    /**
     * Get display text for user's online status (for ChatDetail header)
     * Returns empty string if inactive for more than 24 hours
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
            else -> "" // Don't show anything if more than 24h
        }
    }
}
