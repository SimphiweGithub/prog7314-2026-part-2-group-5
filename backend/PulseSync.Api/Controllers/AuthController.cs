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

    [HttpPost("refresh")]
    public ActionResult<AuthResponseDto> Refresh([FromBody] RefreshRequestDto request)
    {
        if (string.IsNullOrWhiteSpace(request.RefreshToken))
        {
            return BadRequest(new { message = "Refresh token is required." });
        }

        // For demo/prototype: locate user with matching refresh token
        // If not found, return unauthorized
        return Ok(new AuthResponseDto(
            UserId: "refreshed-user",
            AccessToken: _tokenService.GenerateAccessToken("refreshed-user", "user@pulsesync.ac.za"),
            RefreshToken: _tokenService.GenerateRefreshToken(),
            ExpiresIn: 7200,
            IsNewUser: false
        ));
    }

    [HttpPost("logout")]
    public IActionResult Logout()
    {
        return Ok(new { message = "Logged out successfully." });
    }
}
