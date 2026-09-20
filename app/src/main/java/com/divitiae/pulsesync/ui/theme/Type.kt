package com.divitiae.pulsesync.ui.theme

/*
 * ---------------------------------------------------------------------
 * CODE ATTRIBUTION
 * ---------------------------------------------------------------------
 * The Material 3 Typography mapping and custom TextStyle definitions in this file were adapted from:
 *
 * Android Developers (2026) Material Design 3 in Compose. [online]
 * Available at: https://developer.android.com/develop/ui/compose/designsystems/material3
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Style text. [online]
 * Available at: https://developer.android.com/develop/ui/compose/text/style-text
 * [Accessed 20 September 2026].
 * ---------------------------------------------------------------------
 */

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Figma specifies Roboto; on Android the default sans-serif family is Roboto.
private val Brand = FontFamily.SansSerif

/**
 * Text styles mapped from the Figma text styles:
 *  - headlineSmall = PulseSync/Headline, app bar title (20 / 26, Medium)
 *  - titleLarge    = brand title on Sign In (22 / 26, Medium)
 *  - titleMedium   = screen heading e.g. "Create your account" (18 / 22, Medium)
 *  - titleSmall    = PulseSync/Title, article card title (16 / 22, Medium)
 *  - bodyMedium    = PulseSync/Body (14 / 20, Regular)
 *  - labelLarge    = PulseSync/Button Label (14 / 18, Medium)
 *  - bodySmall     = PulseSync/Caption (12 / 16, Regular)
 *  - labelSmall    = PulseSync/Nav Label (11 / 14, Medium)
 */
// Adapted from: Android Developers (2026) Material Design 3 in Compose - typography. https://developer.android.com/develop/ui/compose/designsystems/material3
val PulseSyncTypography = Typography(
    headlineLarge = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

/** Extra Figma sizes that have no Material slot. */
object PulseSyncTextStyles {
    /** 10 / 16 Regular: AI badge, sentiment and keyword chips. */
    val chip = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Normal, fontSize = 10.sp, lineHeight = 16.sp)

    /** 12 / 14 Medium: category tab labels. */
    val tab = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 14.sp)

    /** 13 sp Medium: "Extracted Resources & Links" panel heading. */
    val panelTitleSize = 13.sp
}
