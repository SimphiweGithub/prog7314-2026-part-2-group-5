package com.divitiae.pulsesync.data.domain

/** The signed-in user, resolved from the SSO exchange. */
data class UserProfile(
    val userId: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val preferredLanguage: String = "en",
    val biometricEnabled: Boolean = false,
)

/**
 * Locally held user preferences (DataStore-backed). Kept in the domain layer
 * so both the settings screen and the network layer (Accept-Language) can read
 * a single source of truth without depending on UI types.
 */
data class UserPreferences(
    val defaultSummaryMode: SummaryModePref = SummaryModePref.DETAILED,
    val language: String = "English",
    val biometricLock: Boolean = false,
    val themeMode: ThemePref = ThemePref.SYSTEM,
    val fontSize: FontSizePref = FontSizePref.MEDIUM,
    val highContrast: Boolean = false,
) {
    /** BCP-47-ish tag for the Accept-Language header, derived from [language]. */
    val languageTag: String
        get() = when (language.lowercase()) {
            "afrikaans" -> "af"
            "isizulu", "zulu" -> "zu"
            "isixhosa", "xhosa" -> "xh"
            else -> "en"
        }
}

enum class SummaryModePref { DETAILED, CONDENSED }
enum class ThemePref { LIGHT, DARK, SYSTEM }
enum class FontSizePref { SMALL, MEDIUM, LARGE }
