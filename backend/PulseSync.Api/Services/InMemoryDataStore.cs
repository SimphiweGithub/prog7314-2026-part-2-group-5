using System.Collections.Concurrent;
using PulseSync.Api.Data;
using PulseSync.Api.Models;

namespace PulseSync.Api.Services;

public interface IDataStore
{
    // Users & Auth
    UserDto UpsertUser(UserDto user);
    UserDto? GetUser(string userId);
    void SaveRefreshToken(string userId, string refreshToken);
    string? GetRefreshToken(string userId);
    void InvalidateRefreshToken(string userId);

    // Preferences
    PreferencesDto GetPreferences(string userId);
    PreferencesDto SavePreferences(string userId, PreferencesDto prefs);

    // Articles & Feed
    IReadOnlyList<ArticleDto> GetArticles(string? category, int limit, string? cursor);
    ArticleDto? GetArticle(string articleId);
    IReadOnlyList<ArticleDto> SearchArticles(string query);
    FullTextDto? GetFullText(string articleId);
    IReadOnlyList<CategoryDto> GetCategories();

    // Notes
    IReadOnlyList<NoteDto> GetNotes(string userId, string? tag, string? search);
    IReadOnlyList<NoteDto> GetNotesForArticle(string userId, string articleId);
    NoteDto CreateNote(string userId, CreateNoteRequestDto req);
    NoteDto? UpdateNote(string userId, string noteId, UpdateNoteRequestDto req);
    bool DeleteNote(string userId, string noteId);
    IReadOnlyList<TagDto> GetNoteTags(string userId);

    // Keywords
    IReadOnlyList<KeywordDto> GetKeywords(string userId);
    KeywordDto AddKeyword(string userId, KeywordRequestDto req);
    bool DeleteKeyword(string userId, string keywordId);

    // Downloads
    DownloadSlotsDto GetDownloadSlots(string userId);
    DownloadSlotsDto ClaimDownloadSlot(string userId, string articleId);
    DownloadSlotsDto ReleaseDownloadSlot(string userId, string articleId);
}

public class InMemoryDataStore : IDataStore
{
    private readonly ConcurrentDictionary<string, UserDto> _users = new();
    private readonly ConcurrentDictionary<string, string> _refreshTokens = new();
    private readonly ConcurrentDictionary<string, PreferencesDto> _preferences = new();
    private readonly ConcurrentDictionary<string, List<NoteDto>> _notes = new();
    private readonly ConcurrentDictionary<string, List<KeywordDto>> _keywords = new();
    private readonly ConcurrentDictionary<string, List<OccupiedSlotDto>> _downloadSlots = new();

    private readonly List<ArticleDto> _articles;
    private readonly List<CategoryDto> _categories;
    private readonly Dictionary<string, FullTextDto> _fullTexts;

    public InMemoryDataStore()
    {
        _articles = new List<ArticleDto>(SeedData.Articles);
        _categories = new List<CategoryDto>(SeedData.Categories);
        _fullTexts = new Dictionary<string, FullTextDto>(SeedData.FullTexts);
    }

    public UserDto UpsertUser(UserDto user)
    {
        return _users.AddOrUpdate(user.UserId, user, (_, _) => user);
    }

    public UserDto? GetUser(string userId)
    {
        _users.TryGetValue(userId, out var user);
        return user;
    }

    public void SaveRefreshToken(string userId, string refreshToken)
    {
        _refreshTokens[userId] = refreshToken;
    }

    public string? GetRefreshToken(string userId)
    {
        _refreshTokens.TryGetValue(userId, out var token);
        return token;
    }

    public void InvalidateRefreshToken(string userId)
    {
        _refreshTokens.TryRemove(userId, out _);
    }

    public PreferencesDto GetPreferences(string userId)
    {
        return _preferences.GetOrAdd(userId, _ => new PreferencesDto());
    }

    public PreferencesDto SavePreferences(string userId, PreferencesDto prefs)
    {
        return _preferences.AddOrUpdate(userId, prefs, (_, _) => prefs);
    }

