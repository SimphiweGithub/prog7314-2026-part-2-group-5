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