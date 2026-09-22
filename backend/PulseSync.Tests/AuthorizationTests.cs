/*
 * Code Attribution No 46
 * This method was taken from "Integration tests in ASP.NET Core"
 * https://learn.microsoft.com/en-us/aspnet/core/test/integration-tests
 * Microsoft Learn
 */

using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using FluentAssertions;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.AspNetCore.TestHost;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using PulseSync.Api.Models;
using PulseSync.Api.Services;
using Xunit;

namespace PulseSync.Tests;

/// <summary>
/// Drives the real HTTP pipeline (JWT bearer middleware + [Authorize]) through
/// an in-process test server, so these assertions cover exactly what the
/// Android client sees on the wire. Firestore is swapped for the in-memory
/// store so no credentials are needed.
/// </summary>
public class AuthorizationTests : IClassFixture<AuthorizationTests.ApiFactory>
{
    public class ApiFactory : WebApplicationFactory<Program>
    {
        protected override void ConfigureWebHost(IWebHostBuilder builder)
        {
            builder.ConfigureTestServices(services =>
            {
                services.RemoveAll<IDataStore>();
                services.AddSingleton<IDataStore, InMemoryDataStore>();
                services.AddTransient<IStartupFilter, ResponseBufferingStartupFilter>();
            });
        }
    }

    /// <summary>
    /// Only active when the net8.0 test host is rolled forward onto a newer
    /// runtime (a machine without the .NET 8 runtime, DOTNET_ROLL_FORWARD=Major).
    /// There the newer System.Text.Json refuses the .NET 8 TestServer response
    /// pipe, so the body is written to a plain stream and copied across instead.
    /// On .NET 8 (CI) this filter is a no-op.
    /// </summary>
    private sealed class ResponseBufferingStartupFilter : IStartupFilter
    {
        public Action<IApplicationBuilder> Configure(Action<IApplicationBuilder> next)
        {
            if (Environment.Version.Major == 8) return next;

            return app =>
            {
                app.Use(async (context, pipeline) =>
                {
                    var original = context.Response.Body;
                    await using var buffer = new MemoryStream();
                    context.Response.Body = buffer;
                    await pipeline();
                    buffer.Position = 0;
                    await buffer.CopyToAsync(original);
                });
                next(app);
            };
        }
    }

    private readonly ApiFactory _factory;

    public AuthorizationTests(ApiFactory factory)
    {
        _factory = factory;
    }

    private HttpClient ClientFor(string? userId)
    {
        var client = _factory.CreateClient();
        if (userId != null)
        {
            var tokens = _factory.Services.GetRequiredService<ITokenService>();
            var jwt = tokens.GenerateAccessToken(userId, $"{userId}@pulsesync.ac.za");
            client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", jwt);
        }
        return client;
    }

    [Theory]
    [InlineData("/api/v1/notes")]
    [InlineData("/api/v1/preferences")]
    [InlineData("/api/v1/keywords")]
    [InlineData("/api/v1/users/me")]
    [InlineData("/api/v1/downloads")]
    public async Task PerUserEndpoints_WithoutBearerToken_Return401(string path)
    {
        var response = await ClientFor(null).GetAsync(path);

        response.StatusCode.Should().Be(HttpStatusCode.Unauthorized);
        response.Headers.WwwAuthenticate.Should().Contain(h => h.Scheme == "Bearer");
    }

    [Fact]
    public async Task PerUserEndpoint_WithTamperedToken_Returns401()
    {
        var client = _factory.CreateClient();
        var forged = ClientFor("victim").DefaultRequestHeaders.Authorization!.Parameter!;
        // Flip the last characters of the signature segment.
        forged = forged[..^4] + "AAAA";
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", forged);

        var response = await client.GetAsync("/api/v1/notes");

        response.StatusCode.Should().Be(HttpStatusCode.Unauthorized);
    }

    [Theory]
    [InlineData("/api/v1/articles")]
    [InlineData("/api/v1/categories")]
    public async Task PublicFeedEndpoints_WithoutBearerToken_Return200(string path)
    {
        var response = await ClientFor(null).GetAsync(path);

        response.StatusCode.Should().Be(HttpStatusCode.OK);
    }

    [Fact]
    public async Task Notes_AreScopedToTheTokenSubject()
    {
        var alice = ClientFor("alice");
        var bob = ClientFor("bob");

        var create = await alice.PostAsJsonAsync("/api/v1/notes", new CreateNoteRequestDto(
            LocalId: "local-1",
            ArticleId: "nsfas-2027",
            Title: "Alice's private note",
            BodyHtml: "<p>secret</p>"));
        create.StatusCode.Should().Be(HttpStatusCode.Created);

        var aliceNotes = await alice.GetFromJsonAsync<List<NoteDto>>("/api/v1/notes");
        var bobNotes = await bob.GetFromJsonAsync<List<NoteDto>>("/api/v1/notes");

        aliceNotes.Should().ContainSingle(n => n.Title == "Alice's private note");
        bobNotes.Should().BeEmpty();
    }

    [Fact]
    public async Task Logout_WithoutBearerToken_Returns401()
    {
        var response = await ClientFor(null).PostAsync("/api/v1/auth/logout", null);

        response.StatusCode.Should().Be(HttpStatusCode.Unauthorized);
    }
}
