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
