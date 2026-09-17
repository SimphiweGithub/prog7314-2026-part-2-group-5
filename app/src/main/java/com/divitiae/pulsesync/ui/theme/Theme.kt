package com.divitiae.pulsesync.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PulseSyncColorScheme = darkColorScheme(
    primary = ElectricTeal,
    onPrimary = DarkIrisSlate,
    primaryContainer = RoyalViolet,
    onPrimaryContainer = PureWhite,
    secondary = RoyalViolet,
    onSecondary = PureWhite,
    secondaryContainer = VioletSurfaceHigh,
    onSecondaryContainer = PureWhite,
    background = DarkIrisSlate,
    onBackground = PureWhite,
    surface = DarkIrisSlate,
    onSurface = PureWhite,
    surfaceVariant = VioletSurface,
    onSurfaceVariant = WhiteMuted,
    outline = RoyalViolet,
    error = ErrorRed,
    onError = DarkIrisSlate,
)

/**
 * Single source of truth for PulseSync colours and typography.
 * The brand is a fixed dark theme (Dark Iris Slate background, Royal Violet
 * surfaces, Electric Teal accents, pure white text), so the scheme does not
 * follow the system light/dark setting.
 */
@Composable
fun PulseSyncTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PulseSyncColorScheme,
        typography = PulseSyncTypography,
        content = content,
    )
}