    public IReadOnlyList<ArticleDto> GetArticles(string? category, int limit, string? cursor)
    {
        var query = _articles.AsEnumerable();
        if (!string.IsNullOrWhiteSpace(category))
        {
            query = query.Where(a => string.Equals(a.Category, category, StringComparison.OrdinalIgnoreCase));
        }

        return query.Take(limit > 0 ? limit : 20).ToList();
    }

    public ArticleDto? GetArticle(string articleId)
    {
        return _articles.FirstOrDefault(a => string.Equals(a.ArticleId, articleId, StringComparison.OrdinalIgnoreCase));
    }

    public IReadOnlyList<ArticleDto> SearchArticles(string query)
    {
        if (string.IsNullOrWhiteSpace(query)) return _articles;

        return _articles.Where(a =>
            a.Headline.Contains(query, StringComparison.OrdinalIgnoreCase) ||
            a.Category.Contains(query, StringComparison.OrdinalIgnoreCase) ||
            a.DerivedTags.Any(t => t.Contains(query, StringComparison.OrdinalIgnoreCase))
        ).ToList();
    }

    public FullTextDto? GetFullText(string articleId)
    {
        _fullTexts.TryGetValue(articleId, out var fullText);
        return fullText;
    }

    public IReadOnlyList<CategoryDto> GetCategories() => _categories;

    public IReadOnlyList<NoteDto> GetNotes(string userId, string? tag, string? search)
    {
        var userNotes = _notes.GetOrAdd(userId, _ => new List<NoteDto>());
        lock (userNotes)
        {
            var result = userNotes.AsEnumerable();
            if (!string.IsNullOrWhiteSpace(tag))
            {
                result = result.Where(n => (n.Tags ?? Enumerable.Empty<string>()).Any(t => string.Equals(t, tag, StringComparison.OrdinalIgnoreCase)));
            }

            if (!string.IsNullOrWhiteSpace(search))
            {
                result = result.Where(n =>
                    n.Title.Contains(search, StringComparison.OrdinalIgnoreCase) ||
                    n.PlainText.Contains(search, StringComparison.OrdinalIgnoreCase)
                );
            }

            return result.OrderByDescending(n => n.UpdatedAt).ToList();
        }
    }

    public IReadOnlyList<NoteDto> GetNotesForArticle(string userId, string articleId)
    {
        var userNotes = _notes.GetOrAdd(userId, _ => new List<NoteDto>());
        lock (userNotes)
        {
            return userNotes
                .Where(n => string.Equals(n.ArticleId, articleId, StringComparison.OrdinalIgnoreCase))
                .OrderByDescending(n => n.UpdatedAt)
                .ToList();
        }
    }

    public NoteDto CreateNote(string userId, CreateNoteRequestDto req)
    {
        var userNotes = _notes.GetOrAdd(userId, _ => new List<NoteDto>());
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        var note = new NoteDto(
            NoteId: Guid.NewGuid().ToString("N")[..12],
            LocalId: req.LocalId,
            ArticleId: req.ArticleId,
            Title: req.Title,
            BodyHtml: req.BodyHtml,
            PlainText: req.PlainText,
            Tags: req.Tags,
            IsVaultNote: req.IsVaultNote,
            IsPinned: false,
            CreatedAt: now,
            UpdatedAt: req.UpdatedAt > 0 ? req.UpdatedAt : now
        );

        lock (userNotes)
        {
            userNotes.Add(note);
        }

        return note;
    }

    public NoteDto? UpdateNote(string userId, string noteId, UpdateNoteRequestDto req)
    {
        var userNotes = _notes.GetOrAdd(userId, _ => new List<NoteDto>());
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

        lock (userNotes)
        {
            var index = userNotes.FindIndex(n => string.Equals(n.NoteId, noteId, StringComparison.OrdinalIgnoreCase));
            if (index < 0) return null;

            var existing = userNotes[index];
            var updated = existing with
            {
                Title = req.Title,
                BodyHtml = req.BodyHtml,
                PlainText = req.PlainText,
                Tags = req.Tags,
                IsPinned = req.IsPinned,
                UpdatedAt = req.UpdatedAt > 0 ? req.UpdatedAt : now
            };

            userNotes[index] = updated;
            return updated;
        }
    }

