# Part 2 — Data Layer Plan (Member 3)

**Author:** Simphiwe Khumalo (ST10451674) — Back-End & Data Architect
**Branch:** `feat/data-layer`
**Scope:** Android client data layer only. The ASP.NET Core REST API is a separate backend project, not in this repo.

---

## Guiding principle

Everything new lives under a new `com.divitiae.pulsesync.data` package. This plan does **not** touch UI screens, theme, or ViewModels — those are Member 2's and Member 4's lanes. The data layer exposes repositories with clean `suspend`/`Flow` APIs that Member 4's ViewModels call, plus `domain → UI` mappers so wiring to the existing `ArticleUi` / `SettingsUiState` contracts is trivial.

## Package structure

```
data/
├── domain/        Article, AiSummary, ResourceLink, Note, Keyword,
│                  Category, NotificationItem, UserProfile, Result<T>
├── remote/        PulseSyncApi (Retrofit), dto/, ApiClient, interceptors
├── local/         PulseSyncDatabase, entity/, dao/, Converters, Note FTS
├── repository/    ArticleRepo, NoteRepo, KeywordRepo, CategoryRepo,
│                  AuthRepo, NotificationRepo, DownloadRepo, PreferencesRepo
├── sync/          NetworkMonitor, SyncWorker (WorkManager)
├── auth/          FirebaseAuth + Google Sign-In wrapper
└── di/            AppContainer (manual DI — no Hilt)
```

## Offline-first behaviour

Room is the source of truth. Repositories emit `Flow` from Room; a refresh call pulls from Retrofit and updates Room. On first run (or when the API is unreachable) the repository **seeds Room from the existing `FeedSampleData`**, so the app runs and demos immediately. The live Render URL sits behind a single constant — flip it on when the API is deployed, with no other code changes.

## Dependencies to add

Added to `gradle/libs.versions.toml` and `app/build.gradle.kts`:

- Retrofit + Gson converter, OkHttp logging interceptor
- Room (runtime, ktx) + KSP plugin for the compiler
- DataStore (preferences)
- WorkManager
- kotlinx-coroutines
- Firebase BOM (Auth + Messaging + Firestore) + Play Services Auth

## Build order — one commit each

1. Gradle dependencies + KSP plugin
2. Domain models + `Result`
3. Room (entities, DAOs, converters, database, Note FTS)
4. Retrofit API + DTOs + client + DTO↔domain mappers
5. Repositories (offline-first, seed from sample data)
6. DataStore preferences repository
7. Firebase Auth + Google SSO wrapper
8. WorkManager sync worker + network monitor
9. `AppContainer` wiring + domain→UI mappers

## Verification

Run `./gradlew assembleDebug` after the dependency-heavy commits and at the end. The Android SDK is present locally, so "it compiles" is a real check. The existing `SmokeTest` must stay green.

## Requires the user (code built, left with clear TODOs)

- **Firebase:** create the Firebase project and add `google-services.json` to `app/`. SSO cannot run without it.
- **API URL:** confirm the deployed Render base URL once live. Placeholder used: `https://pulsesync-api.onrender.com/api/v1/`.

## Open decision

Manual DI (`AppContainer`) is the default — small footprint, no extra library. If the team has standardised on **Hilt**, switch before step 5, since retrofitting DI later touches every repository.

## Alignment with existing UI contracts

- `ui/feed/FeedModels.kt` — `ArticleUi`, `NoteUi`, `ResourceLinkUi`, `Sentiment`, `SummaryMode`
- `ui/settings/SettingsModels.kt` — `SettingsUiState`, `ThemeMode`, `TopicToggle`
- `ui/navigation/PulseSyncNavHost.kt` — auth gating + article resolution TODOs that repositories will satisfy
