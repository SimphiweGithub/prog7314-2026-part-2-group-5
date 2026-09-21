package com.divitiae.pulsesync.ui.theme

/**
 * Code Attribution No 10
 * This method was taken from "Material Design 3 in Compose"
 * https://developer.android.com/develop/ui/compose/designsystems/material3
 * Android Developers
 */

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Light scheme = the default Figma frames (cream canvas, navy text/buttons,
 * teal links, mint accent). Alpha-tinted fills in the design (navy at 6 %,
 * 15 %, 20 %) are derived from onSurface at the component level so they stay
 * correct in both schemes.
 */
// Adapted from: Android Developers (2026) Material Design 3 in Compose - colour scheme. https://developer.android.com/develop/ui/compose/designsystems/material3
private val LightColorScheme = lightColorScheme(
    primary = Navy,
    onPrimary = Cream,
    primaryContainer = Navy,
    onPrimaryContainer = Cream,
    secondary = Teal,
    onSecondary = PureWhite,
    secondaryContainer = Teal,
    onSecondaryContainer = PureWhite,
    tertiary = Mint,
    onTertiary = Navy,
    background = Cream,
    onBackground = Navy,
    surface = Cream,
    onSurface = Navy,
    surfaceVariant = Cream,
    onSurfaceVariant = Navy,
    surfaceContainer = PureWhite,
    surfaceContainerHigh = PureWhite,
    outline = Navy.copy(alpha = 0.20f),
    outlineVariant = Navy.copy(alpha = 0.15f),
    error = ErrorRed,
    onError = PureWhite,
)

/**
 * Dark scheme = the "(Dark)" Figma frames (ink canvas, cream text, navy
 * containers, teal links, mint accent).
 */
private val DarkColorScheme = darkColorScheme(
    primary = Navy,
    onPrimary = Cream,
    primaryContainer = Navy,
    onPrimaryContainer = Cream,
    secondary = Teal,
    onSecondary = PureWhite,
    secondaryContainer = Teal,
    onSecondaryContainer = PureWhite,
    tertiary = Mint,
    onTertiary = Navy,
    background = Ink,
    onBackground = Cream,
    surface = Ink,
    onSurface = Cream,
    surfaceVariant = Ink,
    onSurfaceVariant = Cream,
    surfaceContainer = Navy,
    surfaceContainerHigh = Navy,
    outline = Cream.copy(alpha = 0.20f),
    outlineVariant = Cream.copy(alpha = 0.15f),
    error = ErrorRed,
    onError = PureWhite,
)

/** True while the dark Figma scheme is active; for components with scheme-specific fills. */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

/**
 * Single source of truth for PulseSync colours and typography. Follows the
 * system light/dark setting by default so both Figma variants are honoured;
 * the Settings screen can override it via [darkTheme].
 */
@Composable
fun PulseSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = PulseSyncTypography,
            content = content,
        )
    }
}
