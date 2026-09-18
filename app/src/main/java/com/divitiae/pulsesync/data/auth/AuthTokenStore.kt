package com.divitiae.pulsesync.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "pulsesync_auth")

/**
 * Persists the PulseSync JWT pair. [accessToken] is also cached in memory so
 * the OkHttp interceptor can read it synchronously on a background thread.
 */
class AuthTokenStore(private val context: Context) {

    private object Keys {
        val access = stringPreferencesKey("access_token")
        val refresh = stringPreferencesKey("refresh_token")
    }

    @Volatile
    var accessToken: String? = null
        private set

    /** Warm the in-memory cache from disk at startup. */
    suspend fun load() {
        accessToken = context.authDataStore.data.map { it[Keys.access] }.first()
    }

    suspend fun save(access: String, refresh: String) {
        accessToken = access
        context.authDataStore.edit {
            it[Keys.access] = access
            it[Keys.refresh] = refresh
        }
    }

    suspend fun refreshToken(): String? =
        context.authDataStore.data.map { it[Keys.refresh] }.first()

    suspend fun clear() {
        accessToken = null
        context.authDataStore.edit { it.clear() }
    }
}
