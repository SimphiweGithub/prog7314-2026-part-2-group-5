package com.divitiae.pulsesync.data.auth

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
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
        /** Set when Google/Firebase sign-in succeeded but the API exchange did not (no JWT held). */
        val offlineSession = booleanPreferencesKey("offline_session")
    }

    @Volatile
    var accessToken: String? = null
        private set

    /**
     * True while the user is signed in on the device only: Firebase accepted
     * them but no PulseSync JWT was obtained. Requests go out without a Bearer
     * token and the exchange is retried on reconnect. Cleared by [save] and [clear].
     */
    @Volatile
    var isOfflineSession: Boolean = false
        private set

    /** Emits true whenever a session (online or offline) is on disk — drives "already signed in" routing. */
    val isSignedIn: Flow<Boolean> =
        context.authDataStore.data.map { prefs ->
            prefs[Keys.access]?.let(cipher::decrypt) != null || prefs[Keys.offlineSession] == true
        }

    /** Warm the in-memory cache from disk at startup (called by AppContainer.initialise). */
    suspend fun load() {
        Log.d(TAG, "load: warming in-memory token cache from encrypted storage")
        val prefs = context.authDataStore.data.first()
        accessToken = prefs[Keys.access]?.let(cipher::decrypt)
        isOfflineSession = accessToken == null && prefs[Keys.offlineSession] == true
        Log.d(TAG, "load complete: hasAccessToken=${accessToken != null}, offlineSession=$isOfflineSession")
    }

    suspend fun save(access: String, refresh: String) {
        Log.i(TAG, "save: encrypting and saving access and refresh tokens")
        // Encrypt before the edit block so a keystore failure never leaves a half-written pair.
        val encAccess = cipher.encrypt(access)
        val encRefresh = cipher.encrypt(refresh)
        accessToken = access
        isOfflineSession = false
        context.authDataStore.edit {
            it[Keys.access] = encAccess
            it[Keys.refresh] = encRefresh
            it.remove(Keys.offlineSession)
        }
    }

    /** Records a device-only session (see [isOfflineSession]); never stores a token. */
    suspend fun markOfflineSession() {
        Log.w(TAG, "markOfflineSession: signed in without an API token; requests will be unauthenticated until reconnect")
        isOfflineSession = true
        context.authDataStore.edit { it[Keys.offlineSession] = true }
    }

    suspend fun refreshToken(): String? {
        Log.d(TAG, "refreshToken: reading refresh token from storage")
        return context.authDataStore.data
            .map { prefs -> prefs[Keys.refresh]?.let(cipher::decrypt) }
            .first()
    }

    suspend fun clear() {
        Log.i(TAG, "clear: removing stored tokens")
        accessToken = null
        isOfflineSession = false
        context.authDataStore.edit { it.clear() }
    }

    companion object {
        private const val TAG = "AuthTokenStore"
    }
}