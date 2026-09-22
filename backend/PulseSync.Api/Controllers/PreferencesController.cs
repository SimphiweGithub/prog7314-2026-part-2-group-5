/*
 * Code Attribution No 33
 * This method was taken from "Building RESTful APIs with ASP.NET Core 8"
 * https://www.c-sharpcorner.com/article/building-restful-apis-with-asp-net-core/
 * C# Corner
 */

using Microsoft.AspNetCore.Mvc;
using PulseSync.Api.Models;
using PulseSync.Api.Services;

namespace PulseSync.Api.Controllers;

[ApiController]
[Route("api/v1/preferences")]
public class PreferencesController : AuthenticatedControllerBase
{
    private readonly IDataStore _dataStore;

    public PreferencesController(IDataStore dataStore)
    {
        _dataStore = dataStore;
    }

    [HttpGet]
    public ActionResult<PreferencesDto> GetPreferences()
    {
        var prefs = _dataStore.GetPreferences(CurrentUserId);
        return Ok(prefs);
    }

    [HttpPut]
    public ActionResult<PreferencesDto> UpdatePreferences([FromBody] PreferencesDto preferences)
    {
        var updated = _dataStore.SavePreferences(CurrentUserId, preferences);
        return Ok(updated);
    }
}

[ApiController]
[Route("api/v1/keywords")]
public class KeywordsController : AuthenticatedControllerBase
{
    private readonly IDataStore _dataStore;

    public KeywordsController(IDataStore dataStore)
    {
        _dataStore = dataStore;
    }

    [HttpGet]
    public ActionResult<List<KeywordDto>> GetKeywords()
    {
        var keywords = _dataStore.GetKeywords(CurrentUserId);
        return Ok(keywords.ToList());
    }

    [HttpPost]
    public ActionResult<KeywordDto> AddKeyword([FromBody] KeywordRequestDto request)
    {
        if (string.IsNullOrWhiteSpace(request.Keyword))
        {
            return BadRequest(new { message = "Keyword cannot be blank." });
        }

        var keyword = _dataStore.AddKeyword(CurrentUserId, request);
        return Ok(keyword);
    }

    [HttpDelete("{id}")]
    public IActionResult DeleteKeyword(string id)
    {
        var deleted = _dataStore.DeleteKeyword(CurrentUserId, id);
        if (!deleted)
        {
            return NotFound(new { message = $"Keyword with ID '{id}' was not found." });
        }

        return NoContent();
    }
}
