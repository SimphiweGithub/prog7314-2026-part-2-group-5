using System.Text.Json.Serialization;

namespace PulseSync.Api.Models;

// ---- Authentication DTOs ----
public record GoogleSsoRequestDto(
    [property: JsonPropertyName("firebaseIdToken")] string FirebaseIdToken,
    [property: JsonPropertyName("deviceFcmToken")] string? DeviceFcmToken = null,
    [property: JsonPropertyName("preferredLanguage")] string PreferredLanguage = "en"
);

public record RefreshRequestDto(
    [property: JsonPropertyName("refreshToken")] string RefreshToken
);

public record AuthResponseDto(
    [property: JsonPropertyName("userId")] string UserId = "",
    [property: JsonPropertyName("accessToken")] string AccessToken = "",
    [property: JsonPropertyName("refreshToken")] string RefreshToken = "",
    [property: JsonPropertyName("expiresIn")] long ExpiresIn = 3600,
    [property: JsonPropertyName("isNewUser")] bool IsNewUser = false
);

// ---- Profile & Preferences DTOs ----
public record UserDto(
    [property: JsonPropertyName("userId")] string UserId = "",
    [property: JsonPropertyName("email")] string Email = "",
    [property: JsonPropertyName("displayName")] string DisplayName = "",
    [property: JsonPropertyName("photoUrl")] string? PhotoUrl = null,
    [property: JsonPropertyName("preferredLanguage")] string PreferredLanguage = "en",
    [property: JsonPropertyName("biometricEnabled")] bool BiometricEnabled = false
);

public record UpdateProfileRequestDto(
    [property: JsonPropertyName("displayName")] string? DisplayName = null,
    [property: JsonPropertyName("photoUrl")] string? PhotoUrl = null
);

public record PreferencesDto(
    [property: JsonPropertyName("defaultSummaryMode")] string DefaultSummaryMode = "DETAILED",
    [property: JsonPropertyName("language")] string Language = "en",
    [property: JsonPropertyName("biometricEnabled")] bool BiometricEnabled = false,
    [property: JsonPropertyName("topicClusters")] List<string>? TopicClusters = null
);

// ---- Feed & Articles DTOs ----
public record FeedResponseDto(
    [property: JsonPropertyName("articles")] List<ArticleDto> Articles,
    [property: JsonPropertyName("nextCursor")] string? NextCursor = null,
    [property: JsonPropertyName("serverTimestamp")] long ServerTimestamp = 0
);

public record ArticleDto(
    [property: JsonPropertyName("articleId")] string ArticleId,
    [property: JsonPropertyName("headline")] string Headline,
    [property: JsonPropertyName("sourceName")] string SourceName,
    [property: JsonPropertyName("sourceUrl")] string SourceUrl,
    [property: JsonPropertyName("author")] string? Author,
    [property: JsonPropertyName("category")] string Category,
    [property: JsonPropertyName("imageUrl")] string? ImageUrl,
    [property: JsonPropertyName("publishedAt")] long PublishedAt,
    [property: JsonPropertyName("readTimeMinutes")] int ReadTimeMinutes,
    [property: JsonPropertyName("aiSummary")] AiSummaryDto AiSummary,
    [property: JsonPropertyName("sentiment")] SentimentDto? Sentiment,
    [property: JsonPropertyName("derivedTags")] List<string> DerivedTags,
    [property: JsonPropertyName("extractedLinks")] List<ExtractedLinkDto> ExtractedLinks
);

public record AiSummaryDto(
    [property: JsonPropertyName("detailed")] List<string> Detailed,
    [property: JsonPropertyName("condensed")] List<string> Condensed,
    [property: JsonPropertyName("model")] string Model = "gemini-2.5-flash",
    [property: JsonPropertyName("generatedAt")] long GeneratedAt = 0
);

public record SentimentDto(
    [property: JsonPropertyName("score")] double Score = 0.0,
    [property: JsonPropertyName("label")] string Label = "neutral"
);

public record ExtractedLinkDto(
    [property: JsonPropertyName("url")] string Url,
    [property: JsonPropertyName("label")] string Label,
    [property: JsonPropertyName("linkType")] string LinkType = "WEB"
);

public record FullTextDto(
    [property: JsonPropertyName("articleId")] string ArticleId,
    [property: JsonPropertyName("sanitizedText")] string SanitizedText,
    [property: JsonPropertyName("wordCount")] int WordCount,
    [property: JsonPropertyName("sizeBytes")] int SizeBytes,
    [property: JsonPropertyName("retrievedAt")] long RetrievedAt
);

// ---- Categories & Keywords DTOs ----
public record CategoryDto(
    [property: JsonPropertyName("slug")] string Slug,
    [property: JsonPropertyName("nameEn")] string NameEn,
    [property: JsonPropertyName("nameZu")] string NameZu,
    [property: JsonPropertyName("nameAf")] string NameAf,
    [property: JsonPropertyName("isSubscribed")] bool IsSubscribed = true
);

public record KeywordDto(
    [property: JsonPropertyName("keywordId")] string KeywordId,
    [property: JsonPropertyName("keyword")] string Keyword,
    [property: JsonPropertyName("notifyOnMatch")] bool NotifyOnMatch = true
);

public record KeywordRequestDto(
    [property: JsonPropertyName("keyword")] string Keyword,
    [property: JsonPropertyName("notifyOnMatch")] bool NotifyOnMatch = true
);

