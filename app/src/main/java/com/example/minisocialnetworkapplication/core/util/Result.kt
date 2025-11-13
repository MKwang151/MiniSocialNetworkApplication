package com.example.minisocialnetworkapplication.core.util

/**
 * A generic wrapper class for handling results from repository/use case operations
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception, val message: String? = exception.message) : Result<Nothing>()
    data object Loading : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = if (this is Success) data else null

    fun exceptionOrNull(): Exception? = if (this is Error) exception else null
}

/**
 * Extension function to convert nullable data to Result
 */
fun <T> T?.toResult(): Result<T> {
    return if (this != null) {
        Result.Success(this)
    } else {
        Result.Error(Exception("Data is null"))
    }
}

/**
 * Extension function to handle Result with suspend functions
 */
suspend fun <T> Result<T>.onSuccess(action: suspend (T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

suspend fun <T> Result<T>.onError(action: suspend (Exception) -> Unit): Result<T> {
    if (this is Result.Error) {
        action(exception)
    }
    return this
}

