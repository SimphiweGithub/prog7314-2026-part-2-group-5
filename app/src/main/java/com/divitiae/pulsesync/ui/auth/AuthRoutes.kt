package com.divitiae.pulsesync.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.divitiae.pulsesync.R
import com.divitiae.pulsesync.ui.common.UiState
import com.divitiae.pulsesync.ui.common.userMessage

/**
 * Stateful wrappers ("routes") around Member 2's stateless SignInScreen and
 * SignUpScreen. They own the ViewModel, the Google intent launcher, the
 * loading scrim and the error Snackbar, so the screen files stay untouched.
 *
 * Both routes share one [AuthViewModel] instance (created in the NavHost and
 * scoped to the Activity) so a sign-in started on Sign Up and completed after
 * navigating back still lands on the Feed.
 */
@Composable
fun SignInRoute(
    viewModel: AuthViewModel,
    onSignedIn: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScaffold(viewModel = viewModel, onSignedIn = onSignedIn, modifier = modifier) { launchGoogle, isBusy ->
        SignInScreen(
            onSignIn = { _, _ -> if (!isBusy) viewModel.onEmailSignInRequested() },
            onGoogleSignIn = { if (!isBusy) launchGoogle() },
            onBiometricSignIn = { /* BiometricPrompt is a separate deliverable */ },
            onForgotPassword = { /* password reset is out of scope for SSO-only auth */ },
            onNavigateToSignUp = onNavigateToSignUp,
        )
    }
}

@Composable
fun SignUpRoute(
    viewModel: AuthViewModel,
    onSignedIn: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScaffold(viewModel = viewModel, onSignedIn = onSignedIn, modifier = modifier) { launchGoogle, isBusy ->
        SignUpScreen(
            onCreateAccount = { _, _, _ -> if (!isBusy) viewModel.onEmailSignInRequested() },
            // Google handles new vs. returning users; the API returns isNewUser.
            onGoogleSignUp = { if (!isBusy) launchGoogle() },
            onNavigateToSignIn = onNavigateToSignIn,
        )
    }
}

/**
 * Shared plumbing: collects [AuthViewModel.signInState], navigates on
 * Success, shows a Snackbar on Error, dims the screen while Loading, and
 * skips straight to the Feed when a JWT already exists on disk.
 */
@Composable
private fun AuthScaffold(
    viewModel: AuthViewModel,
    onSignedIn: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (launchGoogle: () -> Unit, isBusy: Boolean) -> Unit,
) {
    val signInState by viewModel.signInState.collectAsState()
    val hasSession by viewModel.hasExistingSession.collectAsState()
    val event by viewModel.events.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val emailNotAvailable = stringResource(R.string.auth_email_not_available)
    val sessionExpired = stringResource(R.string.common_error_unauthorized)
    val notConfigured = stringResource(R.string.auth_error_google_not_configured)
    val noIdToken = stringResource(R.string.auth_error_google_no_id_token)
    val googleGeneric = stringResource(R.string.auth_error_google_generic)
    // Resolved during composition (stringResource is @Composable); read inside the effect below.
    val apiErrorText = (signInState as? UiState.Error)?.userMessage() ?: googleGeneric

    // Returning user: token already persisted → go straight in.
    LaunchedEffect(hasSession) {
        if (hasSession == true) onSignedIn()
    }

    // React to the outcome of the /auth/google exchange.
    LaunchedEffect(signInState) {
        when (val state = signInState) {
            is UiState.Success -> onSignedIn()
            is UiState.Error -> {
                // Prefer the friendly message for the Google-side failure; fall
                // back to the network/HTTP mapping for the API exchange itself.
                val text = when ((event as? AuthEvent.GoogleFailure)?.reason) {
                    GoogleSignInFailure.NOT_CONFIGURED -> notConfigured
                    GoogleSignInFailure.NO_ID_TOKEN -> noIdToken
                    GoogleSignInFailure.API_ERROR -> googleGeneric
                    null -> apiErrorText
                }
                snackbarHostState.showSnackbar(text)
                viewModel.consumeEvent()
                viewModel.consumeError()
            }
            else -> Unit
        }
    }

    // Events that are not tied to signInState (e.g. email button pressed, session expired).
    LaunchedEffect(event) {
        when (event) {
            AuthEvent.EmailNotAvailable -> {
                snackbarHostState.showSnackbar(emailNotAvailable)
                viewModel.consumeEvent()
            }
            AuthEvent.SessionExpired -> {
                snackbarHostState.showSnackbar(sessionExpired)
                viewModel.consumeEvent()
            }
            else -> Unit
        }
    }

    val launchGoogle = rememberGoogleSignInLauncher(
        onIdToken = viewModel::onGoogleIdToken,
        onFailure = viewModel::onGoogleSignInFailed,
    )
    val isBusy = signInState is UiState.Loading

    Box(modifier = modifier.fillMaxSize()) {
        content(launchGoogle, isBusy)

        if (isBusy) {
            // Scrim that also swallows taps so the form cannot be re-submitted mid-exchange.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f))
                    .pointerInput(Unit) { },
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }
}