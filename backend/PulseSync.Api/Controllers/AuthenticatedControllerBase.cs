/*
 * Code Attribution No 44
 * This method was taken from "Simple authorization in ASP.NET Core"
 * https://learn.microsoft.com/en-us/aspnet/core/security/authorization/simple
 * Microsoft Learn
 */

using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;

namespace PulseSync.Api.Controllers;

/// <summary>
/// Base for every per-user endpoint. <see cref="AuthorizeAttribute"/> makes the
/// JWT bearer middleware reject any request without a valid PulseSync access
/// token (HTTP 401) before the action runs, and <see cref="CurrentUserId"/> is
/// taken from the token's subject claim so a caller can only ever read or
/// write their own notes, keywords, preferences and download slots.
/// </summary>
[Authorize]
[UnauthorizedAccessExceptionFilter]
public abstract class AuthenticatedControllerBase : ControllerBase
{
    /// <summary>
    /// The authenticated user's id. Throws <see cref="UnauthorizedAccessException"/>
    /// (mapped to 401 by <see cref="UnauthorizedAccessExceptionFilterAttribute"/>)
    /// when no identity claim is present, so there is no anonymous fallback user.
    /// </summary>
    protected string CurrentUserId =>
        User?.FindFirstValue(ClaimTypes.NameIdentifier) ??
        User?.FindFirstValue(JwtRegisteredClaimNames.Sub) ??
        User?.FindFirstValue("userId") ??
        throw new UnauthorizedAccessException("The request does not carry an authenticated user identity.");
}

/// <summary>
/// Turns an <see cref="UnauthorizedAccessException"/> raised inside an action
/// into a 401 response instead of an unhandled 500.
/// </summary>
[AttributeUsage(AttributeTargets.Class | AttributeTargets.Method)]
public sealed class UnauthorizedAccessExceptionFilterAttribute : ExceptionFilterAttribute
{
    public override void OnException(ExceptionContext context)
    {
        if (context.Exception is UnauthorizedAccessException ex)
        {
            context.Result = new UnauthorizedObjectResult(new { message = ex.Message });
            context.ExceptionHandled = true;
        }
    }
}
