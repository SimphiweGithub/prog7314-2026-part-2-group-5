/*
 * ---------------------------------------------------------------------
 * CODE ATTRIBUTION
 * ---------------------------------------------------------------------
 * Author: Simphiwe Khumalo (ST10451674) - Member 3: Back-End & Data Architect
 * Assisted by: Antigravity AI Coding Assistant (Google DeepMind)
 *
 * The ASP.NET Core 8 Web API service configuration, JWT Bearer middleware,
 * Swagger/OpenAPI setup and CORS configuration in this file were adapted from:
 *
 * C# Corner (2024) JWT Authentication and Authorization in ASP.NET Core Web API. [online]
 * Available at: https://www.c-sharpcorner.com/article/jwt-authentication-and-authorization-in-net-core-web-api/
 * [Accessed 21 September 2026].
 *
 * Microsoft Learn (2024) Overview of ASP.NET Core middleware. [online]
 * Available at: https://learn.microsoft.com/en-us/aspnet/core/fundamentals/middleware/
 * [Accessed 21 September 2026].
 *
 * Google DeepMind Antigravity (2026) AI Pair Programming and REST API Architecture Scaffolding.
 * ---------------------------------------------------------------------
 */

using System.Text.Json;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;
using PulseSync.Api.Services;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase;
        options.JsonSerializerOptions.DefaultIgnoreCondition = System.Text.Json.Serialization.JsonIgnoreCondition.WhenWritingNull;
    });

// Core singleton data store and services
builder.Services.AddSingleton<IDataStore, InMemoryDataStore>();
builder.Services.AddSingleton<ITokenService, TokenService>();
builder.Services.AddScoped<IFirebaseAuthService, FirebaseAuthService>();

// CORS configuration (allow Android client requests)
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
    {
        policy.AllowAnyOrigin()
              .AllowAnyMethod()
              .AllowAnyHeader();
    });
});

// Swagger / OpenAPI documentation
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new OpenApiInfo
    {
        Title = "PulseSync REST API",
        Version = "v1",
        Description = "ASP.NET Core 8 Web API powering the PulseSync Android Client (PROG7314, Group 5, Member 3)."
    });

    c.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        Description = "JWT Authorization header using the Bearer scheme. Example: \"Authorization: Bearer {token}\"",
        Name = "Authorization",
        In = ParameterLocation.Header,
        Type = SecuritySchemeType.ApiKey,
        Scheme = "Bearer"
    });

    c.AddSecurityRequirement(new OpenApiSecurityRequirement
    {
        {
            new OpenApiSecurityScheme
            {
                Reference = new OpenApiReference
                {
                    Type = ReferenceType.SecurityScheme,
                    Id = "Bearer"
                }
            },
            Array.Empty<string>()
        }
    });
});

// JWT Authentication configuration
var jwtSecret = builder.Configuration["Jwt:Secret"] ?? "PulseSync-PROG7314-Group5-SecretKey-For-HmacSha256-Signing-2026!";
builder.Services.AddAuthentication(options =>
{
    options.DefaultAuthenticateScheme = JwtBearerDefaults.AuthenticationScheme;
    options.DefaultChallengeScheme = JwtBearerDefaults.AuthenticationScheme;
})
.AddJwtBearer(options =>
{
    options.RequireHttpsMetadata = false;
    options.SaveToken = true;
    options.TokenValidationParameters = new TokenValidationParameters
    {
        ValidateIssuerSigningKey = true,
        IssuerSigningKey = new SymmetricSecurityKey(System.Text.Encoding.UTF8.GetBytes(jwtSecret)),
        ValidateIssuer = false,
        ValidateAudience = false,
        ClockSkew = TimeSpan.FromMinutes(5)
    };
});

builder.Services.AddAuthorization();

var app = builder.Build();

// Configure the HTTP request pipeline.
app.UseSwagger();
app.UseSwaggerUI(c =>
{
    c.SwaggerEndpoint("/swagger/v1/swagger.json", "PulseSync API v1");
    c.RoutePrefix = string.Empty; // Serves Swagger UI at application root (/)
});

app.UseCors();

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

app.Run();

// Expose Program for integration test web application factory
public partial class Program { }
