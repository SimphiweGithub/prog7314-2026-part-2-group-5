/*
 * ---------------------------------------------------------------------
 * CODE ATTRIBUTION
 * ---------------------------------------------------------------------
 * Author: Simphiwe Khumalo (ST10451674) - Member 3: Back-End & Data Architect
 * Assisted by: Antigravity AI Coding Assistant (Google DeepMind)
 *
 * The Google Cloud Firestore NoSQL repository implementation, subcollection document
 * tree mapping, and asynchronous Firestore writes in this file were adapted from:
 *
 * Google Firebase (2024) Cloud Firestore: Add data with the Cloud Client Libraries. [online]
 * Available at: https://firebase.google.com/docs/firestore/quickstart#c
 * [Accessed 21 September 2026].
 *
 * Google Cloud (2024) FirestoreDb Class (.NET API reference). [online]
 * Available at: https://cloud.google.com/dotnet/docs/reference/Google.Cloud.Firestore/latest
 * [Accessed 21 September 2026].
 *
 * C# Corner (2024) Integrating Google Cloud Firestore with .NET Core Web API. [online]
 * Available at: https://www.c-sharpcorner.com/article/integrating-google-cloud-firestore-with-net-core-web-api/
 * [Accessed 21 September 2026].
 *
 * Google DeepMind Antigravity (2026) Firestore Repository & Real-Time Sync Scaffolding.
 * ---------------------------------------------------------------------
 */

using Google.Apis.Auth.OAuth2;
using Google.Cloud.Firestore;
using PulseSync.Api.Models;

namespace PulseSync.Api.Services;

public class FirestoreDataStore : IDataStore
{
    private readonly InMemoryDataStore _fallbackCache = new();
    private readonly FirestoreDb? _firestoreDb;
    private readonly ILogger<FirestoreDataStore> _logger;
    private readonly string _projectId;

    public FirestoreDataStore(IConfiguration config, ILogger<FirestoreDataStore> logger)
    {
        _logger = logger;
        _projectId = config["Firebase:ProjectId"] ?? "pulsesync-ede3e";

        try
        {
            var builder = new FirestoreDbBuilder { ProjectId = _projectId };
            var credentialsJson = Environment.GetEnvironmentVariable("FIREBASE_CREDENTIALS_JSON") ??
                                  config["Firebase:CredentialsJson"];

            if (!string.IsNullOrWhiteSpace(credentialsJson))
            {
#pragma warning disable CS0618
                builder.Credential = GoogleCredential.FromJson(credentialsJson);
#pragma warning restore CS0618
            }

            _firestoreDb = builder.Build();
            _logger.LogInformation("Firestore connected successfully to project '{ProjectId}'", _projectId);

            // Seed Firestore collections in background
            _ = SeedFirestoreAsync();
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Firestore credentials not configured or initialization skipped. Using resilient cache for local testing.");
            _firestoreDb = null;
        }
    }

    private async Task SeedFirestoreAsync()
    {
        if (_firestoreDb == null) return;

        try
        {
            // Seed Categories
            var categories = _fallbackCache.GetCategories();
            foreach (var category in categories)
            {
                var docRef = _firestoreDb.Collection("categories").Document(category.Slug);
                await docRef.SetAsync(new Dictionary<string, object>
                {
                    ["slug"] = category.Slug,
                    ["nameEn"] = category.NameEn,
                    ["nameZu"] = category.NameZu,
                    ["nameAf"] = category.NameAf,
                    ["isSubscribed"] = category.IsSubscribed
                }, SetOptions.MergeAll);
            }

            // Seed Articles
            var articles = _fallbackCache.GetArticles(null, 50, null);
            foreach (var article in articles)
            {
                var docRef = _firestoreDb.Collection("articles").Document(article.ArticleId);
                await docRef.SetAsync(new Dictionary<string, object>
                {
                    ["articleId"] = article.ArticleId,
                    ["headline"] = article.Headline,
                    ["sourceName"] = article.SourceName,
                    ["sourceUrl"] = article.SourceUrl,
                    ["category"] = article.Category,
                    ["publishedAt"] = article.PublishedAt,
                    ["readTimeMinutes"] = article.ReadTimeMinutes,
                    ["aiSummary"] = new Dictionary<string, object>
                    {
                        ["detailed"] = article.AiSummary.Detailed,
                        ["condensed"] = article.AiSummary.Condensed,
                        ["model"] = article.AiSummary.Model,
                        ["generatedAt"] = article.AiSummary.GeneratedAt
                    },
                    ["derivedTags"] = article.DerivedTags
                }, SetOptions.MergeAll);
            }

            _logger.LogInformation("Firestore collections 'categories' and 'articles' verified.");
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Could not background-seed Firestore. Local fallback active.");
        }
    }

