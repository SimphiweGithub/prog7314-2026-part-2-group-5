/*
 * Code Attribution No 45
 * This method was taken from "Unit test controllers in ASP.NET Core"
 * https://learn.microsoft.com/en-us/aspnet/core/mvc/controllers/testing
 * Microsoft Learn
 */

using System.Security.Claims;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;

namespace PulseSync.Tests;

/// <summary>
/// Controllers are invoked directly in the unit tests, so the JWT middleware
/// never runs. This attaches the same claims the middleware would have set
/// after validating a PulseSync access token for <paramref name="userId"/>.
/// </summary>
public static class TestPrincipal
{
    public static T SignedInAs<T>(this T controller, string userId) where T : ControllerBase
    {
        var identity = new ClaimsIdentity(
            new[] { new Claim(ClaimTypes.NameIdentifier, userId), new Claim("userId", userId) },
            authenticationType: "TestJwt");

        controller.ControllerContext = new ControllerContext
        {
            HttpContext = new DefaultHttpContext { User = new ClaimsPrincipal(identity) }
        };
        return controller;
    }
}
