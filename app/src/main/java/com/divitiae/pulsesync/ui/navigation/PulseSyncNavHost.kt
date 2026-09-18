package com.divitiae.pulsesync.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.divitiae.pulsesync.ui.auth.SignInScreen
import com.divitiae.pulsesync.ui.auth.SignUpScreen

/** Route names for the navigation graph. */
object Routes {
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
}

/**
 * Top-level navigation graph. Currently holds the auth flow; the feed,
 * article and settings destinations are added in later commits.
 *
 * Auth actions are no-ops here on purpose: the UI layer exposes the events
 * and Member 4's ViewModels will consume them.
 */
@Composable
fun PulseSyncNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.SIGN_IN,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Routes.SIGN_IN) {
            SignInScreen(
                onSignIn = { _, _ -> /* TODO(Member 4): AuthViewModel.signIn */ },
                onGoogleSignIn = { /* TODO(Member 4): Google SSO */ },
                onBiometricSignIn = { /* TODO(Member 4): BiometricPrompt */ },
                onForgotPassword = { /* TODO: password reset flow */ },
                onNavigateToSignUp = {
                    navController.navigate(Routes.SIGN_UP) { launchSingleTop = true }
                },
            )
        }
        composable(Routes.SIGN_UP) {
            SignUpScreen(
                onCreateAccount = { _, _, _ -> /* TODO(Member 4): AuthViewModel.register */ },
                onGoogleSignUp = { /* TODO(Member 4): Google SSO */ },
                onNavigateToSignIn = {
                    // Sign In is always the root, so pop back instead of stacking.
                    navController.popBackStack(Routes.SIGN_IN, inclusive = false)
                },
            )
        }
    }
}
