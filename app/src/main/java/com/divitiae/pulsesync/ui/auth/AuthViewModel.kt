package com.divitiae.pulsesync.ui.auth

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
