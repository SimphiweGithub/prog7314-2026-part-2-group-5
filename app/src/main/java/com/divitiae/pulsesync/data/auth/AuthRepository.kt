package com.divitiae.pulsesync.data.auth

/**
 * Code Attribution No 7
 * This method was taken from "Authenticate Using Google Sign-In on Android"
 * https://firebase.google.com/docs/auth/android/google-signin
 * Firebase Documentation & Android Open Source Project
 */

import android.util.Log
import com.divitiae.pulsesync.data.domain.AppError
import com.divitiae.pulsesync.data.domain.Result
import com.divitiae.pulsesync.data.domain.UserProfile
import com.divitiae.pulsesync.data.local.dao.UserDao
import com.divitiae.pulsesync.data.mapper.toDomain
import com.divitiae.pulsesync.data.mapper.toEntity
import com.divitiae.pulsesync.data.remote.PulseSyncApi
import com.divitiae.pulsesync.data.remote.dto.GoogleSsoRequestDto
import com.divitiae.pulsesync.data.repository.safeApiCall
import com.divitiae.pulsesync.data.repository.safeApiCallEmpty
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Outcome of a successful Google sign-in. [serverError] is set when Google and
 * Firebase accepted the user but the PulseSync API could not be reached, in
 * which case the app runs an **offline session**: cached content only, no API
 * token, until [AuthRepository.completePendingExchange] succeeds.
 */
data class SignIn(
    val profile: UserProfile,
    val serverError: AppError? = null,
) {
    val isOffline: Boolean get() = serverError != null
}

/**
 * Handles the SSO flow: a Google ID token is signed into Firebase, the
 * resulting Firebase ID token is exchanged with our API for a JWT, and the
 * profile + tokens are persisted. Firebase is only touched during sign-in, so
 * the app still launches and runs before google-services.json is added.
 */
