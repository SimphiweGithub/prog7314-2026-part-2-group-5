/*
 * ---------------------------------------------------------------------
 * CODE ATTRIBUTION
 * ---------------------------------------------------------------------
 * Author: Simphiwe Khumalo (ST10451674) - Member 3: Back-End & Data Architect
 * Assisted by: Antigravity AI Coding Assistant (Google DeepMind)
 *
 * The xUnit test patterns and FluentAssertions for article feed verification were adapted from:
 *
 * C# Corner (2024) Unit Testing in ASP.NET Core Web API Using xUnit and Moq. [online]
 * Available at: https://www.c-sharpcorner.com/article/unit-testing-in-asp-net-core-web-api-using-xunit-and-moq/
 * [Accessed 21 September 2026].
 *
 * Google DeepMind Antigravity (2026) Automated Test Suite Scaffolding & Assertion Design.
 * ---------------------------------------------------------------------
 */

using FluentAssertions;
using Microsoft.AspNetCore.Mvc;
using PulseSync.Api.Controllers;
using PulseSync.Api.Models;
using PulseSync.Api.Services;
using Xunit;

namespace PulseSync.Tests;

public class ArticlesControllerTests
{
    private readonly IDataStore _dataStore;
    private readonly ArticlesController _articlesController;
    private readonly CategoriesController _categoriesController;

    public ArticlesControllerTests()
    {
        _dataStore = new InMemoryDataStore();
        _articlesController = new ArticlesController(_dataStore);
        _categoriesController = new CategoriesController(_dataStore);
    }

    [Fact]
    public void GetFeed_ReturnsAllArticlesWithDualModeSummaries()
    {
        // Act
        var result = _articlesController.GetFeed();

        // Assert
        result.Result.Should().BeOfType<OkObjectResult>();
        var okResult = result.Result as OkObjectResult;
        var feed = okResult!.Value as FeedResponseDto;

        feed.Should().NotBeNull();
        feed!.Articles.Should().NotBeEmpty();
        feed.Articles.Count.Should().BeGreaterThanOrEqualTo(4);

        // Every article must have both detailed and condensed AI summaries (User Defined 1 requirement)
        feed.Articles.Should().AllSatisfy(article =>
        {
            article.AiSummary.Should().NotBeNull();
            article.AiSummary.Detailed.Should().NotBeEmpty();
            article.AiSummary.Condensed.Should().NotBeEmpty();
        });
    }

    [Fact]
    public void GetFeed_WithCategoryFilter_ReturnsOnlyMatchingCategory()
    {
        // Act
        var result = _articlesController.GetFeed(category: "load-shedding");

        // Assert
        result.Result.Should().BeOfType<OkObjectResult>();
        var okResult = result.Result as OkObjectResult;
        var feed = okResult!.Value as FeedResponseDto;

        feed.Should().NotBeNull();
        feed!.Articles.Should().NotBeEmpty();
        feed.Articles.Should().OnlyContain(a => a.Category == "load-shedding");
    }

    [Fact]
    public void GetArticle_WithValidId_ReturnsArticle()
    {
        // Act
        var result = _articlesController.GetArticle("nsfas-2027");

        // Assert
        result.Result.Should().BeOfType<OkObjectResult>();
        var okResult = result.Result as OkObjectResult;
        var article = okResult!.Value as ArticleDto;

        article.Should().NotBeNull();
        article!.ArticleId.Should().Be("nsfas-2027");
        article.Headline.Should().Contain("NSFAS");
        article.ExtractedLinks.Should().NotBeEmpty();
    }

    [Fact]
    public void GetArticle_WithInvalidId_Returns404NotFound()
    {
        // Act
        var result = _articlesController.GetArticle("non_existent_article_id");

        // Assert
        result.Result.Should().BeOfType<NotFoundObjectResult>();
    }

    [Fact]
    public void SearchArticles_WithQuery_ReturnsRelevantMatches()
    {
        // Act
        var result = _articlesController.SearchArticles("Eskom");

        // Assert
        result.Result.Should().BeOfType<OkObjectResult>();
        var okResult = result.Result as OkObjectResult;
        var list = okResult!.Value as List<ArticleDto>;

        list.Should().NotBeNull();
        list!.Should().Contain(a => a.ArticleId == "eskom-stage-2");
    }

    [Fact]
    public void GetCategories_ReturnsLocalizedCategories()
    {
        // Act
        var result = _categoriesController.GetCategories();

        // Assert
        result.Result.Should().BeOfType<OkObjectResult>();
        var okResult = result.Result as OkObjectResult;
        var categories = okResult!.Value as List<CategoryDto>;

        categories.Should().NotBeNull();
        categories!.Count.Should().BeGreaterThanOrEqualTo(4);
        categories.Should().Contain(c => c.Slug == "load-shedding" && c.NameZu == "Ukucinywa kukagesi");
    }
}
