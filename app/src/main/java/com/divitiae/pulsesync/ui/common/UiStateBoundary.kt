package com.divitiae.pulsesync.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.divitiae.pulsesync.R
import com.divitiae.pulsesync.data.domain.AppError

/**
 * Error boundary for network-backed screens. Renders a spinner while loading,
 * a friendly retry pane on failure, and hands the payload to [content] on
 * success. Screens that have their own skeleton (the Feed) can override
 * [loading] so the existing skeleton is reused.
 */
@Composable
fun <T> UiStateBoundary(
    state: UiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    loading: @Composable () -> Unit = { LoadingPane(modifier) },
    error: @Composable (UiState.Error) -> Unit = { ErrorPane(it, onRetry, modifier) },
    content: @Composable (T) -> Unit,
) {
    when (state) {
        is UiState.Loading -> loading()
        is UiState.Error -> error(state)
        is UiState.Success -> content(state.data)
    }
}

@Composable
fun LoadingPane(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun ErrorPane(
    state: UiState.Error,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = state.userMessage(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
        )
        if (state.retryable) {
            Button(onClick = onRetry) { Text(stringResource(R.string.common_retry)) }
        }
    }
}

/**
 * Resolves a localised, non-technical message for an error. Preference order:
 * a category-specific string for [UiState.Error.error], then the raw
 * [UiState.Error.message], then the generic fallback.
 */
@Composable
fun UiState.Error.userMessage(): String {
    val appError = error
    return when (appError) {
        is AppError.Network -> stringResource(R.string.common_error_network)
        is AppError.Unauthorized -> stringResource(R.string.common_error_unauthorized)
        is AppError.Http -> stringResource(R.string.common_error_server, appError.code)
        is AppError.Unknown, null ->
            message?.takeIf { it.isNotBlank() } ?: stringResource(R.string.common_error_unknown)
    }
}

/** Same resolution as [userMessage] but usable from a non-composable (e.g. a Snackbar lambda). */
fun AppError?.toMessageRes(): Int = when (this) {
    is AppError.Network -> R.string.common_error_network
    is AppError.Unauthorized -> R.string.common_error_unauthorized
    is AppError.Http -> R.string.common_error_server_generic
    is AppError.Unknown, null -> R.string.common_error_unknown
}