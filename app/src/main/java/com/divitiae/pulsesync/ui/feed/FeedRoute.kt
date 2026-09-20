package com.divitiae.pulsesync.ui.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.divitiae.pulsesync.R
import com.divitiae.pulsesync.ui.common.UiState
import com.divitiae.pulsesync.ui.common.UiStateBoundary
import com.divitiae.pulsesync.ui.common.userMessage
import com.divitiae.pulsesync.ui.components.BottomDestination
import com.divitiae.pulsesync.ui.util.ExternalLinks
import kotlinx.coroutines.launch

/**
 * ViewModel-backed replacement for the prototype [FeedScreen]. Point the
 * NavHost's FEED destination at this; [FeedScreen] can stay in the codebase
 * for previews.
 *
 * Error boundary behaviour:
 *  - Loading  → the existing skeleton cards (FeedContent with isLoading = true)
 *  - Error    → ErrorPane with Retry (only if Room itself failed — rare)
 *  - Refresh failure while data is cached → Snackbar, list stays visible
 */
@Composable
fun FeedRoute(
    onOpenArticle: (ArticleUi) -> Unit,
    onNavigate: (BottomDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = viewModel(factory = FeedViewModel.Factory),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val refreshError by viewModel.refreshError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }

    val noAppMessage = stringResource(R.string.article_no_app_for_link)
    val refreshFailedTemplate = stringResource(R.string.feed_refresh_failed)
    val refreshErrorText = refreshError?.userMessage()

    LaunchedEffect(refreshErrorText) {
        if (refreshErrorText != null) {
            snackbarHostState.showSnackbar(refreshFailedTemplate.format(refreshErrorText))
            viewModel.consumeRefreshError()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        UiStateBoundary(
            state = state,
            onRetry = viewModel::refresh,
            loading = {
                FeedContent(
                    state = FeedUiState(
                        categories = listOf(FeedSampleData.GENERAL_FEED),
                        selectedCategory = FeedSampleData.GENERAL_FEED,
                        isLoading = true,
                    ),
                    query = query,
                    onQueryChange = { query = it },
                    onSelectCategory = {},
                    onRefresh = {},
                    onToggleSummary = {},
                    onToggleSave = {},
                    onOpenSource = {},
                    onAddNote = {},
                    onOpenResources = {},
                    onOpenArticle = {},
                    onNavigate = onNavigate,
                )
            },
        ) { feed ->
            FeedContent(
                state = feed,
                query = query,
                onQueryChange = { query = it },
                onSelectCategory = viewModel::selectCategory,
                onRefresh = viewModel::refresh,
                onToggleSummary = viewModel::toggleSummaryMode,
                onToggleSave = viewModel::toggleSave,
                onOpenSource = { article ->
                    // User Defined Feature 2 entry point from the card: explicit ACTION_VIEW.
                    if (!ExternalLinks.open(context, article.sourceUrl)) {
                        scope.launch { snackbarHostState.showSnackbar(noAppMessage) }
                    }
                },
                onAddNote = onOpenArticle,
                onOpenResources = onOpenArticle,
                onOpenArticle = onOpenArticle,
                onNavigate = onNavigate,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp), // clear the bottom nav bar
        )
    }
}