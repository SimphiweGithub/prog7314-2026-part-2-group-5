package com.divitiae.pulsesync.data.domain

/** A keyword the user has starred; the backend watches these for push alerts. */
data class Keyword(
    val id: String,
    val keyword: String,
    val notifyOnMatch: Boolean = true,
)

/** A topic cluster, with all three localised names cached for offline use. */
data class Category(
    val slug: String,
    val nameEn: String,
    val nameZu: String,
    val nameAf: String,
    val isSubscribed: Boolean = false,
) {
    /** Resolves the display name for a locale tag such as "en", "zu" or "af". */
    fun localisedName(languageTag: String): String =
        when (languageTag.lowercase().take(2)) {
            "zu" -> nameZu
            "af" -> nameAf
            else -> nameEn
        }
}

/** A delivered push notification, backing the in-app alerts screen. */
data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    val articleId: String? = null,
    val matchedKeyword: String? = null,
    val isRead: Boolean = false,
    /** Unix epoch millis. */
    val receivedAt: Long,
)

/** An occupied offline-download slot (the app caps these at five). */
data class DownloadSlot(
    val articleId: String,
    val sizeBytes: Int,
    val downloadedAt: Long,
)
