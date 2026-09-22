/*
 * Code Attribution No 40
 * This method was taken from "Unit Testing in ASP.NET Core Web API Using xUnit and Moq"
 * https://www.c-sharpcorner.com/article/unit-testing-in-asp-net-core-web-api-using-xunit-and-moq/
 * C# Corner
 */

using FluentAssertions;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using Moq;
using PulseSync.Api.Controllers;
using PulseSync.Api.Models;
using PulseSync.Api.Services;
using Xunit;

namespace PulseSync.Tests;

public class AuthControllerTests
{
    private readonly Mock<IFirebaseAuthService> _firebaseAuthMock;
    private readonly Mock<ITokenService> _tokenServiceMock;
    private readonly IDataStore _dataStore;
    private readonly Mock<ILogger<AuthController>> _loggerMock;
    private readonly AuthController _controller;

    public AuthControllerTests()
    {
        _firebaseAuthMock = new Mock<IFirebaseAuthService>();
        _tokenServiceMock = new Mock<ITokenService>();
        _dataStore = new InMemoryDataStore();
        _loggerMock = new Mock<ILogger<AuthController>>();

        _controller = new AuthController(
            _firebaseAuthMock.Object,
            _tokenServiceMock.Object,
            _dataStore,
            _loggerMock.Object
        );
    }

    [Fact]
    public async Task ExchangeGoogleToken_WithEmptyToken_ReturnsBadRequest()
    {
        // Arrange
        var request = new GoogleSsoRequestDto(string.Empty);

        // Act
        var result = await _controller.ExchangeGoogleToken(request);

        // Assert
        result.Result.Should().BeOfType<BadRequestObjectResult>();
    }

    [Fact]
    public async Task ExchangeGoogleToken_WhenFirebaseVerificationFails_ReturnsUnauthorized()
    {
        // Arrange
        var request = new GoogleSsoRequestDto("invalid_or_expired_token");
        _firebaseAuthMock
            .Setup(s => s.VerifyIdTokenAsync("invalid_or_expired_token"))
            .ReturnsAsync((FirebaseUserInfo?)null);

        // Act
        var result = await _controller.ExchangeGoogleToken(request);

        // Assert
        result.Result.Should().BeOfType<UnauthorizedObjectResult>();
    }

    [Fact]
    public async Task ExchangeGoogleToken_WithValidToken_Returns200WithJwt()
    {
        // Arrange
        var request = new GoogleSsoRequestDto("valid_firebase_token", PreferredLanguage: "zu");
        var userInfo = new FirebaseUserInfo(
            Uid: "test_uid_12345",
            Email: "student@wits.ac.za",
            DisplayName: "Thabo Mbeki",
            PhotoUrl: "https://lh3.googleusercontent.com/photo.png"
        );

        _firebaseAuthMock
            .Setup(s => s.VerifyIdTokenAsync("valid_firebase_token"))
            .ReturnsAsync(userInfo);

        _tokenServiceMock
            .Setup(s => s.GenerateAccessToken("test_uid_12345", "student@wits.ac.za"))
            .Returns("header.payload.signature_jwt");

        _tokenServiceMock
            .Setup(s => s.GenerateRefreshToken())
            .Returns("secure_refresh_token_xyz");

        // Act
        var result = await _controller.ExchangeGoogleToken(request);

        // Assert
        result.Result.Should().BeOfType<OkObjectResult>();
        var okResult = result.Result as OkObjectResult;
        var response = okResult!.Value as AuthResponseDto;

        response.Should().NotBeNull();
        response!.UserId.Should().Be("test_uid_12345");
        response.AccessToken.Should().Be("header.payload.signature_jwt");
        response.RefreshToken.Should().Be("secure_refresh_token_xyz");
        response.IsNewUser.Should().BeTrue();

        // Verify persisted user profile in datastore
        var persisted = _dataStore.GetUser("test_uid_12345");
        persisted.Should().NotBeNull();
        persisted!.DisplayName.Should().Be("Thabo Mbeki");
        persisted.Email.Should().Be("student@wits.ac.za");
    }

    [Fact]
    public void Refresh_WithEmptyToken_ReturnsBadRequest()
    {
        var result = _controller.Refresh(new RefreshRequestDto(string.Empty));

        result.Result.Should().BeOfType<BadRequestObjectResult>();
    }

    [Fact]
    public void Refresh_WithUnknownToken_ReturnsUnauthorized()
    {
        var result = _controller.Refresh(new RefreshRequestDto("never-issued"));

        result.Result.Should().BeOfType<UnauthorizedObjectResult>();
    }

    [Fact]
    public void Refresh_WithStoredToken_RotatesPairForThatUser()
    {
        // Arrange: a user who signed in earlier and holds refresh token "rt-1".
        _dataStore.UpsertUser(new UserDto(UserId: "uid-42", Email: "s42@wits.ac.za", DisplayName: "Student 42"));
        _dataStore.SaveRefreshToken("uid-42", "rt-1");
        _tokenServiceMock
            .Setup(s => s.GenerateAccessToken("uid-42", "s42@wits.ac.za"))
            .Returns("access-for-uid-42");
        _tokenServiceMock
            .Setup(s => s.GenerateRefreshToken())
            .Returns("rt-2");

        // Act
        var result = _controller.Refresh(new RefreshRequestDto("rt-1"));

        // Assert: the pair belongs to the token's owner, not a placeholder user.
        result.Result.Should().BeOfType<OkObjectResult>();
        var response = (result.Result as OkObjectResult)!.Value as AuthResponseDto;
        response!.UserId.Should().Be("uid-42");
        response.AccessToken.Should().Be("access-for-uid-42");
        response.RefreshToken.Should().Be("rt-2");
        _dataStore.GetRefreshToken("uid-42").Should().Be("rt-2");

        // Replaying the consumed token must fail.
        var replay = _controller.Refresh(new RefreshRequestDto("rt-1"));
        replay.Result.Should().BeOfType<UnauthorizedObjectResult>();
    }

    [Fact]
    public void Refresh_AfterLogout_ReturnsUnauthorized()
    {
        _dataStore.SaveRefreshToken("uid-7", "rt-7");
        _controller.SignedInAs("uid-7");
        _controller.Logout();

        var result = _controller.Refresh(new RefreshRequestDto("rt-7"));

        result.Result.Should().BeOfType<UnauthorizedObjectResult>();
    }

    [Fact]
    public void Logout_WhenAuthenticated_RevokesRefreshTokenAndReturnsOk()
    {
        // Arrange
        _dataStore.SaveRefreshToken("test_uid_12345", "refresh_to_revoke");
        _controller.SignedInAs("test_uid_12345");

        // Act
        var result = _controller.Logout();

        // Assert
        result.Should().BeOfType<OkObjectResult>();
        _dataStore.GetRefreshToken("test_uid_12345").Should().BeNull();
    }

    [Fact]
    public void Logout_WithoutIdentity_ReturnsUnauthorized()
    {
        // Act
        var result = _controller.Logout();

        // Assert
        result.Should().BeOfType<UnauthorizedObjectResult>();
    }
}
