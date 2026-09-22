/*
 * Code Attribution No 31
 * This method was taken from "How to Implement Refresh Token in ASP.NET Core Web API"
 * https://www.c-sharpcorner.com/article/how-to-implement-refresh-token-in-asp-net-core-web-api/
 * C# Corner
 */

using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using PulseSync.Api.Models;
using PulseSync.Api.Services;

namespace PulseSync.Api.Controllers;

[ApiController]
[Route("api/v1/auth")]
public class AuthController : ControllerBase
{
    private readonly IFirebaseAuthService _firebaseAuth;
    private readonly ITokenService _tokenService;
    private readonly IDataStore _dataStore;
    private readonly ILogger<AuthController> _logger;

    public AuthController(
        IFirebaseAuthService firebaseAuth,
        ITokenService tokenService,
        IDataStore dataStore,
        ILogger<AuthController> logger)
    {
        _firebaseAuth = firebaseAuth;
        _tokenService = tokenService;
        _dataStore = dataStore;
        _logger = logger;
    }

    [HttpPost("google")]
    public async Task<ActionResult<AuthResponseDto>> ExchangeGoogleToken([FromBody] GoogleSsoRequestDto request)
    {
        if (string.IsNullOrWhiteSpace(request.FirebaseIdToken))
        {
            return BadRequest(new { message = "Firebase ID token is required." });
        }

        var userInfo = await _firebaseAuth.VerifyIdTokenAsync(request.FirebaseIdToken);
        if (userInfo == null)
        {
            _logger.LogWarning("Firebase ID token verification failed");
            return Unauthorized(new { message = "Invalid or expired Firebase ID token." });
        }

        var existingUser = _dataStore.GetUser(userInfo.Uid);
        var isNewUser = existingUser == null;

        var user = new UserDto(
            UserId: userInfo.Uid,
            Email: userInfo.Email,
            DisplayName: userInfo.DisplayName,
            PhotoUrl: userInfo.PhotoUrl,
            PreferredLanguage: request.PreferredLanguage,
            BiometricEnabled: existingUser?.BiometricEnabled ?? false
        );

        _dataStore.UpsertUser(user);

        var accessToken = _tokenService.GenerateAccessToken(user.UserId, user.Email);
        var refreshToken = _tokenService.GenerateRefreshToken();
        _dataStore.SaveRefreshToken(user.UserId, refreshToken);

        _logger.LogInformation("Authenticated user {UserId} ({Email}) successfully", user.UserId, user.Email);

        return Ok(new AuthResponseDto(
            UserId: user.UserId,
            AccessToken: accessToken,
            RefreshToken: refreshToken,
            ExpiresIn: 7200,
            IsNewUser: isNewUser
        ));
    }

    /// <summary>
    /// Exchanges a refresh token for a new access/refresh pair. The refresh
    /// token must match the one currently stored for its user; it is rotated on
    /// every use, so a replayed (old) token is rejected with 401.
    /// </summary>
    [HttpPost("refresh")]
    public ActionResult<AuthResponseDto> Refresh([FromBody] RefreshRequestDto request)
    {
        if (string.IsNullOrWhiteSpace(request.RefreshToken))
        {
            return BadRequest(new { message = "Refresh token is required." });
        }

        var userId = _dataStore.FindUserIdByRefreshToken(request.RefreshToken);
        if (userId == null)
        {
            _logger.LogWarning("Refresh rejected: token is unknown, rotated or revoked");
            return Unauthorized(new { message = "Refresh token is invalid or has been revoked. Please sign in again." });
        }

        var email = _dataStore.GetUser(userId)?.Email ?? string.Empty;
        var accessToken = _tokenService.GenerateAccessToken(userId, email);
        var refreshToken = _tokenService.GenerateRefreshToken();
        _dataStore.SaveRefreshToken(userId, refreshToken);

        _logger.LogInformation("Rotated tokens for user {UserId}", userId);

        return Ok(new AuthResponseDto(
            UserId: userId,
            AccessToken: accessToken,
            RefreshToken: refreshToken,
            ExpiresIn: 7200,
            IsNewUser: false
        ));
    }

    /// <summary>
    /// Revokes the caller's refresh token so it can no longer mint access tokens.
    /// Requires a valid access token; an anonymous call is rejected with 401.
    /// </summary>
    [Authorize]
    [HttpPost("logout")]
    public IActionResult Logout()
    {
        var userId = User?.FindFirstValue(ClaimTypes.NameIdentifier) ?? User?.FindFirstValue("userId");
        if (userId == null)
        {
            return Unauthorized(new { message = "The request does not carry an authenticated user identity." });
        }

        _dataStore.InvalidateRefreshToken(userId);
        _logger.LogInformation("User {UserId} logged out; refresh token revoked", userId);
        return Ok(new { message = "Logged out successfully." });
    }
}
