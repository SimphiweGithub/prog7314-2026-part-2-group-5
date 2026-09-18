package com.divitiae.pulsesync.data.mapper

import com.divitiae.pulsesync.data.domain.AiSummary
import com.divitiae.pulsesync.data.domain.Article
import com.divitiae.pulsesync.data.domain.ArticleFullText
import com.divitiae.pulsesync.data.domain.Category
import com.divitiae.pulsesync.data.domain.DownloadSlot
import com.divitiae.pulsesync.data.domain.Keyword
import com.divitiae.pulsesync.data.domain.Note
import com.divitiae.pulsesync.data.domain.NotificationItem
import com.divitiae.pulsesync.data.domain.ResourceKind
import com.divitiae.pulsesync.data.domain.ResourceLink
import com.divitiae.pulsesync.data.domain.SentimentType
import com.divitiae.pulsesync.data.domain.UserProfile
import com.divitiae.pulsesync.data.local.entity.ArticleEntity
import com.divitiae.pulsesync.data.local.entity.ArticleFullTextEntity
import com.divitiae.pulsesync.data.local.entity.ArticleWithLinks
import com.divitiae.pulsesync.data.local.entity.CategoryEntity
import com.divitiae.pulsesync.data.local.entity.DownloadSlotEntity
import com.divitiae.pulsesync.data.local.entity.ExtractedLinkEntity
import com.divitiae.pulsesync.data.local.entity.NoteEntity
import com.divitiae.pulsesync.data.local.entity.NotificationEntity
import com.divitiae.pulsesync.data.local.entity.TrackedKeywordEntity
import com.divitiae.pulsesync.data.local.entity.UserEntity

private fun String.toSentimentTypeOrNeutral(): SentimentType =
    runCatching { SentimentType.valueOf(this) }.getOrDefault(SentimentType.NEUTRAL)

private fun String.toResourceKindOrWeb(): ResourceKind =
    runCatching { ResourceKind.valueOf(this) }.getOrDefault(ResourceKind.WEB)

// ---- Article ----

fun ArticleWithLinks.toDomain(): Article = Article(
    id = article.articleId,
    headline = article.headline,
    sourceName = article.sourceName,
    sourceUrl = article.sourceUrl,
    author = article.author,
    category = article.category,
    imageUrl = article.imageUrl,
    publishedAt = article.publishedAt,
    readTimeMinutes = article.readTimeMinutes,
    summary = AiSummary(article.summaryDetailed, article.summaryCondensed, article.summaryModel),
    sentiment = article.sentiment.toSentimentTypeOrNeutral(),
    tags = article.derivedTags,
    resources = links.map {
        ResourceLink(url = it.url, label = it.label, kind = it.linkType.toResourceKindOrWeb())
    },
    isBookmarked = article.isBookmarked,
    isDownloaded = article.isDownloaded,
)

fun Article.toEntity(cachedAt: Long): ArticleEntity = ArticleEntity(
    articleId = id,
    headline = headline,
    sourceName = sourceName,
    sourceUrl = sourceUrl,
    author = author,
    category = category,
    imageUrl = imageUrl,
    publishedAt = publishedAt,
    readTimeMinutes = readTimeMinutes,
    summaryDetailed = summary.detailed,
    summaryCondensed = summary.condensed,
    summaryModel = summary.model,
    sentiment = sentiment.name,
    derivedTags = tags,
    isBookmarked = isBookmarked,
    isDownloaded = isDownloaded,
    cachedAt = cachedAt,
)

fun Article.toLinkEntities(): List<ExtractedLinkEntity> = resources.mapIndexed { index, link ->
    ExtractedLinkEntity(
        linkId = "$id-$index",
        articleId = id,
        url = link.url,
        label = link.label,
        linkType = link.kind.name,
    )
}

fun ArticleFullTextEntity.toDomain(): ArticleFullText = ArticleFullText(
    articleId = articleId,
    sanitizedText = sanitizedText,
    wordCount = wordCount,
    sizeBytes = sizeBytes,
)

// ---- Note ----

fun NoteEntity.toDomain(): Note = Note(
    localId = localId,
    serverId = serverId,
    articleId = articleId,
    title = title,
    bodyHtml = bodyHtml,
    plainText = plainText,
    tags = tags,
    isVaultNote = isVaultNote,
    isPinned = isPinned,
    isSynced = isSynced,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Note.toEntity(userId: String): NoteEntity = NoteEntity(
    localId = localId,
    serverId = serverId,
    userId = userId,
    articleId = articleId,
    title = title,
    bodyHtml = bodyHtml,
    plainText = plainText,
    tags = tags,
    isVaultNote = isVaultNote,
    isPinned = isPinned,
    isSynced = isSynced,
    isDeleted = false,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ---- Keyword ----

fun TrackedKeywordEntity.toDomain(): Keyword =
    Keyword(id = keywordId, keyword = keyword, notifyOnMatch = notifyOnMatch)

fun Keyword.toEntity(createdAt: Long, isSynced: Boolean): TrackedKeywordEntity =
    TrackedKeywordEntity(
        keywordId = id,
        keyword = keyword,
        notifyOnMatch = notifyOnMatch,
        isSynced = isSynced,
        createdAt = createdAt,
    )

// ---- Category ----

fun CategoryEntity.toDomain(): Category =
    Category(slug = slug, nameEn = nameEn, nameZu = nameZu, nameAf = nameAf, isSubscribed = isSubscribed)

fun Category.toEntity(): CategoryEntity =
    CategoryEntity(slug = slug, nameEn = nameEn, nameZu = nameZu, nameAf = nameAf, isSubscribed = isSubscribed)

// ---- Notification ----

fun NotificationEntity.toDomain(): NotificationItem = NotificationItem(
    id = notificationId,
    title = title,
    body = body,
    type = type,
    articleId = articleId,
    matchedKeyword = matchedKeyword,
    isRead = isRead,
    receivedAt = receivedAt,
)

fun NotificationItem.toEntity(): NotificationEntity = NotificationEntity(
    notificationId = id,
    title = title,
    body = body,
    type = type,
    articleId = articleId,
    matchedKeyword = matchedKeyword,
    isRead = isRead,
    receivedAt = receivedAt,
)

// ---- Download slot ----

fun DownloadSlotEntity.toDomain(): DownloadSlot =
    DownloadSlot(articleId = articleId, sizeBytes = sizeBytes, downloadedAt = downloadedAt)

// ---- User ----

fun UserEntity.toDomain(): UserProfile = UserProfile(
    userId = userId,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl,
    preferredLanguage = preferredLanguage,
    biometricEnabled = biometricEnabled,
)

fun UserProfile.toEntity(lastLoginAt: Long): UserEntity = UserEntity(
    userId = userId,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl,
    preferredLanguage = preferredLanguage,
    biometricEnabled = biometricEnabled,
    lastLoginAt = lastLoginAt,
)
