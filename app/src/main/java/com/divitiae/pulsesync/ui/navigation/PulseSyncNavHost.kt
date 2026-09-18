package com.divitiae.pulsesync.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.divitiae.pulsesync.ui.article.ArticleDetailScreen
import com.divitiae.pulsesync.ui.auth.SignInScreen
import com.divitiae.pulsesync.ui.auth.SignUpScreen
import com.divitiae.pulsesync.ui.components.BottomDestination
import com.divitiae.pulsesync.ui.feed.FeedSampleData
import com.divitiae.pulsesync.ui.feed.FeedScreen
import com.divitiae.pulsesync.ui.settings.SettingsScreen
import com.divitiae.pulsesync.ui.settings.ThemeMode

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
 * Top-level navigation graph: auth flow → feed → article detail, plus the
 * settings tab. Vault is outside Member 2's scope and stays a no-op.
 *
 * Auth actions only navigate here; the actual sign-in/registration calls
 * are Member 4's ViewModels, which will gate the navigation on success.
 */
@Composable
fun PulseSyncNavHost(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.SIGN_IN,
) {
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
            SignInScreen(
                onSignIn = { _, _ -> enterApp() /* TODO(Member 4): AuthViewModel.signIn */ },
                onGoogleSignIn = { enterApp() /* TODO(Member 4): Google SSO */ },
                onBiometricSignIn = { enterApp() /* TODO(Member 4): BiometricPrompt */ },
                onForgotPassword = { /* TODO: password reset flow */ },
                onNavigateToSignUp = {
                    navController.navigate(Routes.SIGN_UP) { launchSingleTop = true }
                },
            )
        }
        composable(Routes.SIGN_UP) {
            SignUpScreen(
                onCreateAccount = { _, _, _ -> enterApp() /* TODO(Member 4): AuthViewModel.register */ },
                onGoogleSignUp = { enterApp() /* TODO(Member 4): Google SSO */ },
                onNavigateToSignIn = {
                    // Sign In is always the root, so pop back instead of stacking.
                    navController.popBackStack(Routes.SIGN_IN, inclusive = false)
                },
            )
        }
        composable(Routes.FEED) {
            FeedScreen(
                onOpenArticle = { article ->
                    navController.navigate(Routes.article(article.id)) { launchSingleTop = true }
                },
                onNavigate = { destination ->
                    when (destination) {
                        BottomDestination.FEED -> Unit
                        BottomDestination.VAULT -> Unit // Vault is outside Member 2's scope
                        BottomDestination.SETTINGS -> switchTab(Routes.SETTINGS)
                    }
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                onNavigate = { destination ->
                    when (destination) {
                        BottomDestination.FEED -> switchTab(Routes.FEED)
                        BottomDestination.VAULT -> Unit // Vault is outside Member 2's scope
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
            // TODO(Member 4): resolve from the ArticleViewModel instead of sample data.
            val article = articleId?.let(FeedSampleData::articleById)
            if (article == null) {
                navController.popBackStack()
            } else {
                ArticleDetailScreen(
                    article = article,
                    onBack = { navController.popBackStack() },
                    onToggleSave = { /* TODO(Member 4): persist saved state */ },
                    onSaveNote = { _, _ -> /* TODO(Member 4): NotesViewModel.save */ },
                )
            }
        }
    }
}
