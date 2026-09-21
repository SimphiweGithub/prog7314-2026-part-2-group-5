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
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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

    val isSignedIn: Boolean get() = tokenStore.accessToken != null

    suspend fun currentUserId(): String = userDao.current()?.userId ?: "local"

    suspend fun signInWithGoogleIdToken(
        googleIdToken: String,
        fcmToken: String?,
    ): Result<UserProfile> = withContext(io) {
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

            when (
                val exchange = safeApiCall {
                    api.exchangeGoogleToken(GoogleSsoRequestDto(firebaseIdToken, fcmToken))
                }
            ) {
                is Result.Success -> {
                    Log.i(TAG, "Backend token exchange succeeded; storing JWT tokens for userId=${exchange.data.userId}")
                    tokenStore.save(exchange.data.accessToken, exchange.data.refreshToken)
                    val profile = UserProfile(
                        userId = exchange.data.userId.ifBlank { firebaseUser.uid },
                        email = firebaseUser.email.orEmpty(),
                        displayName = firebaseUser.displayName ?: firebaseUser.email.orEmpty(),
                        photoUrl = firebaseUser.photoUrl?.toString(),
                    )
                    userDao.upsert(profile.toEntity(System.currentTimeMillis()))
                    Result.Success(profile)
                }

                is Result.Failure -> {
                    // Offline-first fallback:
                    // Google + Firebase Auth succeeded on the client. If the remote ASP.NET Core
                    // REST API is unreachable or returns 404/5xx (e.g. cold start, load shedding,
                    // or staging environment offline), proceed using the Firebase ID token and user
                    // profile so the user can access the offline/cached feed without being blocked.
                    Log.w(TAG, "Backend token exchange failed (${exchange.error}); falling back to client-side Firebase token for offline access")
                    tokenStore.save(firebaseIdToken, firebaseIdToken)
                    val profile = UserProfile(
                        userId = firebaseUser.uid,
                        email = firebaseUser.email.orEmpty(),
                        displayName = firebaseUser.displayName ?: firebaseUser.email.orEmpty(),
                        photoUrl = firebaseUser.photoUrl?.toString(),
                    )
                    userDao.upsert(profile.toEntity(System.currentTimeMillis()))
                    Result.Success(profile)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "signInWithGoogleIdToken threw exception during token exchange", e)
            Result.Failure(AppError.Unknown(e.message, e))
        }
    }

    suspend fun signOut() = withContext(io) {
        Log.i(TAG, "signOut: clearing stored tokens and signing out of Firebase")
        safeApiCallEmpty { api.logout() }
        tokenStore.clear()
        runCatching { firebaseAuth.signOut() }
        userDao.clear()
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}
