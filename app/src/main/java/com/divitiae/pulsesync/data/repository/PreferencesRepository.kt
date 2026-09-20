package com.divitiae.pulsesync.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.divitiae.pulsesync.data.domain.FontSizePref
import com.divitiae.pulsesync.data.domain.SummaryModePref
import com.divitiae.pulsesync.data.domain.ThemePref
import com.divitiae.pulsesync.data.domain.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "pulsesync_prefs")

/** DataStore-backed user preferences — the single source for settings state. */
class PreferencesRepository(private val context: Context) {

    private object Keys {
        val summaryMode = stringPreferencesKey("default_summary_mode")
        val language = stringPreferencesKey("language")
        val biometric = booleanPreferencesKey("biometric_lock")
        val theme = stringPreferencesKey("theme_mode")
        val fontSize = stringPreferencesKey("font_size")
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

    suspend fun setHighContrast(enabled: Boolean) =
        edit { it[Keys.highContrast] = enabled }

    /**
     * Synchronous read of the language tag for the OkHttp interceptor, which
     * runs off the main thread. Falls back to English if the store is empty.
     */
    fun languageTagBlocking(): String =
        runCatching { runBlocking { preferences.first().languageTag } }.getOrDefault("en")

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
