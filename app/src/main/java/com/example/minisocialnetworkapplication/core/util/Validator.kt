package com.example.minisocialnetworkapplication.core.util

import android.util.Patterns

object Validator {

    /**
     * Validate email format
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Validate password
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= Constants.MIN_PASSWORD_LENGTH
    }

    /**
     * Validate display name
     */
    fun isValidDisplayName(name: String): Boolean {
        return name.isNotBlank() && name.length >= 2
    }

    /**
     * Validate post text
     */
    fun isValidPostText(text: String): Boolean {
        return text.isNotBlank() && text.length <= Constants.MAX_POST_TEXT_LENGTH
    }

    /**
     * Validate comment text
     */
    fun isValidCommentText(text: String): Boolean {
        return text.isNotBlank() && text.length <= Constants.MAX_COMMENT_TEXT_LENGTH
    }

    /**
     * Get email error message
     */
    fun getEmailError(email: String): String? {
        return when {
            email.isBlank() -> "Email is required"
            !isValidEmail(email) -> "Invalid email format"
            else -> null
        }
    }

    /**
     * Get password error message
     */
    fun getPasswordError(password: String): String? {
        return when {
            password.isBlank() -> "Password is required"
            password.length < Constants.MIN_PASSWORD_LENGTH ->
                "Password must be at least ${Constants.MIN_PASSWORD_LENGTH} characters"
            else -> null
        }
    }

    /**
     * Get display name error message
     */
    fun getDisplayNameError(name: String): String? {
        return when {
            name.isBlank() -> "Name is required"
            name.length < 2 -> "Name must be at least 2 characters"
            else -> null
        }
    }
}

