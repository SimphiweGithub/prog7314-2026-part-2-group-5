/*
 * Code Attribution No 49
 * This method was taken from "Verify ID Tokens using Firebase Admin SDK"
 * https://firebase.google.com/docs/auth/admin/verify-id-tokens
 * Google Firebase
 */

using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using FluentAssertions;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.IdentityModel.Tokens;
using PulseSync.Api.Services;
using Xunit;

namespace PulseSync.Tests;

/// <summary>
/// The service must never accept a token on the strength of its claims alone.
/// These cases use a self-signed token that carries a perfectly plausible
/// Firebase payload; before this change it would have been "verified".
/// </summary>
public class FirebaseAuthServiceTests
{
    private static FirebaseAuthService CreateService()
    {
        // A real RSA key that Google has never seen: enough to build the Admin
        // app offline, never enough to make a forged token verify. JSON needs
        // the PEM's line breaks written as the two characters backslash + n.
        using var rsa = RSA.Create(2048);
        var pemLines = rsa.ExportPkcs8PrivateKeyPem()
            .Split('\n', StringSplitOptions.RemoveEmptyEntries)
            .Select(line => line.TrimEnd('\r'));
        var privateKeyPem = string.Join("\\n", pemLines);

        var fakeServiceAccount = $$"""
        {
          "type": "service_account",
          "project_id": "pulsesync-test",
          "private_key_id": "test",
          "private_key": "{{privateKeyPem}}",
          "client_email": "test@pulsesync-test.iam.gserviceaccount.com",
          "client_id": "0",
          "token_uri": "https://oauth2.googleapis.com/token"
        }
        """;

        var config = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["Firebase:ProjectId"] = "pulsesync-test",
                ["Firebase:CredentialsJson"] = fakeServiceAccount
            })
            .Build();
        return new FirebaseAuthService(config, NullLogger<FirebaseAuthService>.Instance);
    }

    /// <summary>A token whose claims look exactly like Firebase's, signed with a key Google never issued.</summary>
    private static string ForgeFirebaseLookingToken()
    {
        var key = new SymmetricSecurityKey(System.Text.Encoding.UTF8.GetBytes("attacker-controlled-key-material-32b!"));
        var token = new JwtSecurityToken(
            issuer: "https://securetoken.google.com/pulsesync-test",
            audience: "pulsesync-test",
            claims: new[]
            {
                new Claim("sub", "victim-uid"),
                new Claim("user_id", "victim-uid"),
                new Claim("email", "victim@wits.ac.za"),
                new Claim("name", "Victim")
            },
            expires: DateTime.UtcNow.AddHours(1),
            signingCredentials: new SigningCredentials(key, SecurityAlgorithms.HmacSha256));
        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    [Fact]
    public async Task VerifyIdTokenAsync_WithBlankToken_ReturnsNull()
    {
        var result = await CreateService().VerifyIdTokenAsync("   ");

        result.Should().BeNull();
    }

    [Fact]
    public async Task VerifyIdTokenAsync_WithMalformedToken_ReturnsNull()
    {
        var result = await CreateService().VerifyIdTokenAsync("not-a-jwt");

        result.Should().BeNull();
    }

    [Fact]
    public async Task VerifyIdTokenAsync_WithForgedSignature_ReturnsNullInsteadOfTrustingClaims()
    {
        var forged = ForgeFirebaseLookingToken();

        var result = await CreateService().VerifyIdTokenAsync(forged);

        result.Should().BeNull("a token must be rejected unless its signature was produced by Firebase");
    }
}
