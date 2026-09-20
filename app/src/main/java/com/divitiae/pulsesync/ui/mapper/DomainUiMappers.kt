package com.divitiae.pulsesync.ui.mapper

import com.divitiae.pulsesync.data.domain.Article
import com.divitiae.pulsesync.data.domain.Note
import com.divitiae.pulsesync.data.domain.ResourceKind
import com.divitiae.pulsesync.data.domain.ResourceLink
import com.divitiae.pulsesync.data.domain.SentimentType
import com.divitiae.pulsesync.ui.feed.ArticleUi
import com.divitiae.pulsesync.ui.feed.NoteSyncState
import com.divitiae.pulsesync.ui.feed.NoteUi
import com.divitiae.pulsesync.ui.feed.ResourceLinkUi
import com.divitiae.pulsesync.ui.feed.ResourceType
import com.divitiae.pulsesync.ui.feed.Sentiment
import java.util.concurrent.TimeUnit

/**
 * Bridges domain models to the UI models the screens already render. Kept in
 * the ui layer (which may depend on data), so the data layer stays UI-agnostic.
 * Member 4's ViewModels call these when exposing repository data to Compose.
 */

fun SentimentType.toUi(): Sentiment = when (this) {
    SentimentType.POSITIVE -> Sentiment.POSITIVE
    SentimentType.NEUTRAL -> Sentiment.NEUTRAL
    SentimentType.NEGATIVE -> Sentiment.NEGATIVE
}

fun ResourceKind.toUi(): ResourceType = when (this) {
    ResourceKind.PDF -> ResourceType.PDF
    ResourceKind.PORTAL -> ResourceType.PORTAL
    ResourceKind.WEB -> ResourceType.WEB
    ResourceKind.VIDEO -> ResourceType.VIDEO
}

fun ResourceLink.toUi(): ResourceLinkUi =
    ResourceLinkUi(title = label, url = url, type = kind.toUi())

fun Note.toUi(): NoteUi = NoteUi(
    text = plainText.ifBlank { bodyHtml },
    tag = tags.firstOrNull(),
    syncState = if (isSynced) NoteSyncState.SYNCED else NoteSyncState.LOCAL,
)

fun Article.toUi(
    offlineSlotsUsed: Int = 0,
    offlineSlotsTotal: Int = 5,
    note: NoteUi? = null,
): ArticleUi = ArticleUi(
    id = id,
    source = sourceName,
    category = category.toCategoryLabel(),
    timeAgo = publishedAt.toRelativeTime(),
    title = headline,
    sourceUrl = sourceUrl,
    body = summary.detailed,
    detailedSummary = summary.detailed,
    condensedSummary = summary.condensed,
    sentiment = sentiment.toUi(),
    keywords = tags,
    resources = resources.map { it.toUi() },
    offlineSlotsUsed = offlineSlotsUsed,
    offlineSlotsTotal = offlineSlotsTotal,
    isSaved = isBookmarked,
    note = note,
)

/** "bursaries" -> "Bursaries", "load-shedding" -> "Load Shedding". */
private fun String.toCategoryLabel(): String =
    split('-', ' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

private fun Long.toRelativeTime(): String {
    if (this <= 0L) return ""
    val diff = System.currentTimeMillis() - this
    if (diff < 0) return "just now"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}
