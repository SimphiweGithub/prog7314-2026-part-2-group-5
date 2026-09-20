package com.divitiae.pulsesync.data.remote.dto

/*
 * Wire models for the PulseSync REST API. Property names match the JSON keys
 * exactly, so Gson maps them without @SerializedName. All fields carry
 * defaults so a partial or evolving payload never crashes deserialisation.
 */

// ---- Authentication ----
data class GoogleSsoRequestDto(
    val firebaseIdToken: String,
    val deviceFcmToken: String? = null,
    val preferredLanguage: String = "en",
)

data class RefreshRequestDto(val refreshToken: String)

data class AuthResponseDto(
    val userId: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresIn: Long = 3600,
    val isNewUser: Boolean = false,
)

// ---- Profile & preferences ----
data class UserDto(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val preferredLanguage: String = "en",
    val biometricEnabled: Boolean = false,
)

data class UpdateProfileRequestDto(
    val displayName: String? = null,
    val photoUrl: String? = null,
)

data class PreferencesDto(
    val defaultSummaryMode: String = "DETAILED",
    val language: String = "en",
    val biometricEnabled: Boolean = false,
    val topicClusters: List<String> = emptyList(),
)

// ---- Feed & articles ----
data class FeedResponseDto(
    val articles: List<ArticleDto> = emptyList(),
    val nextCursor: String? = null,
    val serverTimestamp: Long = 0,
)

data class ArticleDto(
    val articleId: String = "",
    val headline: String = "",
    val sourceName: String = "",
    val sourceUrl: String = "",
    val author: String? = null,
    val category: String = "",
    val imageUrl: String? = null,
    val publishedAt: Long = 0,
    val readTimeMinutes: Int = 0,
    val aiSummary: AiSummaryDto = AiSummaryDto(),
    val sentiment: SentimentDto? = null,
    val derivedTags: List<String> = emptyList(),
    val extractedLinks: List<ExtractedLinkDto> = emptyList(),
)

data class AiSummaryDto(
    val detailed: List<String> = emptyList(),
    val condensed: List<String> = emptyList(),
    val model: String = "",
    val generatedAt: Long = 0,
)

data class SentimentDto(
    val score: Double = 0.0,
    val label: String = "neutral",
)

data class ExtractedLinkDto(
    val url: String = "",
    val label: String = "",
    val linkType: String = "WEB",
)

data class FullTextDto(
    val articleId: String = "",
    val sanitizedText: String = "",
    val wordCount: Int = 0,
    val sizeBytes: Int = 0,
    val retrievedAt: Long = 0,
)

// ---- Categories & keywords ----
data class CategoryDto(
    val slug: String = "",
    val nameEn: String = "",
    val nameZu: String = "",
    val nameAf: String = "",
    val isSubscribed: Boolean = false,
)

data class KeywordDto(
    val keywordId: String = "",
    val keyword: String = "",
    val notifyOnMatch: Boolean = true,
)

data class KeywordRequestDto(
    val keyword: String,
    val notifyOnMatch: Boolean = true,
)

// ---- Notes ----
data class NoteDto(
    val noteId: String = "",
    val localId: String? = null,
    val articleId: String? = null,
    val title: String = "",
    val bodyHtml: String = "",
    val plainText: String = "",
    val tags: List<String> = emptyList(),
    val isVaultNote: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

data class CreateNoteRequestDto(
    val localId: String,
    val articleId: String? = null,
    val title: String,
    val bodyHtml: String,
    val plainText: String = "",
    val tags: List<String> = emptyList(),
    val isVaultNote: Boolean = false,
    val updatedAt: Long,
)

data class UpdateNoteRequestDto(
    val title: String,
    val bodyHtml: String,
    val plainText: String = "",
    val tags: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val updatedAt: Long,
)

data class TagDto(
    val tag: String = "",
    val count: Int = 0,
)

// ---- Offline downloads ----
data class DownloadSlotsDto(
    val slotsUsed: Int = 0,
    val slotLimit: Int = 5,
    val occupied: List<OccupiedSlotDto> = emptyList(),
)

data class OccupiedSlotDto(
    val articleId: String = "",
    val sizeBytes: Int = 0,
)

data class ClaimSlotRequestDto(val articleId: String)

// ---- Synchronisation ----
data class SyncPullResponseDto(
    val articles: List<ArticleDto> = emptyList(),
    val notes: List<NoteDto> = emptyList(),
    val keywords: List<KeywordDto> = emptyList(),
    val deletedNoteIds: List<String> = emptyList(),
    val deletedKeywordIds: List<String> = emptyList(),
    val serverTimestamp: Long = 0,
)

data class SyncPushRequestDto(
    val newNotes: List<CreateNoteRequestDto> = emptyList(),
    val updatedNotes: List<NoteDto> = emptyList(),
    val deletedNoteIds: List<String> = emptyList(),
    val newKeywords: List<String> = emptyList(),
)

data class SyncedNoteDto(
    val localId: String = "",
    val serverId: String = "",
)

data class ConflictDto(
    val localId: String = "",
    val serverId: String = "",
    val resolution: String = "SERVER_WINS",
    val serverUpdatedAt: Long = 0,
)

data class SyncPushResponseDto(
    val syncedNotes: List<SyncedNoteDto> = emptyList(),
    val conflicts: List<ConflictDto> = emptyList(),
    val serverTimestamp: Long = 0,
)

// ---- Notifications ----
data class FcmTokenRequestDto(val token: String)

data class NotificationDto(
    val notificationId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "INFO",
    val articleId: String? = null,
    val matchedKeyword: String? = null,
    val isRead: Boolean = false,
    val sentAt: Long = 0,
)
