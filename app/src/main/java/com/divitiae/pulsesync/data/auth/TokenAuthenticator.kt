package com.divitiae.pulsesync.data.auth

/**
 * Code Attribution No 50
 * This method was taken from "OkHttp Authenticator: Handling authentication challenges and token refresh"
 * https://square.github.io/okhttp/recipes/#handling-authentication-kt-java
 * Square, Inc.
 */

import android.util.Log
import com.divitiae.pulsesync.data.remote.dto.AuthResponseDto
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Silent token refresh. Access tokens live for two hours; when one is
 * rejected (HTTP 401) this authenticator swaps the stored refresh token for a
 * new pair via `POST /auth/refresh` and replays the request once. Only when
 * that fails does the 401 reach [com.divitiae.pulsesync.data.remote.AuthInterceptor],
 * which then expires the session.
 *
 * Refresh tokens rotate on every use, so concurrent 401s are serialised with
 * a lock: the first caller refreshes, the rest notice the stored access token
 * already differs from the one they sent and simply retry with it.
 *
 * [refreshCall] is a blocking function so this class stays free of Retrofit;
 * the container wires it to a bare API client that carries no Bearer token.
 */
class TokenAuthenticator(
    private val tokenStore: AuthTokenStore,
    private val refreshCall: (refreshToken: String) -> AuthResponseDto?,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val failedRequest = response.request
        val failedHeader = failedRequest.header(HEADER) ?: return null // never sent a token: nothing to refresh
        if (failedRequest.url.encodedPath.contains("/auth/")) return null // sign-in / refresh / logout themselves
        if (response.priorResponse != null) {
            Log.w(TAG, "Replayed request was rejected again; giving up on refresh")
            return null
        }

        synchronized(lock) {
            val current = tokenStore.accessToken
            if (current != null && bearer(current) != failedHeader) {
                Log.d(TAG, "Token already rotated by another request; retrying with the new one")
                return failedRequest.withBearer(current)
            }

            val refreshToken = runBlocking { tokenStore.refreshToken() }
            if (refreshToken.isNullOrBlank()) {
                Log.w(TAG, "No refresh token stored; cannot refresh session")
                return null
            }

            Log.i(TAG, "Access token rejected; refreshing session")
            val refreshed = runCatching { refreshCall(refreshToken) }
                .onFailure { Log.e(TAG, "Refresh call threw", it) }
                .getOrNull()
            if (refreshed == null || refreshed.accessToken.isBlank()) {
                Log.w(TAG, "Refresh rejected by the API; session will be expired")
                return null
            }

            runBlocking {
                tokenStore.save(
                    access = refreshed.accessToken,
                    refresh = refreshed.refreshToken.ifBlank { refreshToken },
                )
            }
            Log.i(TAG, "Session refreshed; replaying [${failedRequest.method}] ${failedRequest.url}")
            return failedRequest.withBearer(refreshed.accessToken)
        }
    }

    private fun Request.withBearer(token: String): Request =
        newBuilder().header(HEADER, bearer(token)).build()

    private fun bearer(token: String) = "Bearer $token"

    private companion object {
        const val TAG = "TokenAuthenticator"
        const val HEADER = "Authorization"
    }
}