class AuthRepository(
    private val api: PulseSyncApi,
    private val userDao: UserDao,
    private val tokenStore: AuthTokenStore,
    private val firebaseAuth: FirebaseAuth,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    fun observeUser(): Flow<UserProfile?> =
        userDao.observeCurrent().map { it?.toDomain() }

    val isSignedIn: Boolean get() = tokenStore.accessToken != null || tokenStore.isOfflineSession

    suspend fun currentUserId(): String = userDao.current()?.userId ?: "local"

    suspend fun signInWithGoogleIdToken(
        googleIdToken: String,
        fcmToken: String?,
    ): Result<SignIn> = withContext(io) {
        Log.d(TAG, "signInWithGoogleIdToken: received Google ID token; authenticating with Firebase")
        try {
            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
            if (firebaseUser == null) {
                Log.w(TAG, "Firebase credential sign-in returned null user")
                return@withContext Result.Failure(AppError.Unauthorized("Firebase returned no user"))
            }
            Log.i(TAG, "Firebase credential sign-in succeeded for uid=${firebaseUser.uid}")
            val firebaseIdToken = firebaseUser.getIdToken(false).await().token
            if (firebaseIdToken == null) {
                Log.w(TAG, "Failed to retrieve Firebase ID token for uid=${firebaseUser.uid}")
                return@withContext Result.Failure(AppError.Unauthorized("No Firebase ID token"))
            }
            Log.d(TAG, "Firebase ID token retrieved; requesting token exchange with backend API")

            val profile = firebaseUser.toProfile()
            when (val exchange = exchange(firebaseIdToken, fcmToken)) {
                is Result.Success -> {
                    Log.i(TAG, "Backend token exchange succeeded; storing JWT tokens for userId=${exchange.data}")
                    val resolved = profile.copy(userId = exchange.data.ifBlank { firebaseUser.uid })
                    userDao.upsert(resolved.toEntity(System.currentTimeMillis()))
                    Result.Success(SignIn(resolved))
                }

                is Result.Failure -> {
                    if (!exchange.error.isServerUnavailable()) {
                        // The API answered and rejected the credential (401, 4xx): that is a
                        // real sign-in failure, not an outage, so do not let the user in.
                        Log.w(TAG, "Backend token exchange rejected the credential: ${exchange.error}")
                        return@withContext Result.Failure(exchange.error)
                    }
                    // Offline-first fallback: Google + Firebase succeeded but the REST API is
                    // unreachable (no connectivity, Render cold start, 5xx). Enter an offline
                    // session so cached content stays available, but do NOT store the Firebase
                    // ID token as if it were an API token; the exchange is retried on reconnect.
                    Log.w(TAG, "Backend token exchange failed (${exchange.error}); entering OFFLINE session for uid=${firebaseUser.uid}")
                    tokenStore.markOfflineSession()
                    userDao.upsert(profile.toEntity(System.currentTimeMillis()))
                    Result.Success(SignIn(profile, serverError = exchange.error))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "signInWithGoogleIdToken threw exception during token exchange", e)
            Result.Failure(AppError.Unknown(e.message, e))
        }
    }

    /**
     * Turns an offline session into a real one: if the last sign-in never got an
     * API token, ask Firebase for a fresh ID token and retry the exchange. Safe
     * to call whenever connectivity returns; it is a no-op for online sessions.
     */
    suspend fun completePendingExchange(): Result<Unit> = withContext(io) {
        if (!tokenStore.isOfflineSession || tokenStore.accessToken != null) {
            return@withContext Result.Success(Unit)
        }
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) {
            Log.w(TAG, "completePendingExchange: offline session but no Firebase user; clearing session")
            tokenStore.clear()
            return@withContext Result.Failure(AppError.Unauthorized("No Firebase user for offline session"))
        }
        try {
            val firebaseIdToken = firebaseUser.getIdToken(false).await().token
                ?: return@withContext Result.Failure(AppError.Unauthorized("No Firebase ID token"))
            Log.i(TAG, "completePendingExchange: retrying backend token exchange for uid=${firebaseUser.uid}")
            when (val exchange = exchange(firebaseIdToken, fcmToken = null)) {
                is Result.Success -> {
                    Log.i(TAG, "completePendingExchange: offline session upgraded to online for userId=${exchange.data}")
                    Result.Success(Unit)
                }
                is Result.Failure -> {
                    Log.w(TAG, "completePendingExchange: exchange still failing (${exchange.error}); staying offline")
                    Result.Failure(exchange.error)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "completePendingExchange threw", e)
            Result.Failure(AppError.Unknown(e.message, e))
        }
    }

    /** Exchanges a Firebase ID token for a PulseSync JWT pair and stores it. Returns the API's user id. */
    private suspend fun exchange(firebaseIdToken: String, fcmToken: String?): Result<String> =
        when (val result = safeApiCall { api.exchangeGoogleToken(GoogleSsoRequestDto(firebaseIdToken, fcmToken)) }) {
            is Result.Success -> {
                tokenStore.save(result.data.accessToken, result.data.refreshToken)
                Result.Success(result.data.userId)
            }
            is Result.Failure -> Result.Failure(result.error)
        }

    private fun FirebaseUser.toProfile() = UserProfile(
        userId = uid,
        email = email.orEmpty(),
        displayName = displayName ?: email.orEmpty(),
        photoUrl = photoUrl?.toString(),
    )

    suspend fun signOut() = withContext(io) {
        Log.i(TAG, "signOut: clearing stored tokens and signing out of Firebase")
        safeApiCallEmpty { api.logout() }
        tokenStore.clear()
        runCatching { firebaseAuth.signOut() }
        userDao.clear()
    }

    companion object {
        private const val TAG = "AuthRepository"

        /**
         * True when the failure means the API could not be reached or is down,
         * as opposed to the API having looked at the credential and refused it.
         */
        fun AppError.isServerUnavailable(): Boolean = when (this) {
            is AppError.Network -> true
            is AppError.Unknown -> true
            is AppError.Http -> code == 404 || code >= 500
            is AppError.Unauthorized -> false
        }
    }
}
