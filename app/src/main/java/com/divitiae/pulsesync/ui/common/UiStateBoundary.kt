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