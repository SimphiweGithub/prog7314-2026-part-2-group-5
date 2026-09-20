package com.divitiae.pulsesync.data.domain

/**
 * Outcome of a data-layer operation. Repositories return this instead of
 * throwing so that Member 4's ViewModels can branch on success/failure
 * without wrapping every call in try/catch.
 */
sealed interface Result<out T> {
    data class Success<out T>(val data: T) : Result<T>
    data class Failure(val error: AppError) : Result<Nothing>
}

/** Categorised failure so the UI can show a sensible message. */
sealed class AppError(open val message: String?) {
    /** No connectivity, timeout, or DNS failure. */
    data class Network(override val message: String? = null) : AppError(message)

    /** Server responded with a non-2xx status. */
    data class Http(val code: Int, override val message: String? = null) : AppError(message)

    /** Token missing, expired, or rejected (HTTP 401/403). */
    data class Unauthorized(override val message: String? = null) : AppError(message)

    /** Anything not covered above. */
    data class Unknown(override val message: String? = null, val cause: Throwable? = null) :
        AppError(message)
}

inline fun <T> Result<T>.onSuccess(block: (T) -> Unit): Result<T> {
    if (this is Result.Success) block(data)
    return this
}

inline fun <T> Result<T>.onFailure(block: (AppError) -> Unit): Result<T> {
    if (this is Result.Failure) block(error)
    return this
}

fun <T> Result<T>.getOrNull(): T? = (this as? Result.Success)?.data
