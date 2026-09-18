package com.divitiae.pulsesync.data.domain

/**
 * A news article after the backend has summarised, scored and link-extracted
 * it. This is the shape repositories expose; the raw article body used for
 * offline reading is fetched separately (see [ArticleFullText]).
 */
data class Article(
    val id: String,
    val headline: String,
    val sourceName: String,
    val sourceUrl: String,
    val author: String?,
    val category: String,
    val imageUrl: String?,
    /** Unix epoch millis. */
    val publishedAt: Long,
    val readTimeMinutes: Int,
    val summary: AiSummary,
    val sentiment: SentimentType,
    val tags: List<String>,
    val resources: List<ResourceLink>,
    val isBookmarked: Boolean = false,
    val isDownloaded: Boolean = false,
)

/** Both AI summary formats, so the dual-mode toggle works offline. */
data class AiSummary(
    val detailed: List<String>,
    val condensed: List<String>,
    val model: String = "",
)

enum class SentimentType { POSITIVE, NEUTRAL, NEGATIVE }

/** An actionable link parsed out of the source article. */
data class ResourceLink(
    val url: String,
    val label: String,
    val kind: ResourceKind,
)

/**
 * Matches the four kinds the UI renders. The backend's richer classification
 * (application portals, bursaries, PDF forms, secondary sources) is folded
 * into these when the DTO is mapped to the domain model.
 */
enum class ResourceKind { PDF, PORTAL, WEB, VIDEO }

/** Sanitised full article body, held only for downloaded (offline) articles. */
data class ArticleFullText(
    val articleId: String,
    val sanitizedText: String,
    val wordCount: Int,
    val sizeBytes: Int,
)
