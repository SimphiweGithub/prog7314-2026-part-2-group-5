package com.divitiae.pulsesync.data.auth

/**
 * Code Attribution No 47
 * This method was taken from "Handle 401 responses with OkHttp interceptors"
 * https://square.github.io/okhttp/features/interceptors/
 * Square, Inc.
 */

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Single place that decides what happens when the API rejects the stored
 * access token. The OkHttp layer calls [onUnauthorized] (from a background
 * thread) whenever a request that carried a Bearer token came back 401; the
 * token store is wiped so no further requests go out with the dead token, and
 * [sessionExpired] flips to `true` so the UI can route back to Sign In.
 *
 * Several requests can fail at once, so the flag is set with compare-and-set
 * and the store is cleared only once; [acknowledgeExpiry] resets it after the
 * UI has navigated.
 */
class SessionManager(
    private val tokenStore: AuthTokenStore,
    private val scope: CoroutineScope,
) {
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    /** Called by [com.divitiae.pulsesync.data.remote.AuthInterceptor] on a 401 for a request that sent a token. */
    fun onUnauthorized() {
        if (tokenStore.accessToken == null) {
            Log.d(TAG, "onUnauthorized: no stored session to expire; ignoring")
            return
        }
        if (!_sessionExpired.compareAndSet(expect = false, update = true)) {
            Log.d(TAG, "onUnauthorized: expiry already in progress; ignoring duplicate")
            return
        }
        Log.w(TAG, "onUnauthorized: server rejected the stored access token; clearing session")
        scope.launch { tokenStore.clear() }
    }

    /** The UI has shown Sign In; clear the flag so a later expiry is a fresh event. */
    fun acknowledgeExpiry() {
        _sessionExpired.value = false
    }

    companion object {
        private const val TAG = "SessionManager"
    }
}
