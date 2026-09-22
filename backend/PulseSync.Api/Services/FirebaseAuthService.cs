/*
 * Code Attribution No 35
 * This method was taken from "Verify ID Tokens using Firebase Admin SDK"
 * https://firebase.google.com/docs/auth/admin/verify-id-tokens
 * Google Firebase
 */

using FirebaseAdmin;
using FirebaseAdmin.Auth;
using Google.Apis.Auth.OAuth2;

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

/// <summary>
/// Verifies Firebase ID tokens with the Firebase Admin SDK: the token's RS256
/// signature is checked against Google's published public keys, and the
/// issuer, audience (project id) and expiry are validated. A token that fails
/// any of those checks, or a server with no Firebase credentials configured,
/// yields <c>null</c> so the caller answers 401 (fail closed).
/// </summary>
public class FirebaseAuthService : IFirebaseAuthService
{
    private static readonly object InitLock = new();
    private static FirebaseApp? _app;
    private static bool _initialisationAttempted;

    private readonly ILogger<FirebaseAuthService> _logger;
    private readonly IConfiguration _config;

    public FirebaseAuthService(IConfiguration config, ILogger<FirebaseAuthService> logger)
    {
        _config = config;
        _logger = logger;
    }

    public async Task<FirebaseUserInfo?> VerifyIdTokenAsync(string idToken)
    {
        if (string.IsNullOrWhiteSpace(idToken))
        {
            return null;
        }

        var app = GetOrCreateApp();
        if (app == null)
        {
            _logger.LogError("Firebase Admin SDK is not initialised (no credentials); rejecting ID token");
            return null;
        }

        try
        {
            var decoded = await FirebaseAuth.GetAuth(app).VerifyIdTokenAsync(idToken);
            return ToUserInfo(decoded);
        }
        catch (FirebaseAuthException ex)
        {
            _logger.LogWarning("Firebase ID token rejected: {Reason} ({Code})", ex.Message, ex.AuthErrorCode);
            return null;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Unexpected error verifying Firebase ID token");
            return null;
        }
    }

    private static FirebaseUserInfo ToUserInfo(FirebaseToken decoded)
    {
        var email = Claim(decoded, "email") ?? string.Empty;
        var name = Claim(decoded, "name") ?? (email.Contains('@') ? email.Split('@')[0] : decoded.Uid);
        var picture = Claim(decoded, "picture");
        return new FirebaseUserInfo(decoded.Uid, email, name, picture);
    }

    private static string? Claim(FirebaseToken token, string name) =>
        token.Claims.TryGetValue(name, out var value) ? value?.ToString() : null;

    /// <summary>
    /// Builds the singleton <see cref="FirebaseApp"/> on first use. Credentials
    /// come from the same <c>FIREBASE_CREDENTIALS_JSON</c> environment variable
    /// (or <c>Firebase:CredentialsJson</c> setting) that Firestore uses, falling
    /// back to Application Default Credentials. Only one attempt is made per
    /// process so a misconfigured host does not retry on every request.
    /// </summary>
    private FirebaseApp? GetOrCreateApp()
    {
        if (_app != null) return _app;

        lock (InitLock)
        {
            if (_app != null || _initialisationAttempted) return _app;
            _initialisationAttempted = true;

            try
            {
                var credentialsJson = Environment.GetEnvironmentVariable("FIREBASE_CREDENTIALS_JSON") ??
                                      _config["Firebase:CredentialsJson"];
                var credential = string.IsNullOrWhiteSpace(credentialsJson)
                    ? GoogleCredential.GetApplicationDefault()
                    : GoogleCredential.FromJson(credentialsJson);

                _app = FirebaseApp.DefaultInstance ?? FirebaseApp.Create(new AppOptions
                {
                    Credential = credential,
                    ProjectId = _config["Firebase:ProjectId"] ?? "pulsesync-ede3e"
                });
                _logger.LogInformation("Firebase Admin SDK initialised for project '{ProjectId}'", _app.Options.ProjectId);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Firebase Admin SDK could not be initialised; ID token verification is unavailable");
                _app = null;
            }

            return _app;
        }
    }
}
