package com.divitiae.pulsesync.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.divitiae.pulsesync.ui.article.ArticleDetailScreen
import com.divitiae.pulsesync.ui.auth.AuthViewModel
import com.divitiae.pulsesync.ui.auth.SignInRoute
import com.divitiae.pulsesync.ui.auth.SignUpRoute
import com.divitiae.pulsesync.ui.components.BottomDestination
import com.divitiae.pulsesync.ui.feed.FeedRoute
import com.divitiae.pulsesync.ui.settings.SettingsScreen

/** Route names for the navigation graph. */
object Routes {
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
    const val FEED = "feed"
    const val SETTINGS = "settings"
    const val ARTICLE_ID_ARG = "articleId"
    const val ARTICLE = "article/{$ARTICLE_ID_ARG}"

    fun article(articleId: String): String = "article/$articleId"
}

/**
 * REPLACES Member 2's PulseSyncNavHost.kt.
 *
 * Top-level graph: auth (Google SSO) → feed → article detail, plus settings.
 * Navigation out of the auth screens is now gated on the ViewModel: the
 * routes call `onSignedIn` only after `/api/v1/auth/google` returns a JWT (or
 * when one is already persisted). Vault remains outside scope.
 *
 * Theme is no longer threaded through here: Settings writes it to DataStore
 * and the app root observes that flow, so every screen re-themes at once.
 */
@Composable
fun PulseSyncNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.SIGN_IN,
) {
    // Scoped to the Activity (not a back-stack entry) so Sign In and Sign Up
    // share one in-flight sign-in state.
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)

    fun switchTab(route: String) {
        navController.navigate(route) {
            popUpTo(Routes.FEED) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun enterApp() {
        navController.navigate(Routes.FEED) {
            popUpTo(Routes.SIGN_IN) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Routes.SIGN_IN) {
            SignInRoute(
                viewModel = authViewModel,
                onSignedIn = ::enterApp,
                onNavigateToSignUp = {
                    navController.navigate(Routes.SIGN_UP) { launchSingleTop = true }
                },
            )
        }
        composable(Routes.SIGN_UP) {
            SignUpRoute(
                viewModel = authViewModel,
                onSignedIn = ::enterApp,
                onNavigateToSignIn = {
                    // Sign In is always the root, so pop back instead of stacking.
                    navController.popBackStack(Routes.SIGN_IN, inclusive = false)
                },
            )
        }
        composable(Routes.FEED) {
            FeedRoute(
                onOpenArticle = { article ->
                    navController.navigate(Routes.article(article.id)) { launchSingleTop = true }
                },
                onNavigate = { destination ->
                    when (destination) {
                        BottomDestination.FEED -> Unit
                        BottomDestination.VAULT -> Unit // Vault is outside scope for Part 2
                        BottomDestination.SETTINGS -> switchTab(Routes.SETTINGS)
                    }
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigate = { destination ->
                    when (destination) {
                        BottomDestination.FEED -> switchTab(Routes.FEED)
                        BottomDestination.VAULT -> Unit
                        BottomDestination.SETTINGS -> Unit
                    }
                },
            )
        }
        composable(
            route = Routes.ARTICLE,
            arguments = listOf(navArgument(Routes.ARTICLE_ID_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getString(Routes.ARTICLE_ID_ARG)
            if (articleId.isNullOrBlank()) {
                navController.popBackStack()
            } else {
                // The ViewModel resolves the article from Room; a missing id
                // renders the error boundary rather than popping blindly.
                ArticleDetailScreen(
                    articleId = articleId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
