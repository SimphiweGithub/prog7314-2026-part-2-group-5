# ---------------------------------------------------------------------
# CODE ATTRIBUTION
# ---------------------------------------------------------------------
# Author: Simphiwe Khumalo (ST10451674) - Member 3: Back-End & Data Architect
# Assisted by: Antigravity AI Coding Assistant (Google DeepMind)
#
# The multi-stage build optimization and ASP.NET Core 8 container configuration were adapted from:
#
# C# Corner (2024) Dockerizing an ASP.NET Core Web API: A Step-by-Step Guide. [online]
# Available at: https://www.c-sharpcorner.com/article/dockerizing-an-asp-net-core-web-api/
# [Accessed 21 September 2026].
#
# Render (2024) Deploy a Docker Web Service on Render. [online]
# Available at: https://render.com/docs/docker
# [Accessed 21 September 2026].
#
# Google DeepMind Antigravity (2026) Multi-Stage Dockerfile & Render Port 8080 Configuration.
# ---------------------------------------------------------------------

# Multi-stage Dockerfile for PulseSync ASP.NET Core 8 Web API
# Optimized for Render Cloud Hosting (https://pulsesync-api.onrender.com/)

FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS base
WORKDIR /app
EXPOSE 8080
ENV ASPNETCORE_HTTP_PORTS=8080
ENV ASPNETCORE_ENVIRONMENT=Production

FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src

# Copy project files and restore dependencies
COPY ["PulseSync.Api/PulseSync.Api.csproj", "PulseSync.Api/"]
RUN dotnet restore "PulseSync.Api/PulseSync.Api.csproj"

# Copy remaining source code and publish
COPY . .
WORKDIR "/src/PulseSync.Api"
RUN dotnet publish "PulseSync.Api.csproj" -c Release -o /app/publish /p:UseAppHost=false

FROM base AS final
WORKDIR /app
COPY --from=build /app/publish .
ENTRYPOINT ["dotnet", "PulseSync.Api.dll"]
