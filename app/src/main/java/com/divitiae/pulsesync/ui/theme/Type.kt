package com.divitiae.pulsesync.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Figma specifies Roboto; on Android the default sans-serif family is Roboto.
private val Brand = FontFamily.SansSerif

/**
 * Text styles mapped from the Figma text styles:
 *  - titleLarge  = brand title on Sign In (22 / 26, Medium)
 *  - titleMedium = screen heading e.g. "Create your account" (18 / 22, Medium)
 *  - bodyMedium  = PulseSync/Body (14 / 20, Regular)
 *  - labelLarge  = PulseSync/Button Label (14 / 18, Medium)
 *  - bodySmall   = PulseSync/Caption (12 / 16, Regular)
 */
val PulseSyncTypography = Typography(
    headlineLarge = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Brand, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)
