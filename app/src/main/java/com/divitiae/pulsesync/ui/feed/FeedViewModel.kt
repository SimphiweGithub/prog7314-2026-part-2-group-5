package com.divitiae.pulsesync.ui.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.divitiae.pulsesync.data.domain.AppError
import com.divitiae.pulsesync.data.domain.Article
import com.divitiae.pulsesync.data.domain.Category
import com.divitiae.pulsesync.data.domain.DownloadSlot
import com.divitiae.pulsesync.data.domain.Result
import com.divitiae.pulsesync.data.domain.SummaryModePref
import com.divitiae.pulsesync.data.domain.UserPreferences
import com.divitiae.pulsesync.data.repository.ArticleRepository
import com.divitiae.pulsesync.data.repository.CategoryRepository
import com.divitiae.pulsesync.data.repository.DownloadRepository
import com.divitiae.pulsesync.data.repository.PreferencesRepository
import com.divitiae.pulsesync.ui.common.UiState
import com.divitiae.pulsesync.ui.common.toUiState
import com.divitiae.pulsesync.ui.mapper.toUi
import com.divitiae.pulsesync.ui.viewmodel.containerViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Backs Member 2's stateless [FeedContent] with real repository data.
 *
 * Offline-first: the list comes from Room via [ArticleRepository.observeFeed]
 * and is never blocked on the network. [refresh] hits the API and only
 * surfaces a Snackbar message on failure — the cached list stays on screen.
 *
 * User Defined Feature 1 (DualMode toggle) is held here as an in-memory map
 * of article id → [SummaryMode]; articles with no entry fall back to the
 * user's default from Settings. No persistence, no network — exactly what the
 * spec asks for.
 */
class FeedViewModel(
    private val articleRepository: ArticleRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    init {
        Log.d(TAG, "FeedViewModel initialized")
    }

    /** Ephemeral, UI-only controls kept in one flow so `combine` stays at 5 sources. */
    private data class Controls(
        val selectedCategory: String? = FeedSampleData.GENERAL_FEED,
        val isRefreshing: Boolean = false,
        /** Per-article override of the default summary mode (User Defined 1). */
        val summaryOverrides: Map<String, SummaryMode> = emptyMap(),
    )

    private val controls = MutableStateFlow(Controls())

    private val _refreshError = MutableStateFlow<UiState.Error?>(null)
    /** Set when a pull-to-refresh fails; the route shows a Snackbar and calls [consumeRefreshError]. */
    val refreshError: StateFlow<UiState.Error?> = _refreshError.asStateFlow()

    val uiState: StateFlow<UiState<FeedUiState>> = combine(
        articleRepository.observeFeed(categorySlug = null),
        categoryRepository.observeAll(),
        preferencesRepository.preferences,
        downloadRepository.observeSlots(),
        controls,
        ::buildState,
    )
        .catch { e -> emit(AppError.Unknown(e.message, e).toUiState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private fun buildState(
        articles: List<Article>,
        categories: List<Category>,
        prefs: UserPreferences,
        slots: List<DownloadSlot>,
        ctl: Controls,
    ): UiState<FeedUiState> {
        val defaultMode = prefs.defaultSummaryMode.toUi()
        val articleUis = articles.map { it.toUi(offlineSlotsUsed = slots.size) }
        return UiState.Success(
            FeedUiState(
                // Chip labels must equal ArticleUi.category (slug → "Load Shedding"), which
                // nameEn does for the seeded catalogue.
                categories = listOf(FeedSampleData.GENERAL_FEED) + categories.map { it.nameEn },
                selectedCategory = ctl.selectedCategory,
                articles = articleUis,
                isLoading = false,
                isRefreshing = ctl.isRefreshing,
                // Materialise the effective mode for every article so FeedContent's
                // summaryModeFor() honours the Settings default, not just DETAILED.
                summaryModes = articleUis.associate { a ->
                    a.id to (ctl.summaryOverrides[a.id] ?: defaultMode)
                },
            ),
        )
    }

    init {
        // Warm the cache from the API once on first open; failures are silent
        // here because the seeded/cached list is already visible.
        viewModelScope.launch { articleRepository.refresh() }
    }

    fun selectCategory(label: String) = controls.update { it.copy(selectedCategory = label) }

    /** User Defined Feature 1: flip this article between detailed and condensed, in memory only. */
    fun toggleSummaryMode(articleId: String) {
        val current = uiState.value.let { (it as? UiState.Success)?.data?.summaryModeFor(articleId) }
            ?: SummaryMode.DETAILED
        controls.update { it.copy(summaryOverrides = it.summaryOverrides + (articleId to current.toggled())) }
    }

    fun toggleSave(articleId: String) {
        val article = (uiState.value as? UiState.Success)?.data?.articles?.firstOrNull { it.id == articleId }
            ?: return
        viewModelScope.launch { articleRepository.setBookmarked(articleId, !article.isSaved) }
    }

    fun refresh() {
        if (controls.value.isRefreshing) return
        controls.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            val result = articleRepository.refresh()
            controls.update { it.copy(isRefreshing = false) }
            if (result is Result.Failure) _refreshError.value = result.error.toUiState()
        }
    }

    fun consumeRefreshError() {
        _refreshError.value = null
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "FeedViewModel onCleared")
    }

    companion object {
        private const val TAG = "FeedViewModel"
        val Factory: ViewModelProvider.Factory = containerViewModelFactory { c ->
            FeedViewModel(
                articleRepository = c.articleRepository,
                categoryRepository = c.categoryRepository,
                preferencesRepository = c.preferencesRepository,
                downloadRepository = c.downloadRepository,
            )
        }
    }
}

/** Settings preference → UI enum. */
fun SummaryModePref.toUi(): SummaryMode = when (this) {
    SummaryModePref.DETAILED -> SummaryMode.DETAILED
    SummaryModePref.CONDENSED -> SummaryMode.CONDENSED
}