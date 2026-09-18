package com.divitiae.pulsesync.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "articles",
    indices = [Index("category"), Index("publishedAt")],
)
data class ArticleEntity(
    @PrimaryKey val articleId: String,
    val headline: String,
    val sourceName: String,
    val sourceUrl: String,
    val author: String?,
    val category: String,
    val imageUrl: String?,
    val publishedAt: Long,
    val readTimeMinutes: Int,
    val summaryDetailed: List<String>,
    val summaryCondensed: List<String>,
    val summaryModel: String,
    /** [com.divitiae.pulsesync.data.domain.SentimentType] name. */
    val sentiment: String,
    val derivedTags: List<String>,
    val isBookmarked: Boolean = false,
    val isDownloaded: Boolean = false,
    val cachedAt: Long,
)

@Entity(
    tableName = "extracted_links",
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["articleId"],
            childColumns = ["articleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("articleId"), Index("linkType")],
)
data class ExtractedLinkEntity(
    @PrimaryKey val linkId: String,
    val articleId: String,
    val url: String,
    val label: String,
    /** [com.divitiae.pulsesync.data.domain.ResourceKind] name. */
    val linkType: String,
)

@Entity(
    tableName = "article_full_text",
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["articleId"],
            childColumns = ["articleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ArticleFullTextEntity(
    @PrimaryKey val articleId: String,
    val sanitizedText: String,
    val wordCount: Int,
    val sizeBytes: Int,
    val downloadedAt: Long,
)

/** An article joined with its extracted links, assembled by Room. */
data class ArticleWithLinks(
    @Embedded val article: ArticleEntity,
    @Relation(parentColumn = "articleId", entityColumn = "articleId")
    val links: List<ExtractedLinkEntity>,
)

@Entity(
    tableName = "notes",
    indices = [Index("articleId"), Index("isVaultNote"), Index("updatedAt")],
)
data class NoteEntity(
    @PrimaryKey val localId: String,
    val serverId: String?,
    val userId: String,
    val articleId: String?,
    val title: String,
    val bodyHtml: String,
    val plainText: String,
    val tags: List<String>,
    val isVaultNote: Boolean = false,
    val isPinned: Boolean = false,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "keywords")
data class TrackedKeywordEntity(
    @PrimaryKey val keywordId: String,
    val keyword: String,
    val notifyOnMatch: Boolean = true,
    val isSynced: Boolean = false,
    val createdAt: Long,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val slug: String,
    val nameEn: String,
    val nameZu: String,
    val nameAf: String,
    val isSubscribed: Boolean = false,
)

@Entity(tableName = "notifications", indices = [Index("receivedAt")])
data class NotificationEntity(
    @PrimaryKey val notificationId: String,
    val title: String,
    val body: String,
    val type: String,
    val articleId: String?,
    val matchedKeyword: String?,
    val isRead: Boolean = false,
    val receivedAt: Long,
)

@Entity(tableName = "download_slots")
data class DownloadSlotEntity(
    @PrimaryKey val articleId: String,
    val sizeBytes: Int,
    val downloadedAt: Long,
)

/** Ordered log of local mutations awaiting push to the server. */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val queueId: Long = 0,
    val entityType: String,
    val entityLocalId: String,
    val operation: String,
    val attemptCount: Int = 0,
    val queuedAt: Long,
)

/** Singleton row (id is always 1) holding sync state. */
@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey val id: Int = 1,
    val lastSyncTimestamp: Long = 0,
    val pendingCount: Int = 0,
    val lastSyncStatus: String = "NEVER",
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?,
    val preferredLanguage: String = "en",
    val biometricEnabled: Boolean = false,
    val lastLoginAt: Long,
)