    public UserDto UpsertUser(UserDto user)
    {
        var result = _fallbackCache.UpsertUser(user);

        if (_firestoreDb != null)
        {
            _ = Task.Run(async () =>
            {
                try
                {
                    var docRef = _firestoreDb.Collection("users").Document(user.UserId);
                    await docRef.SetAsync(new Dictionary<string, object>
                    {
                        ["userId"] = user.UserId,
                        ["email"] = user.Email,
                        ["displayName"] = user.DisplayName,
                        ["photoUrl"] = user.PhotoUrl ?? string.Empty,
                        ["preferredLanguage"] = user.PreferredLanguage,
                        ["biometricEnabled"] = user.BiometricEnabled,
                        ["updatedAt"] = FieldValue.ServerTimestamp
                    }, SetOptions.MergeAll);

                    _logger.LogInformation("Synced user '{UserId}' to Firestore collection 'users'", user.UserId);
                }
                catch (Exception ex)
                {
                    _logger.LogWarning(ex, "Failed to persist user '{UserId}' to Firestore.", user.UserId);
                }
            });
        }

        return result;
    }

    public UserDto? GetUser(string userId) => _fallbackCache.GetUser(userId);

    public void SaveRefreshToken(string userId, string refreshToken) => _fallbackCache.SaveRefreshToken(userId, refreshToken);

    public string? GetRefreshToken(string userId) => _fallbackCache.GetRefreshToken(userId);

    public void InvalidateRefreshToken(string userId) => _fallbackCache.InvalidateRefreshToken(userId);

    public PreferencesDto GetPreferences(string userId) => _fallbackCache.GetPreferences(userId);

    public PreferencesDto SavePreferences(string userId, PreferencesDto prefs)
    {
        var result = _fallbackCache.SavePreferences(userId, prefs);

        if (_firestoreDb != null)
        {
            _ = Task.Run(async () =>
            {
                try
                {
                    var docRef = _firestoreDb.Collection("users").Document(userId);
                    await docRef.SetAsync(new Dictionary<string, object>
                    {
                        ["defaultSummaryMode"] = prefs.DefaultSummaryMode,
                        ["preferredLanguage"] = prefs.Language,
                        ["biometricEnabled"] = prefs.BiometricEnabled,
                        ["topicClusters"] = prefs.TopicClusters ?? new List<string>(),
                        ["updatedAt"] = FieldValue.ServerTimestamp
                    }, SetOptions.MergeAll);

                    _logger.LogInformation("Synced preferences for user '{UserId}' to Firestore", userId);
                }
                catch (Exception ex)
                {
                    _logger.LogWarning(ex, "Failed to persist preferences for user '{UserId}' to Firestore.", userId);
                }
            });
        }

        return result;
    }

    public IReadOnlyList<ArticleDto> GetArticles(string? category, int limit, string? cursor) =>
        _fallbackCache.GetArticles(category, limit, cursor);

    public ArticleDto? GetArticle(string articleId) => _fallbackCache.GetArticle(articleId);

    public IReadOnlyList<ArticleDto> SearchArticles(string query) => _fallbackCache.SearchArticles(query);

    public FullTextDto? GetFullText(string articleId) => _fallbackCache.GetFullText(articleId);

    public IReadOnlyList<CategoryDto> GetCategories() => _fallbackCache.GetCategories();

    public IReadOnlyList<NoteDto> GetNotes(string userId, string? tag, string? search) =>
        _fallbackCache.GetNotes(userId, tag, search);

    public IReadOnlyList<NoteDto> GetNotesForArticle(string userId, string articleId) =>
        _fallbackCache.GetNotesForArticle(userId, articleId);

    public NoteDto CreateNote(string userId, CreateNoteRequestDto req)
    {
        var note = _fallbackCache.CreateNote(userId, req);

        if (_firestoreDb != null)
        {
            _ = Task.Run(async () =>
            {
                try
                {
                    // Path: users/{userId}/notes/{noteId} as specified in Section 06 of Architecture Doc
                    var docRef = _firestoreDb.Collection("users").Document(userId).Collection("notes").Document(note.NoteId);
                    await docRef.SetAsync(new Dictionary<string, object>
                    {
                        ["noteId"] = note.NoteId,
                        ["localId"] = note.LocalId ?? string.Empty,
                        ["articleId"] = note.ArticleId ?? string.Empty,
                        ["title"] = note.Title,
                        ["bodyHtml"] = note.BodyHtml,
                        ["plainText"] = note.PlainText,
                        ["tags"] = note.Tags ?? new List<string>(),
                        ["isVaultNote"] = note.IsVaultNote,
                        ["isPinned"] = note.IsPinned,
                        ["createdAt"] = note.CreatedAt,
                        ["updatedAt"] = note.UpdatedAt
                    });

                    _logger.LogInformation("Synced note '{NoteId}' to Firestore subcollection 'users/{UserId}/notes'", note.NoteId, userId);
                }
                catch (Exception ex)
                {
                    _logger.LogWarning(ex, "Failed to persist note '{NoteId}' to Firestore subcollection.", note.NoteId);
                }
            });
        }

        return note;
    }

