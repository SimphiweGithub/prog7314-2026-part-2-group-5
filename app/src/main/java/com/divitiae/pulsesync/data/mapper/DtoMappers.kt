package com.divitiae.pulsesync.data.mapper

import com.divitiae.pulsesync.data.domain.AiSummary
import com.divitiae.pulsesync.data.domain.Article
import com.divitiae.pulsesync.data.domain.ArticleFullText
import com.divitiae.pulsesync.data.domain.Category
import com.divitiae.pulsesync.data.domain.Keyword
import com.divitiae.pulsesync.data.domain.Note
import com.divitiae.pulsesync.data.domain.NotificationItem
import com.divitiae.pulsesync.data.domain.ResourceKind
import com.divitiae.pulsesync.data.domain.ResourceLink
import com.divitiae.pulsesync.data.domain.SentimentType
import com.divitiae.pulsesync.data.domain.UserProfile
import com.divitiae.pulsesync.data.remote.dto.ArticleDto
import com.divitiae.pulsesync.data.remote.dto.CategoryDto
import com.divitiae.pulsesync.data.remote.dto.CreateNoteRequestDto
import com.divitiae.pulsesync.data.remote.dto.ExtractedLinkDto
import com.divitiae.pulsesync.data.remote.dto.FullTextDto
import com.divitiae.pulsesync.data.remote.dto.KeywordDto
import com.divitiae.pulsesync.data.remote.dto.NoteDto
import com.divitiae.pulsesync.data.remote.dto.NotificationDto
import com.divitiae.pulsesync.data.remote.dto.UpdateNoteRequestDto
import com.divitiae.pulsesync.data.remote.dto.UserDto

/** Maps a server sentiment label onto the domain enum. */
fun String?.toSentimentType(): SentimentType = when (this?.lowercase()) {
    "positive" -> SentimentType.POSITIVE
    "negative" -> SentimentType.NEGATIVE
    else -> SentimentType.NEUTRAL
}

/** Folds the backend's richer link classification into the four UI kinds. */
fun String?.toResourceKind(): ResourceKind = when (this?.uppercase()) {
    "PDF", "PDF_FORM" -> ResourceKind.PDF
    "PORTAL", "APPLICATION_PORTAL", "BURSARY" -> ResourceKind.PORTAL
    "VIDEO" -> ResourceKind.VIDEO
    else -> ResourceKind.WEB
}

fun ExtractedLinkDto.toDomain(): ResourceLink =
    ResourceLink(url = url, label = label, kind = linkType.toResourceKind())

fun ArticleDto.toDomain(): Article = Article(
    id = articleId,
    headline = headline,
    sourceName = sourceName,
    sourceUrl = sourceUrl,
    author = author,
    category = category,
    imageUrl = imageUrl,
    publishedAt = publishedAt,
    readTimeMinutes = readTimeMinutes,
    summary = AiSummary(aiSummary.detailed, aiSummary.condensed, aiSummary.model),
    sentiment = sentiment?.label.toSentimentType(),
    tags = derivedTags,
    resources = extractedLinks.map { it.toDomain() },
)

fun NoteDto.toDomain(): Note = Note(
    localId = localId ?: noteId,
    serverId = noteId.ifBlank { null },
    articleId = articleId,
    title = title,
    bodyHtml = bodyHtml,
    plainText = plainText,
    tags = tags,
    isVaultNote = isVaultNote,
    isPinned = isPinned,
    isSynced = true,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun KeywordDto.toDomain(): Keyword =
    Keyword(id = keywordId, keyword = keyword, notifyOnMatch = notifyOnMatch)

fun CategoryDto.toDomain(): Category =
    Category(slug = slug, nameEn = nameEn, nameZu = nameZu, nameAf = nameAf, isSubscribed = isSubscribed)

fun NotificationDto.toDomain(): NotificationItem = NotificationItem(
    id = notificationId,
    title = title,
    body = body,
    type = type,
    articleId = articleId,
    matchedKeyword = matchedKeyword,
    isRead = isRead,
    receivedAt = sentAt,
)

fun UserDto.toDomain(): UserProfile = UserProfile(
    userId = userId,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl,
    preferredLanguage = preferredLanguage,
    biometricEnabled = biometricEnabled,
)

fun FullTextDto.toDomain(): ArticleFullText = ArticleFullText(
    articleId = articleId,
    sanitizedText = sanitizedText,
    wordCount = wordCount,
    sizeBytes = sizeBytes,
)

// ---- domain -> request bodies ----

fun Note.toCreateRequest(): CreateNoteRequestDto = CreateNoteRequestDto(
    localId = localId,
    articleId = articleId,
    title = title,
    bodyHtml = bodyHtml,
    plainText = plainText,
    tags = tags,
    isVaultNote = isVaultNote,
    updatedAt = updatedAt,
)

fun Note.toUpdateRequest(): UpdateNoteRequestDto = UpdateNoteRequestDto(
    title = title,
    bodyHtml = bodyHtml,
    plainText = plainText,
    tags = tags,
    isPinned = isPinned,
    updatedAt = updatedAt,
)
