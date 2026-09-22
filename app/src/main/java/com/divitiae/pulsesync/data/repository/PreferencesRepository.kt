package com.divitiae.pulsesync.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.divitiae.pulsesync.data.domain.AppError
import com.divitiae.pulsesync.data.domain.FontSizePref
import com.divitiae.pulsesync.data.domain.Result
import com.divitiae.pulsesync.data.domain.SummaryModePref
import com.divitiae.pulsesync.data.domain.ThemePref
import com.divitiae.pulsesync.data.domain.UserPreferences
import com.divitiae.pulsesync.data.remote.PulseSyncApi
import com.divitiae.pulsesync.data.remote.dto.PreferencesDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "pulsesync_prefs")

/**
 * DataStore-backed user preferences — the single source for settings state.
 *
 * The on-device copy is always written first so Settings works offline; a
 * change is then mirrored to `PUT /api/v1/preferences` via [pushToCloud].
 * [apiProvider] is a lambda (rather than the API itself) because the API
 * client depends on this repository for its Accept-Language header.
 */
class PreferencesRepository(
    private val context: Context,
    private val apiProvider: () -> PulseSyncApi? = { null },
) {

    private object Keys {
        val summaryMode = stringPreferencesKey("default_summary_mode")
        val language = stringPreferencesKey("language")
        val biometric = booleanPreferencesKey("biometric_lock")
        val theme = stringPreferencesKey("theme_mode")
        val fontSize = stringPreferencesKey("font_size")
        val fontType = stringPreferencesKey("font_type")
        val highContrast = booleanPreferencesKey("high_contrast")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { p ->
        UserPreferences(
            defaultSummaryMode = p[Keys.summaryMode]
                ?.let { runCatching { SummaryModePref.valueOf(it) }.getOrNull() }
                ?: SummaryModePref.DETAILED,
            language = p[Keys.language] ?: "English",
            biometricLock = p[Keys.biometric] ?: false,
            themeMode = p[Keys.theme]
                ?.let { runCatching { ThemePref.valueOf(it) }.getOrNull() }
                ?: ThemePref.SYSTEM,
            fontSize = p[Keys.fontSize]
                ?.let { runCatching { FontSizePref.valueOf(it) }.getOrNull() }
                ?: FontSizePref.MEDIUM,
            fontType = p[Keys.fontType] ?: "Default",
            highContrast = p[Keys.highContrast] ?: false,
        )
    }

    suspend fun setSummaryMode(mode: SummaryModePref) =
        edit { it[Keys.summaryMode] = mode.name }

    suspend fun setLanguage(language: String) =
        edit { it[Keys.language] = language }

    suspend fun setBiometricLock(enabled: Boolean) =
        edit { it[Keys.biometric] = enabled }

    suspend fun setThemeMode(mode: ThemePref) =
        edit { it[Keys.theme] = mode.name }

    suspend fun setFontSize(size: FontSizePref) =
        edit { it[Keys.fontSize] = size.name }

    suspend fun setFontType(type: String) =
        edit { it[Keys.fontType] = type }

    suspend fun setHighContrast(enabled: Boolean) =
        edit { it[Keys.highContrast] = enabled }

    /**
     * Mirrors the current on-device preferences to the cloud
     * (`PUT /api/v1/preferences`). The local copy is already saved by the time
     * this runs, so a failure here only means the cloud copy is stale; the
     * caller decides whether to tell the user.
     */
    suspend fun pushToCloud(): Result<Unit> {
        val api = apiProvider()
            ?: return Result.Failure(AppError.Network("API client not initialised"))
        val current = preferences.first()
        Log.d(
            TAG,
            "pushToCloud: PUT /preferences (summary=${current.defaultSummaryMode}, " +
                "language=${current.languageTag}, biometric=${current.biometricLock})",
        )
        return when (val result = safeApiCall { api.updatePreferences(current.toDto()) }) {
            is Result.Success -> {
                Log.i(TAG, "pushToCloud: preferences synced to cloud")
                Result.Success(Unit)
            }

            is Result.Failure -> {
                Log.w(TAG, "pushToCloud: cloud sync failed, device copy kept: ${result.error}")
                result
            }
        }
    }

    /**
     * Synchronous read of the language tag for the OkHttp interceptor, which
     * runs off the main thread. Falls back to English if the store is empty.
     */
    fun languageTagBlocking(): String =
        runCatching { runBlocking { preferences.first().languageTag } }.getOrDefault("en")

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    companion object {
        private const val TAG = "PreferencesRepository"
    }
}

/** Only the fields the API stores; accessibility settings stay on the device. */
fun UserPreferences.toDto(): PreferencesDto = PreferencesDto(
    defaultSummaryMode = defaultSummaryMode.name,
    language = languageTag,
    biometricEnabled = biometricLock,
)
