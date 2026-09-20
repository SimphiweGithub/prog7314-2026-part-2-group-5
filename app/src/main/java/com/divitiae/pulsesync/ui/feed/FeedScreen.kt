package com.divitiae.pulsesync.ui.feed

/*
 * ---------------------------------------------------------------------
 * CODE ATTRIBUTION
 * ---------------------------------------------------------------------
 * The LazyColumn with keyed items, PullToRefreshBox, LaunchedEffect side effects, Scaffold and preview annotations in this file were adapted from:
 *
 * Android Developers (2026) Lazy lists and lazy grids. [online]
 * Available at: https://developer.android.com/develop/ui/compose/lists
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) androidx.compose.material3.pulltorefresh. [online]
 * Available at: https://developer.android.com/reference/kotlin/androidx/compose/material3/pulltorefresh/package-summary
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Side-effects in Compose. [online]
 * Available at: https://developer.android.com/develop/ui/compose/side-effects
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Scaffold. [online]
 * Available at: https://developer.android.com/develop/ui/compose/components/scaffold
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Preview your UI with composable previews. [online]
 * Available at: https://developer.android.com/develop/ui/compose/tooling/previews
 * [Accessed 20 September 2026].
 * ---------------------------------------------------------------------
 */

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.divitiae.pulsesync.ui.components.BottomDestination
import com.divitiae.pulsesync.ui.components.PulseSyncBottomBar
import com.divitiae.pulsesync.ui.components.PulseSyncDimens
import com.divitiae.pulsesync.ui.theme.PulseSyncTheme
import kotlinx.coroutines.delay

/**
 * Feed Dashboard (Figma 1:15, condensed 15:35, dark 29:68).
 *
 * Holds prototype state locally with [FeedSampleData] so the screen runs on
 * its own. Member 4 swaps this for a ViewModel-backed [FeedUiState] and the
 * stateless [FeedContent] stays untouched.
 */
@Composable
fun FeedScreen(
    onOpenArticle: (ArticleUi) -> Unit,
    onNavigate: (BottomDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember { mutableStateOf(FeedSampleData.initialState().copy(isLoading = true)) }
    var query by rememberSaveable { mutableStateOf("") }

    // Simulated initial load so the skeleton state is visible in the prototype.
    // Adapted from: Android Developers (2026) Side-effects in Compose - LaunchedEffect. https://developer.android.com/develop/ui/compose/side-effects
    LaunchedEffect(Unit) {
        delay(900)
        state = state.copy(isLoading = false)
    }
    LaunchedEffect(state.isRefreshing) {
        if (state.isRefreshing) {
            delay(1200)
            state = state.copy(isRefreshing = false)
        }
    }

    FeedContent(
        state = state,
        query = query,
        onQueryChange = { query = it },
        onSelectCategory = { state = state.copy(selectedCategory = it) },
        onRefresh = { state = state.copy(isRefreshing = true) },
        onToggleSummary = { id ->
            val next = state.summaryModeFor(id).toggled()
            state = state.copy(summaryModes = state.summaryModes + (id to next))
        },
        onToggleSave = { id ->
            state = state.copy(
                articles = state.articles.map { if (it.id == id) it.copy(isSaved = !it.isSaved) else it },
            )
        },
        onOpenSource = { /* TODO(Member 2, article commit): ACTION_VIEW intent */ },
        onAddNote = onOpenArticle,
        onOpenResources = onOpenArticle,
        onOpenArticle = onOpenArticle,
        onNavigate = onNavigate,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedContent(
    state: FeedUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onRefresh: () -> Unit,
    onToggleSummary: (articleId: String) -> Unit,
    onToggleSave: (articleId: String) -> Unit,
    onOpenSource: (ArticleUi) -> Unit,
    onAddNote: (ArticleUi) -> Unit,
    onOpenResources: (ArticleUi) -> Unit,
    onOpenArticle: (ArticleUi) -> Unit,
    onNavigate: (BottomDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleArticles = remember(state.articles, state.selectedCategory, query) {
        state.articles
            .filter { article ->
                state.selectedCategory == null ||
                    state.selectedCategory == FeedSampleData.GENERAL_FEED ||
                    article.category == state.selectedCategory
            }
            .filter { article ->
                query.isBlank() ||
                    article.title.contains(query, ignoreCase = true) ||
                    article.keywords.any { it.contains(query.trimStart('#'), ignoreCase = true) }
            }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                FeedTopBar(query = query, onQueryChange = onQueryChange)
                CategoryStrip(
                    categories = state.categories,
                    selected = state.selectedCategory,
                    onSelect = onSelectCategory,
                )
            }
        },
        bottomBar = {
            PulseSyncBottomBar(selected = BottomDestination.FEED, onSelect = onNavigate)
        },
    ) { innerPadding ->
        // Adapted from: Android Developers (2026) androidx.compose.material3.pulltorefresh. https://developer.android.com/reference/kotlin/androidx/compose/material3/pulltorefresh/package-summary
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = PulseSyncDimens.ScreenPadding, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when {
                    state.isLoading -> items(count = 3, key = { "skeleton-$it" }) {
                        ArticleCardSkeleton()
                    }
                    visibleArticles.isEmpty() -> item(key = "empty") {
                        FeedEmptyState(modifier = Modifier.fillMaxWidth())
                    }
                    else -> items(visibleArticles, key = { it.id }) { article ->
                        ArticleCard(
                            article = article,
                            summaryMode = state.summaryModeFor(article.id),
                            onToggleSummary = { onToggleSummary(article.id) },
                            onOpenSource = { onOpenSource(article) },
                            onAddNote = { onAddNote(article) },
                            onOpenResources = { onOpenResources(article) },
                            onToggleSave = { onToggleSave(article.id) },
                            onOpenArticle = { onOpenArticle(article) },
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Feed · Light", showBackground = true, widthDp = 360, heightDp = 800)
@Preview(
    name = "Feed · Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun FeedPreview() {
    PulseSyncTheme { FeedPreviewContent(FeedSampleData.initialState()) }
}

@Preview(name = "Feed · Condensed", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun FeedCondensedPreview() {
    val base = FeedSampleData.initialState()
    PulseSyncTheme {
        FeedPreviewContent(
            base.copy(summaryModes = base.articles.associate { it.id to SummaryMode.CONDENSED }),
        )
    }
}

@Preview(name = "Feed · Loading", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun FeedLoadingPreview() {
    PulseSyncTheme { FeedPreviewContent(FeedSampleData.initialState().copy(isLoading = true)) }
}

@Preview(name = "Feed · Empty", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun FeedEmptyPreview() {
    PulseSyncTheme { FeedPreviewContent(FeedSampleData.initialState().copy(articles = emptyList())) }
}

@Composable
private fun FeedPreviewContent(state: FeedUiState) {
    FeedContent(
        state = state,
        query = "",
        onQueryChange = {},
        onSelectCategory = {},
        onRefresh = {},
        onToggleSummary = {},
        onToggleSave = {},
        onOpenSource = {},
        onAddNote = {},
        onOpenResources = {},
        onOpenArticle = {},
        onNavigate = {},
    )
}
