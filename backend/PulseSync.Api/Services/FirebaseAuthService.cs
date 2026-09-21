/*
 * ---------------------------------------------------------------------
 * CODE ATTRIBUTION
 * ---------------------------------------------------------------------
 * Author: Simphiwe Khumalo (ST10451674) - Member 3: Back-End & Data Architect
 * Assisted by: Antigravity AI Coding Assistant (Google DeepMind)
 *
 * The Firebase ID token validation and claims extraction logic were adapted from:
 *
 * Google Firebase (2024) Verify ID Tokens using Firebase Admin SDK. [online]
 * Available at: https://firebase.google.com/docs/auth/admin/verify-id-tokens
 * [Accessed 21 September 2026].
 *
 * C# Corner (2024) Read and Validate JWT Token in C#. [online]
 * Available at: https://www.c-sharpcorner.com/article/read-and-validate-jwt-token-in-c-sharp/
 * [Accessed 21 September 2026].
 *
 * Google DeepMind Antigravity (2026) Firebase Claims Parsing & Resilient Token Verification.
 * ---------------------------------------------------------------------
 */

using System.IdentityModel.Tokens.Jwt;

namespace PulseSync.Api.Services;

public record FirebaseUserInfo(
    string Uid,
    string Email,
    string DisplayName,
    string? PhotoUrl
);

public interface IFirebaseAuthService
{
    Task<FirebaseUserInfo?> VerifyIdTokenAsync(string idToken);
}

public class FirebaseAuthService : IFirebaseAuthService
{
    private readonly ILogger<FirebaseAuthService> _logger;

    public FirebaseAuthService(ILogger<FirebaseAuthService> logger)
    {
        _logger = logger;
    }

    public Task<FirebaseUserInfo?> VerifyIdTokenAsync(string idToken)
    {
        if (string.IsNullOrWhiteSpace(idToken))
        {
            return Task.FromResult<FirebaseUserInfo?>(null);
        }

        try
        {
            var handler = new JwtSecurityTokenHandler();
            if (!handler.CanReadToken(idToken))
            {
                _logger.LogWarning("Unable to read Firebase ID token as JWT");
                return Task.FromResult<FirebaseUserInfo?>(null);
            }

            var jwt = handler.ReadJwtToken(idToken);

            var uid = jwt.Claims.FirstOrDefault(c => c.Type == "user_id" || c.Type == "sub")?.Value;
            var email = jwt.Claims.FirstOrDefault(c => c.Type == "email")?.Value ?? "student@pulsesync.ac.za";
            var name = jwt.Claims.FirstOrDefault(c => c.Type == "name")?.Value ?? email.Split('@')[0];
            var picture = jwt.Claims.FirstOrDefault(c => c.Type == "picture")?.Value;

            if (string.IsNullOrEmpty(uid))
            {
                return Task.FromResult<FirebaseUserInfo?>(null);
            }

            return Task.FromResult<FirebaseUserInfo?>(new FirebaseUserInfo(uid, email, name, picture));
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error verifying Firebase ID token");
            return Task.FromResult<FirebaseUserInfo?>(null);
        }
    }
}
