package com.divitiae.pulsesync.ui.common

import com.divitiae.pulsesync.data.domain.AppError
import com.divitiae.pulsesync.data.domain.Result

/**
 * Generic UI state wrapper (Member 4 — Robustness & Error Boundaries).
 *
 * Every screen that talks to the network exposes a `StateFlow<UiState<T>>`.
 * The composable side never sees a raw exception or a Retrofit Response — it
 * only ever branches on these three cases, so a dropped connection renders as
 * a clean [Error] pane instead of a crash.
 *
 * [Error.error] is kept so the UI can pick a localised message with
 * [AppError.toMessageRes] ... ViewModels must stay context-free, so they never
 * resolve strings themselves.
 */
sealed interface UiState<out T> {

    /** Work in flight; show a spinner / skeleton. */
    data object Loading : UiState<Nothing>

    /** Work completed; [data] is ready to render. */
    data class Success<out T>(val data: T) : UiState<T>

    /**
     * Work failed. [message] is an optional developer/server message; the UI
     * should prefer a localised string derived from [error] and fall back to
     * [message] only when [error] is null.
     */
    data class Error(
        val message: String? = null,
        val error: AppError? = null,
        val retryable: Boolean = true,
    ) : UiState<Nothing>
}

/** Bridges Member 3's data-layer [Result] onto the UI wrapper. */
fun <T> Result<T>.toUiState(): UiState<T> = when (this) {
    is Result.Success -> UiState.Success(data)
    is Result.Failure -> error.toUiState()
}

fun AppError.toUiState(): UiState.Error = UiState.Error(
    message = message,
    error = this,
    // A rejected token will not fix itself on retry; everything else might.
    retryable = this !is AppError.Unauthorized,
)

/** Maps the success payload while leaving Loading/Error untouched. */
inline fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> = when (this) {
    is UiState.Loading -> this
    is UiState.Error -> this
    is UiState.Success -> UiState.Success(transform(data))
}

val UiState<*>.isLoading: Boolean get() = this is UiState.Loading

fun <T> UiState<T>.dataOrNull(): T? = (this as? UiState.Success)?.data

/** Wraps a suspend call so an unexpected throwable becomes [UiState.Error], never a crash. */
suspend inline fun <T> uiStateOf(crossinline block: suspend () -> T): UiState<T> =
    try {
        UiState.Success(block())
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e // never swallow cancellation
    } catch (e: java.io.IOException) {
        AppError.Network(e.message).toUiState()
    } catch (e: Exception) {
        AppError.Unknown(e.message, e).toUiState()
    }