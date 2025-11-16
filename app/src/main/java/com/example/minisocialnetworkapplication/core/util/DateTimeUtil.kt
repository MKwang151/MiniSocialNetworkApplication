package com.example.minisocialnetworkapplication.core.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateTimeUtil {

    private const val PATTERN_FULL = "dd MMM yyyy, HH:mm"
    private const val PATTERN_DATE = "dd MMM yyyy"
    private const val PATTERN_TIME = "HH:mm"

    /**
     * Format timestamp to readable string
     */
    fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "$minutes minute${if (minutes > 1) "s" else ""} ago"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "$hours hour${if (hours > 1) "s" else ""} ago"
            }
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                "$days day${if (days > 1) "s" else ""} ago"
            }
            else -> SimpleDateFormat(PATTERN_DATE, Locale.getDefault()).format(date)
        }
    }

    /**
     * Format Date to string
     */
    fun formatDate(date: Date, pattern: String = PATTERN_FULL): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
    }

    /**
     * Format Firebase Timestamp to relative time string
     */
    fun formatRelativeTime(timestamp: Timestamp): String {
        return formatTimestamp(timestamp.toDate().time)
    }

    /**
     * Alias for formatRelativeTime
     */
    fun getRelativeTime(timestamp: Timestamp): String {
        return formatRelativeTime(timestamp)
    }
}