    public bool DeleteNote(string userId, string noteId)
    {
        var userNotes = _notes.GetOrAdd(userId, _ => new List<NoteDto>());
        lock (userNotes)
        {
            return userNotes.RemoveAll(n => string.Equals(n.NoteId, noteId, StringComparison.OrdinalIgnoreCase)) > 0;
        }
    }

    public IReadOnlyList<TagDto> GetNoteTags(string userId)
    {
        var userNotes = _notes.GetOrAdd(userId, _ => new List<NoteDto>());
        lock (userNotes)
        {
            return userNotes
                .SelectMany(n => n.Tags ?? Enumerable.Empty<string>())
                .Where(t => !string.IsNullOrWhiteSpace(t))
                .GroupBy(t => t.ToLowerInvariant())
                .Select(g => new TagDto(g.Key, g.Count()))
                .OrderByDescending(t => t.Count)
                .ToList();
        }
    }

    public IReadOnlyList<KeywordDto> GetKeywords(string userId)
    {
        var userKeywords = _keywords.GetOrAdd(userId, _ => new List<KeywordDto>
        {
            new(Guid.NewGuid().ToString("N")[..8], "NSFAS", true),
            new(Guid.NewGuid().ToString("N")[..8], "Load Shedding", true)
        });

        lock (userKeywords)
        {
            return userKeywords.ToList();
        }
    }

    public KeywordDto AddKeyword(string userId, KeywordRequestDto req)
    {
        var userKeywords = _keywords.GetOrAdd(userId, _ => new List<KeywordDto>());
        var keyword = new KeywordDto(Guid.NewGuid().ToString("N")[..8], req.Keyword.Trim(), req.NotifyOnMatch);

        lock (userKeywords)
        {
            userKeywords.Add(keyword);
        }

        return keyword;
    }

    public bool DeleteKeyword(string userId, string keywordId)
    {
        var userKeywords = _keywords.GetOrAdd(userId, _ => new List<KeywordDto>());
        lock (userKeywords)
        {
            return userKeywords.RemoveAll(k => string.Equals(k.KeywordId, keywordId, StringComparison.OrdinalIgnoreCase)) > 0;
        }
    }

    public DownloadSlotsDto GetDownloadSlots(string userId)
    {
        var slots = _downloadSlots.GetOrAdd(userId, _ => new List<OccupiedSlotDto>());
        lock (slots)
        {
            return new DownloadSlotsDto(slots.Count, 5, slots.ToList());
        }
    }

    public DownloadSlotsDto ClaimDownloadSlot(string userId, string articleId)
    {
        var slots = _downloadSlots.GetOrAdd(userId, _ => new List<OccupiedSlotDto>());
        lock (slots)
        {
            if (slots.Count >= 5)
            {
                return new DownloadSlotsDto(slots.Count, 5, slots.ToList());
            }

            if (!slots.Any(s => string.Equals(s.ArticleId, articleId, StringComparison.OrdinalIgnoreCase)))
            {
                slots.Add(new OccupiedSlotDto(articleId, 1024));
            }

            return new DownloadSlotsDto(slots.Count, 5, slots.ToList());
        }
    }

    public DownloadSlotsDto ReleaseDownloadSlot(string userId, string articleId)
    {
        var slots = _downloadSlots.GetOrAdd(userId, _ => new List<OccupiedSlotDto>());
        lock (slots)
        {
            slots.RemoveAll(s => string.Equals(s.ArticleId, articleId, StringComparison.OrdinalIgnoreCase));
            return new DownloadSlotsDto(slots.Count, 5, slots.ToList());
        }
    }
}