    public NoteDto? UpdateNote(string userId, string noteId, UpdateNoteRequestDto req)
    {
        var note = _fallbackCache.UpdateNote(userId, noteId, req);
        if (note == null) return null;

        if (_firestoreDb != null)
        {
            _ = Task.Run(async () =>
            {
                try
                {
                    var docRef = _firestoreDb.Collection("users").Document(userId).Collection("notes").Document(noteId);
                    await docRef.SetAsync(new Dictionary<string, object>
                    {
                        ["title"] = note.Title,
                        ["bodyHtml"] = note.BodyHtml,
                        ["plainText"] = note.PlainText,
                        ["tags"] = note.Tags ?? new List<string>(),
                        ["isPinned"] = note.IsPinned,
                        ["updatedAt"] = note.UpdatedAt
                    }, SetOptions.MergeAll);

                    _logger.LogInformation("Updated note '{NoteId}' in Firestore", noteId);
                }
                catch (Exception ex)
                {
                    _logger.LogWarning(ex, "Failed to update note '{NoteId}' in Firestore.", noteId);
                }
            });
        }

        return note;
    }

    public bool DeleteNote(string userId, string noteId)
    {
        var deleted = _fallbackCache.DeleteNote(userId, noteId);
        if (deleted && _firestoreDb != null)
        {
            _ = Task.Run(async () =>
            {
                try
                {
                    var docRef = _firestoreDb.Collection("users").Document(userId).Collection("notes").Document(noteId);
                    await docRef.DeleteAsync();
                    _logger.LogInformation("Deleted note '{NoteId}' from Firestore", noteId);
                }
                catch (Exception ex)
                {
                    _logger.LogWarning(ex, "Failed to delete note '{NoteId}' from Firestore.", noteId);
                }
            });
        }

        return deleted;
    }

    public IReadOnlyList<TagDto> GetNoteTags(string userId) => _fallbackCache.GetNoteTags(userId);

    public IReadOnlyList<KeywordDto> GetKeywords(string userId) => _fallbackCache.GetKeywords(userId);

    public KeywordDto AddKeyword(string userId, KeywordRequestDto req)
    {
        var keyword = _fallbackCache.AddKeyword(userId, req);

        if (_firestoreDb != null)
        {
            _ = Task.Run(async () =>
            {
                try
                {
                    var docRef = _firestoreDb.Collection("users").Document(userId).Collection("keywords").Document(keyword.KeywordId);
                    await docRef.SetAsync(new Dictionary<string, object>
                    {
                        ["keywordId"] = keyword.KeywordId,
                        ["keyword"] = keyword.Keyword,
                        ["notifyOnMatch"] = keyword.NotifyOnMatch
                    });
                }
                catch (Exception ex)
                {
                    _logger.LogWarning(ex, "Failed to add keyword to Firestore.");
                }
            });
        }

        return keyword;
    }

    public bool DeleteKeyword(string userId, string keywordId)
    {
        var deleted = _fallbackCache.DeleteKeyword(userId, keywordId);
        if (deleted && _firestoreDb != null)
        {
            _ = Task.Run(async () =>
            {
                try
                {
                    var docRef = _firestoreDb.Collection("users").Document(userId).Collection("keywords").Document(keywordId);
                    await docRef.DeleteAsync();
                }
                catch (Exception ex)
                {
                    _logger.LogWarning(ex, "Failed to delete keyword from Firestore.");
                }
            });
        }

        return deleted;
    }

    public DownloadSlotsDto GetDownloadSlots(string userId) => _fallbackCache.GetDownloadSlots(userId);

    public DownloadSlotsDto ClaimDownloadSlot(string userId, string articleId) => _fallbackCache.ClaimDownloadSlot(userId, articleId);

    public DownloadSlotsDto ReleaseDownloadSlot(string userId, string articleId) => _fallbackCache.ReleaseDownloadSlot(userId, articleId);
}
