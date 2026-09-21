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
    public void Logout_ReturnsOk()
    {
        // Act
        var result = _controller.Logout();

        // Assert
        result.Should().BeOfType<OkObjectResult>();
    }
}
