package com.divitiae.pulsesync.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "pulsesync_auth")

/**
 * REPLACES AuthTokenStore (same public API, so AuthRepository and
 * AppContainer compile unchanged).
 *
 * Persists the PulseSync JWT pair **encrypted at rest** with an Android
 * Keystore AES-GCM key (see [TokenCipher]). [accessToken] is also cached in
 * memory so the OkHttp interceptor can read it synchronously on a background
 * thread without touching the keystore on every request.
 *
 * Migration note: the DataStore file name is unchanged but the key names are
 * new (`*_enc`), so any plaintext token written by the old implementation is
 * simply ignored and the user signs in once more.
 */
class AuthTokenStore(
    private val context: Context,
    private val cipher: TokenCipher = TokenCipher(),
) {

    private object Keys {
        val access = stringPreferencesKey("access_token_enc")
        val refresh = stringPreferencesKey("refresh_token_enc")
    }

    @Volatile
    var accessToken: String? = null
        private set

    /** Emits true whenever an access token is on disk — drives "already signed in" routing. */
    val isSignedIn: Flow<Boolean> =
        context.authDataStore.data.map { prefs -> prefs[Keys.access]?.let(cipher::decrypt) != null }

    /** Warm the in-memory cache from disk at startup (called by AppContainer.initialise). */
    suspend fun load() {
        accessToken = context.authDataStore.data
            .map { prefs -> prefs[Keys.access]?.let(cipher::decrypt) }
            .first()
    }

    suspend fun save(access: String, refresh: String) {
        // Encrypt before the edit block so a keystore failure never leaves a half-written pair.
        val encAccess = cipher.encrypt(access)
        val encRefresh = cipher.encrypt(refresh)
        accessToken = access
        context.authDataStore.edit {
            it[Keys.access] = encAccess
            it[Keys.refresh] = encRefresh
        }
    }

    suspend fun refreshToken(): String? =
        context.authDataStore.data
            .map { prefs -> prefs[Keys.refresh]?.let(cipher::decrypt) }
            .first()

    suspend fun clear() {
        accessToken = null
        context.authDataStore.edit { it.clear() }
    }
}