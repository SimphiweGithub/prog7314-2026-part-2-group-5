using Microsoft.AspNetCore.Mvc;
using PulseSync.Api.Models;
using PulseSync.Api.Services;

namespace PulseSync.Api.Controllers;

[ApiController]
[Route("api/v1/articles")]
public class ArticlesController : ControllerBase
{
    private readonly IDataStore _dataStore;

    public ArticlesController(IDataStore dataStore)
    {
        _dataStore = dataStore;
    }

    [HttpGet]
    public ActionResult<FeedResponseDto> GetFeed(
        [FromQuery] string? category = null,
        [FromQuery] string? cursor = null,
        [FromQuery] int limit = 20)
    {
        var articles = _dataStore.GetArticles(category, limit, cursor);
        var timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

        return Ok(new FeedResponseDto(
            Articles: articles.ToList(),
            NextCursor: null,
            ServerTimestamp: timestamp
        ));
    }

    [HttpGet("search")]
    public ActionResult<List<ArticleDto>> SearchArticles([FromQuery] string q)
    {
        var results = _dataStore.SearchArticles(q);
        return Ok(results.ToList());
    }

    [HttpGet("{id}")]
    public ActionResult<ArticleDto> GetArticle(string id)
    {
        var article = _dataStore.GetArticle(id);
        if (article == null)
        {
            return NotFound(new { message = $"Article with ID '{id}' was not found." });
        }

        return Ok(article);
    }

    [HttpGet("{id}/fulltext")]
    public ActionResult<FullTextDto> GetFullText(string id)
    {
        var fullText = _dataStore.GetFullText(id);
        if (fullText == null)
        {
            return NotFound(new { message = $"Full text for article '{id}' was not found." });
        }

        return Ok(fullText);
    }
}

[ApiController]
[Route("api/v1/categories")]
public class CategoriesController : ControllerBase
{
    private readonly IDataStore _dataStore;

    public CategoriesController(IDataStore dataStore)
    {
        _dataStore = dataStore;
    }

    [HttpGet]
    public ActionResult<List<CategoryDto>> GetCategories()
    {
        var categories = _dataStore.GetCategories();
        return Ok(categories.ToList());
    }
}
