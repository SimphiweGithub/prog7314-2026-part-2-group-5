package com.divitiae.pulsesync.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.divitiae.pulsesync.data.auth.AuthRepository
import com.divitiae.pulsesync.data.auth.AuthTokenStore
import com.divitiae.pulsesync.data.domain.AppError
import com.divitiae.pulsesync.data.domain.UserProfile
import com.divitiae.pulsesync.ui.common.UiState
import com.divitiae.pulsesync.ui.common.toUiState
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Client-side Google SSO orchestration (Member 4).
 *
 * Flow:
 *  1. The UI launches the Google Sign-In intent ([rememberGoogleSignInLauncher]).
 *  2. The result hands us the Google ID token → [onGoogleIdToken].
 *  3. [AuthRepository] signs that token into Firebase Auth, takes the Firebase
 *     ID token, POSTs it to `/api/v1/auth/google`, and persists the returned
 *     JWT (encrypted) in [AuthTokenStore].
 *  4. [signInState] flips Loading → Success(profile) | Error and the route
 *     navigates or shows a Snackbar.
 *
 * All strings shown to the user are resolved in the composable layer, so this
 * class holds no Context.
 */

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val tokenStore: AuthTokenStore,
) : ViewModel() {

    /** null = idle (nothing attempted yet, or a previous error was dismissed). */
    private val _signInState = MutableStateFlow<UiState<UserProfile>?>(null)
    val signInState: StateFlow<UiState<UserProfile>?> = _signInState.asStateFlow()

    /** null = still checking disk; true = a JWT exists so skip the sign-in screen. */
    private val _hasExistingSession = MutableStateFlow<Boolean?>(null)
    val hasExistingSession: StateFlow<Boolean?> = _hasExistingSession.asStateFlow()

    /** One-shot UI events that are not part of persistent state. */
    private val _events = MutableStateFlow<AuthEvent?>(null)
    val events: StateFlow<AuthEvent?> = _events.asStateFlow()

    init {
        Log.d(TAG, "AuthViewModel initialized")
        viewModelScope.launch {
            // AppContainer.initialise() also calls load(), but racing it here is
            // harmless (idempotent) and guarantees the value is fresh before we route.
            tokenStore.load()
            _hasExistingSession.value = tokenStore.accessToken != null
        }
    }

    /** Step 2: the Google Sign-In intent returned an ID token. */
    fun onGoogleIdToken(googleIdToken: String) {
        if (_signInState.value is UiState.Loading) return // ignore double taps
        Log.d(TAG, "onGoogleIdToken: received Google ID token, initiating sign-in")
        _signInState.value = UiState.Loading

        viewModelScope.launch {
            val fcmToken = fetchFcmTokenOrNull()
            _signInState.value = authRepository
                .signInWithGoogleIdToken(googleIdToken, fcmToken)
                .toUiState()
        }
    }

    /**
     * The intent finished without a token. [reason] null means the user simply
     * cancelled the account picker — that is not an error, so we go back to idle.
     */
    fun onGoogleSignInFailed(reason: GoogleSignInFailure?) {
        _signInState.value = when (reason) {
            null -> null
            else -> UiState.Error(
                message = reason.detail,
                error = AppError.Unauthorized(reason.detail),
                retryable = reason != GoogleSignInFailure.NOT_CONFIGURED,
            )
        }
        if (reason != null) _events.value = AuthEvent.GoogleFailure(reason)
    }

    fun onEmailSignInRequested() {
        // The backend contract exposes Google SSO only; surface that instead of faking a login.
        _events.value = AuthEvent.EmailNotAvailable
    }
    /** Call after showing an error so the same Snackbar is not re-shown on recomposition. */
    fun consumeError() {
        _signInState.update { if (it is UiState.Error) null else it }
    }

    fun consumeEvent() {
        _events.value = null
    }

    fun signOut() {
        Log.i(TAG, "signOut requested in AuthViewModel")
        viewModelScope.launch {
            authRepository.signOut()
            _signInState.value = null
            _hasExistingSession.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "AuthViewModel onCleared")
    }

    /**
     * Best-effort: a missing FCM token must never block sign-in, so we cap the
     * wait and swallow failures (e.g. no Google Play services on an emulator).
     */
    private suspend fun fetchFcmTokenOrNull(): String? = try {
        withTimeoutOrNull(FCM_TOKEN_TIMEOUT_MS) {
            FirebaseMessaging.getInstance().token.await()
        }
    } catch (_: Exception) {
        null
    }

    companion object {
        private const val TAG = "AuthViewModel"
        private const val FCM_TOKEN_TIMEOUT_MS = 3_000L

        val Factory: ViewModelProvider.Factory =
            com.divitiae.pulsesync.ui.viewmodel.containerViewModelFactory { container ->
                AuthViewModel(container.authRepository, container.authTokenStore)
            }
    }
}

/** Why the Google Sign-In intent did not produce an ID token. */
enum class GoogleSignInFailure(val detail: String) {
    /** google-services.json has no OAuth web client (Google provider not enabled / SHA-1 missing). */
    NOT_CONFIGURED("Google Sign-In is not configured for this build"),
    /** The intent returned an account with a null idToken. */
    NO_ID_TOKEN("Google did not return an ID token"),
    /** Play Services returned an ApiException other than cancellation. */
    API_ERROR("Google Sign-In failed"),
}

sealed interface AuthEvent {
    data class GoogleFailure(val reason: GoogleSignInFailure) : AuthEvent
    data object EmailNotAvailable : AuthEvent
}