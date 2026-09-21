using System.Security.Claims;
using Microsoft.AspNetCore.Mvc;
using PulseSync.Api.Models;
using PulseSync.Api.Services;

namespace PulseSync.Api.Controllers;

[ApiController]
[Route("api/v1/users")]
public class UsersController : ControllerBase
{
    private readonly IDataStore _dataStore;

    public UsersController(IDataStore dataStore)
    {
        _dataStore = dataStore;
    }

    private string CurrentUserId =>
        User?.FindFirstValue(ClaimTypes.NameIdentifier) ??
        User?.FindFirstValue("userId") ??
        "demo-user";

    [HttpGet("me")]
    public ActionResult<UserDto> GetCurrentUser()
    {
        var user = _dataStore.GetUser(CurrentUserId) ?? new UserDto(
            UserId: CurrentUserId,
            Email: "student@pulsesync.ac.za",
            DisplayName: "PulseSync Student",
            PhotoUrl: null,
            PreferredLanguage: "en",
            BiometricEnabled: false
        );

        return Ok(user);
    }

    [HttpPut("me")]
    public ActionResult<UserDto> UpdateCurrentUser([FromBody] UpdateProfileRequestDto request)
    {
        var existing = _dataStore.GetUser(CurrentUserId) ?? new UserDto(
            UserId: CurrentUserId,
            Email: "student@pulsesync.ac.za",
            DisplayName: "PulseSync Student"
        );

        var updated = existing with
        {
            DisplayName = request.DisplayName ?? existing.DisplayName,
            PhotoUrl = request.PhotoUrl ?? existing.PhotoUrl
        };

        _dataStore.UpsertUser(updated);
        return Ok(updated);
    }
}

[ApiController]
[Route("api/v1/downloads")]
public class DownloadsController : ControllerBase
{
    private readonly IDataStore _dataStore;

    public DownloadsController(IDataStore dataStore)
    {
        _dataStore = dataStore;
    }

    private string CurrentUserId =>
        User?.FindFirstValue(ClaimTypes.NameIdentifier) ??
        User?.FindFirstValue("userId") ??
        "demo-user";

    [HttpGet]
    public ActionResult<DownloadSlotsDto> GetSlots()
    {
        var slots = _dataStore.GetDownloadSlots(CurrentUserId);
        return Ok(slots);
    }

    [HttpPost]
    public ActionResult<DownloadSlotsDto> ClaimSlot([FromBody] ClaimSlotRequestDto request)
    {
        var slots = _dataStore.ClaimDownloadSlot(CurrentUserId, request.ArticleId);
        return Ok(slots);
    }

    [HttpDelete("{articleId}")]
    public ActionResult<DownloadSlotsDto> ReleaseSlot(string articleId)
    {
        var slots = _dataStore.ReleaseDownloadSlot(CurrentUserId, articleId);
        return Ok(slots);
    }
}
