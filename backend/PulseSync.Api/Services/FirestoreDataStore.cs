/*
 * Code Attribution No 36
 * This method was taken from "Cloud Firestore: Add data with the Cloud Client Libraries"
 * https://firebase.google.com/docs/firestore/quickstart#c
 * Google Firebase
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

    public UserDto? GetUser(string userId)
    {
        var cached = _fallbackCache.GetUser(userId);
        if (cached != null || _firestoreDb == null) return cached;

        // Read-through after a restart: the in-memory cache is empty but the
        // profile was persisted by UpsertUser on the previous run.
        try
        {
            var snapshot = _firestoreDb.Collection("users").Document(userId).GetSnapshotAsync().GetAwaiter().GetResult();
            if (!snapshot.Exists) return null;

            var user = new UserDto(
                UserId: userId,
                Email: snapshot.GetValue<string>("email"),
                DisplayName: snapshot.GetValue<string>("displayName"),
                PhotoUrl: snapshot.ContainsField("photoUrl") ? NullIfEmpty(snapshot.GetValue<string>("photoUrl")) : null,
                PreferredLanguage: snapshot.ContainsField("preferredLanguage") ? snapshot.GetValue<string>("preferredLanguage") : "en",
                BiometricEnabled: snapshot.ContainsField("biometricEnabled") && snapshot.GetValue<bool>("biometricEnabled")
            );
            _fallbackCache.UpsertUser(user);
            return user;
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to read user '{UserId}' from Firestore.", userId);
            return null;
        }
    }

    private static string? NullIfEmpty(string? value) => string.IsNullOrEmpty(value) ? null : value;

    // Refresh tokens are stored by SHA-256 hash only, so a Firestore leak does
    // not hand out usable credentials. The cache keeps the raw token for the
    // lifetime of the process; Firestore lets a session survive a redeploy.
    private static string HashToken(string token) =>
        Convert.ToHexString(System.Security.Cryptography.SHA256.HashData(System.Text.Encoding.UTF8.GetBytes(token)));

    public void SaveRefreshToken(string userId, string refreshToken)
    {
        var previous = _fallbackCache.GetRefreshToken(userId);
        _fallbackCache.SaveRefreshToken(userId, refreshToken);

        if (_firestoreDb == null) return;
        _ = Task.Run(async () =>
        {
            try
            {
                var tokens = _firestoreDb.Collection("refreshTokens");
                if (previous != null)
                {
                    await tokens.Document(HashToken(previous)).DeleteAsync();
                }
                await tokens.Document(HashToken(refreshToken)).SetAsync(new Dictionary<string, object>
                {
                    ["userId"] = userId,
                    ["issuedAt"] = FieldValue.ServerTimestamp
                });
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Failed to persist refresh token for '{UserId}' to Firestore.", userId);
            }
        });
    }

    public string? GetRefreshToken(string userId) => _fallbackCache.GetRefreshToken(userId);

    public string? FindUserIdByRefreshToken(string refreshToken)
    {
        var cached = _fallbackCache.FindUserIdByRefreshToken(refreshToken);
        if (cached != null || _firestoreDb == null) return cached;

        try
        {
            var snapshot = _firestoreDb.Collection("refreshTokens").Document(HashToken(refreshToken))
                .GetSnapshotAsync().GetAwaiter().GetResult();
            if (!snapshot.Exists) return null;

            var userId = snapshot.GetValue<string>("userId");
            _fallbackCache.SaveRefreshToken(userId, refreshToken);
            return userId;
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to look up refresh token in Firestore.");
            return null;
        }
    }

    public void InvalidateRefreshToken(string userId)
    {
        var current = _fallbackCache.GetRefreshToken(userId);
        _fallbackCache.InvalidateRefreshToken(userId);

        if (_firestoreDb == null || current == null) return;
        _ = Task.Run(async () =>
        {
            try
            {
                await _firestoreDb.Collection("refreshTokens").Document(HashToken(current)).DeleteAsync();
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Failed to revoke refresh token for '{UserId}' in Firestore.", userId);
            }
        });
    }

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
                    // Ensure parent user document exists with fields so Firestore does not show it as a phantom
                    var userDocRef = _firestoreDb.Collection("users").Document(userId);
                    await userDocRef.SetAsync(new Dictionary<string, object>
                    {
                        ["userId"] = userId,
                        ["updatedAt"] = FieldValue.ServerTimestamp
                    }, SetOptions.MergeAll);

                    // Path: users/{userId}/notes/{noteId} as specified in Section 06 of Architecture Doc
                    var docRef = userDocRef.Collection("notes").Document(note.NoteId);
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