// ---- Notes DTOs ----
public record NoteDto(
    [property: JsonPropertyName("noteId")] string NoteId,
    [property: JsonPropertyName("localId")] string? LocalId = null,
    [property: JsonPropertyName("articleId")] string? ArticleId = null,
    [property: JsonPropertyName("title")] string Title = "",
    [property: JsonPropertyName("bodyHtml")] string BodyHtml = "",
    [property: JsonPropertyName("plainText")] string PlainText = "",
    [property: JsonPropertyName("tags")] List<string>? Tags = null,
    [property: JsonPropertyName("isVaultNote")] bool IsVaultNote = false,
    [property: JsonPropertyName("isPinned")] bool IsPinned = false,
    [property: JsonPropertyName("createdAt")] long CreatedAt = 0,
    [property: JsonPropertyName("updatedAt")] long UpdatedAt = 0
);

public record CreateNoteRequestDto(
    [property: JsonPropertyName("localId")] string LocalId,
    [property: JsonPropertyName("articleId")] string? ArticleId = null,
    [property: JsonPropertyName("title")] string Title = "",
    [property: JsonPropertyName("bodyHtml")] string BodyHtml = "",
    [property: JsonPropertyName("plainText")] string PlainText = "",
    [property: JsonPropertyName("tags")] List<string>? Tags = null,
    [property: JsonPropertyName("isVaultNote")] bool IsVaultNote = false,
    [property: JsonPropertyName("updatedAt")] long UpdatedAt = 0
);

public record UpdateNoteRequestDto(
    [property: JsonPropertyName("title")] string Title,
    [property: JsonPropertyName("bodyHtml")] string BodyHtml,
    [property: JsonPropertyName("plainText")] string PlainText = "",
    [property: JsonPropertyName("tags")] List<string>? Tags = null,
    [property: JsonPropertyName("isPinned")] bool IsPinned = false,
    [property: JsonPropertyName("updatedAt")] long UpdatedAt = 0
);

public record TagDto(
    [property: JsonPropertyName("tag")] string Tag,
    [property: JsonPropertyName("count")] int Count
);

// ---- Offline Downloads DTOs ----
public record DownloadSlotsDto(
    [property: JsonPropertyName("slotsUsed")] int SlotsUsed = 0,
    [property: JsonPropertyName("slotLimit")] int SlotLimit = 5,
    [property: JsonPropertyName("occupied")] List<OccupiedSlotDto>? Occupied = null
);

public record OccupiedSlotDto(
    [property: JsonPropertyName("articleId")] string ArticleId,
    [property: JsonPropertyName("sizeBytes")] int SizeBytes
);

public record ClaimSlotRequestDto(
    [property: JsonPropertyName("articleId")] string ArticleId
);

// ---- Synchronisation DTOs ----
public record SyncPullResponseDto(
    [property: JsonPropertyName("articles")] List<ArticleDto> Articles,
    [property: JsonPropertyName("notes")] List<NoteDto> Notes,
    [property: JsonPropertyName("keywords")] List<KeywordDto> Keywords,
    [property: JsonPropertyName("deletedNoteIds")] List<string> DeletedNoteIds,
    [property: JsonPropertyName("deletedKeywordIds")] List<string> DeletedKeywordIds,
    [property: JsonPropertyName("serverTimestamp")] long ServerTimestamp
);

public record SyncPushRequestDto(
    [property: JsonPropertyName("newNotes")] List<CreateNoteRequestDto> NewNotes,
    [property: JsonPropertyName("updatedNotes")] List<NoteDto> UpdatedNotes,
    [property: JsonPropertyName("deletedNoteIds")] List<string> DeletedNoteIds,
    [property: JsonPropertyName("newKeywords")] List<string> NewKeywords
);

public record SyncedNoteDto(
    [property: JsonPropertyName("localId")] string LocalId,
    [property: JsonPropertyName("serverId")] string ServerId
);

public record ConflictDto(
    [property: JsonPropertyName("localId")] string LocalId,
    [property: JsonPropertyName("serverId")] string ServerId,
    [property: JsonPropertyName("resolution")] string Resolution = "SERVER_WINS",
    [property: JsonPropertyName("serverUpdatedAt")] long ServerUpdatedAt = 0
);

public record SyncPushResponseDto(
    [property: JsonPropertyName("syncedNotes")] List<SyncedNoteDto> SyncedNotes,
    [property: JsonPropertyName("conflicts")] List<ConflictDto> Conflicts,
    [property: JsonPropertyName("serverTimestamp")] long ServerTimestamp
);

// ---- Notification DTOs ----
public record FcmTokenRequestDto(
    [property: JsonPropertyName("token")] string Token
);

public record NotificationDto(
    [property: JsonPropertyName("notificationId")] string NotificationId,
    [property: JsonPropertyName("title")] string Title,
    [property: JsonPropertyName("body")] string Body,
    [property: JsonPropertyName("type")] string Type = "INFO",
    [property: JsonPropertyName("articleId")] string? ArticleId = null,
    [property: JsonPropertyName("matchedKeyword")] string? MatchedKeyword = null,
    [property: JsonPropertyName("isRead")] bool IsRead = false,
    [property: JsonPropertyName("sentAt")] long SentAt = 0
);
