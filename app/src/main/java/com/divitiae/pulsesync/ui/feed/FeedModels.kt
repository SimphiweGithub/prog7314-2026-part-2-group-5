package com.divitiae.pulsesync.ui.feed

/**
 * UI-facing models for the Feed Dashboard. These describe exactly what the
 * screen renders; Member 4's ViewModel maps the API/domain models onto them.
 */
data class ArticleUi(
    val id: String,
    val source: String,
    val category: String,
    val timeAgo: String,
    val title: String,
    /** Paragraphs shown in [SummaryMode.DETAILED]. */
    val detailedSummary: List<String>,
    /** Exactly three bullets shown in [SummaryMode.CONDENSED]. */
    val condensedSummary: List<String>,
    val sentiment: Sentiment,
    val keywords: List<String>,
    /** "extracted / total" resource links, rendered as e.g. "3/5". */
    val resourcesExtracted: Int,
    val resourcesTotal: Int,
    val isSaved: Boolean = false,
)

enum class Sentiment { POSITIVE, NEUTRAL, NEGATIVE }

/** User Defined Feature 1: DualMode AI Summary Toggle. */
enum class SummaryMode {
    DETAILED,
    CONDENSED;

    fun toggled(): SummaryMode = if (this == DETAILED) CONDENSED else DETAILED
}

data class FeedUiState(
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val articles: List<ArticleUi> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    /** Per-article summary mode; articles not present use [SummaryMode.DETAILED]. */
    val summaryModes: Map<String, SummaryMode> = emptyMap(),
) {
    fun summaryModeFor(articleId: String): SummaryMode =
        summaryModes[articleId] ?: SummaryMode.DETAILED
}
