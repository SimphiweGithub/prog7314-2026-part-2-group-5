package com.divitiae.pulsesync.data.remote

import com.divitiae.pulsesync.data.remote.dto.ArticleDto
import com.divitiae.pulsesync.data.remote.dto.AuthResponseDto
import com.divitiae.pulsesync.data.remote.dto.CategoryDto
import com.divitiae.pulsesync.data.remote.dto.ClaimSlotRequestDto
import com.divitiae.pulsesync.data.remote.dto.CreateNoteRequestDto
import com.divitiae.pulsesync.data.remote.dto.DownloadSlotsDto
import com.divitiae.pulsesync.data.remote.dto.FcmTokenRequestDto
import com.divitiae.pulsesync.data.remote.dto.FeedResponseDto
import com.divitiae.pulsesync.data.remote.dto.FullTextDto
import com.divitiae.pulsesync.data.remote.dto.GoogleSsoRequestDto
import com.divitiae.pulsesync.data.remote.dto.KeywordDto
import com.divitiae.pulsesync.data.remote.dto.KeywordRequestDto
import com.divitiae.pulsesync.data.remote.dto.NoteDto
import com.divitiae.pulsesync.data.remote.dto.NotificationDto
import com.divitiae.pulsesync.data.remote.dto.PreferencesDto
import com.divitiae.pulsesync.data.remote.dto.RefreshRequestDto
import com.divitiae.pulsesync.data.remote.dto.SyncPullResponseDto
import com.divitiae.pulsesync.data.remote.dto.SyncPushRequestDto
import com.divitiae.pulsesync.data.remote.dto.SyncPushResponseDto
import com.divitiae.pulsesync.data.remote.dto.TagDto
import com.divitiae.pulsesync.data.remote.dto.UpdateNoteRequestDto
import com.divitiae.pulsesync.data.remote.dto.UpdateProfileRequestDto
import com.divitiae.pulsesync.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The PulseSync REST contract consumed by the Android client. Every call is a
 * coroutine; the JWT and Accept-Language header are attached by the OkHttp
 * interceptor in [ApiClient], so they do not appear here.
 */
interface PulseSyncApi {

    // ---- Authentication ----
    @POST("auth/google")
    suspend fun exchangeGoogleToken(@Body body: GoogleSsoRequestDto): Response<AuthResponseDto>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): Response<AuthResponseDto>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    // ---- Profile & preferences ----
    @GET("users/me")
    suspend fun getProfile(): Response<UserDto>

    @PUT("users/me")
    suspend fun updateProfile(@Body body: UpdateProfileRequestDto): Response<UserDto>

    @GET("preferences")
    suspend fun getPreferences(): Response<PreferencesDto>

    @PUT("preferences")
    suspend fun updatePreferences(@Body body: PreferencesDto): Response<PreferencesDto>

    // ---- Feed & articles ----
    @GET("articles")
    suspend fun getFeed(
        @Query("category") category: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): Response<FeedResponseDto>

    @GET("articles/{id}")
    suspend fun getArticle(@Path("id") id: String): Response<ArticleDto>

    @GET("articles/search")
    suspend fun searchArticles(@Query("q") query: String): Response<List<ArticleDto>>

    @GET("articles/{id}/fulltext")
    suspend fun getFullText(@Path("id") id: String): Response<FullTextDto>

    @GET("categories")
    suspend fun getCategories(): Response<List<CategoryDto>>

    // ---- Tracked keywords ----
    @GET("keywords")
    suspend fun getKeywords(): Response<List<KeywordDto>>

    @POST("keywords")
    suspend fun addKeyword(@Body body: KeywordRequestDto): Response<KeywordDto>

    @DELETE("keywords/{id}")
    suspend fun removeKeyword(@Path("id") id: String): Response<Unit>

    // ---- Notes & research vault ----
    @GET("notes")
    suspend fun getNotes(
        @Query("tag") tag: String? = null,
        @Query("q") search: String? = null,
    ): Response<List<NoteDto>>

    @GET("notes/article/{articleId}")
    suspend fun getNotesForArticle(@Path("articleId") articleId: String): Response<List<NoteDto>>

    @POST("notes")
    suspend fun createNote(@Body body: CreateNoteRequestDto): Response<NoteDto>

    @PUT("notes/{id}")
    suspend fun updateNote(
        @Path("id") id: String,
        @Body body: UpdateNoteRequestDto,
    ): Response<NoteDto>

    @DELETE("notes/{id}")
    suspend fun deleteNote(@Path("id") id: String): Response<Unit>

    @GET("notes/tags")
    suspend fun getNoteTags(): Response<List<TagDto>>

    // ---- Offline download manager ----
    @GET("downloads")
    suspend fun getDownloadSlots(): Response<DownloadSlotsDto>

    @POST("downloads")
    suspend fun claimDownloadSlot(@Body body: ClaimSlotRequestDto): Response<DownloadSlotsDto>

    @DELETE("downloads/{articleId}")
    suspend fun releaseDownloadSlot(@Path("articleId") articleId: String): Response<DownloadSlotsDto>

    // ---- Synchronisation ----
    @GET("sync/pull")
    suspend fun pullChanges(@Query("lastSync") lastSyncMillis: Long): Response<SyncPullResponseDto>

    @POST("sync/push")
    suspend fun pushChanges(@Body body: SyncPushRequestDto): Response<SyncPushResponseDto>

    // ---- Notifications ----
    @POST("notifications/token")
    suspend fun registerFcmToken(@Body body: FcmTokenRequestDto): Response<Unit>

    @DELETE("notifications/token")
    suspend fun unregisterFcmToken(): Response<Unit>

    @GET("notifications")
    suspend fun getNotifications(): Response<List<NotificationDto>>

    @PUT("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Response<Unit>
}
