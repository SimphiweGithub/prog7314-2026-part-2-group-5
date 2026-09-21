/*
 * Code Attribution No 42
 * This method was taken from "Unit Testing in ASP.NET Core Web API Using xUnit and Moq"
 * https://www.c-sharpcorner.com/article/unit-testing-in-asp-net-core-web-api-using-xunit-and-moq/
 * C# Corner
 */

using FluentAssertions;
using Microsoft.AspNetCore.Mvc;
using PulseSync.Api.Controllers;
using PulseSync.Api.Models;
using PulseSync.Api.Services;
using Xunit;

namespace PulseSync.Tests;

public class PreferencesControllerTests
{
    private readonly IDataStore _dataStore;
    private readonly PreferencesController _preferencesController;
    private readonly KeywordsController _keywordsController;

    public PreferencesControllerTests()
    {
        _dataStore = new InMemoryDataStore();
        _preferencesController = new PreferencesController(_dataStore);
        _keywordsController = new KeywordsController(_dataStore);
    }

    [Fact]
    public void GetPreferences_ReturnsDefaultPreferences()
    {
        // Act
        var result = _preferencesController.GetPreferences();

        // Assert
        result.Result.Should().BeOfType<OkObjectResult>();
        var okResult = result.Result as OkObjectResult;
        var prefs = okResult!.Value as PreferencesDto;

        prefs.Should().NotBeNull();
        prefs!.DefaultSummaryMode.Should().Be("DETAILED");
        prefs.Language.Should().Be("en");
    }

    [Fact]
    public void UpdatePreferences_UpdatesPreferencesAndReturnsUpdatedDto()
    {
        // Arrange
        var newPrefs = new PreferencesDto(
            DefaultSummaryMode: "CONDENSED",
            Language: "zu",
            BiometricEnabled: true,
            TopicClusters: new List<string> { "load-shedding", "bursaries" }
        );

        // Act
        var result = _preferencesController.UpdatePreferences(newPrefs);

        // Assert
        result.Result.Should().BeOfType<OkObjectResult>();
        var okResult = result.Result as OkObjectResult;
        var updated = okResult!.Value as PreferencesDto;

        updated.Should().NotBeNull();
        updated!.DefaultSummaryMode.Should().Be("CONDENSED");
        updated.Language.Should().Be("zu");
        updated.BiometricEnabled.Should().BeTrue();
        updated.TopicClusters.Should().Contain("load-shedding");
    }

    [Fact]
    public void Keywords_AddAndRetrieveKeyword_Succeeds()
    {
        // Arrange
        var request = new KeywordRequestDto("Load Shedding Zone 4", true);

        // Act
        var addResult = _keywordsController.AddKeyword(request);
        var getResult = _keywordsController.GetKeywords();

        // Assert
        addResult.Result.Should().BeOfType<OkObjectResult>();
        var added = (addResult.Result as OkObjectResult)!.Value as KeywordDto;
        added.Should().NotBeNull();
        added!.Keyword.Should().Be("Load Shedding Zone 4");

        getResult.Result.Should().BeOfType<OkObjectResult>();
        var list = (getResult.Result as OkObjectResult)!.Value as List<KeywordDto>;
        list.Should().Contain(k => k.Keyword == "Load Shedding Zone 4");
    }
}
