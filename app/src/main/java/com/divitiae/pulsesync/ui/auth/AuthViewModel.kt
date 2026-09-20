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