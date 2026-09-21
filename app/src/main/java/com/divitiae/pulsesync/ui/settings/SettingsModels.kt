package com.divitiae.pulsesync.ui.settings

/**
 * Code Attribution No 27
 * This method was taken from "Kotlin Data classes and Enum conventions"
 * https://kotlinlang.org/docs/data-classes.html
 * JetBrains
 */

import com.divitiae.pulsesync.ui.feed.SummaryMode

/** How the app resolves light vs. dark (Accessibility card, Figma 23:89). */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class FontSizePreference { SMALL, MEDIUM, LARGE }

data class TopicToggle(val name: String, val enabled: Boolean)

/**
 * UI state for Preferences & Settings (Figma 9:35 / 29:295). Held locally
 * in the prototype; Member 4's SettingsViewModel will back it with DataStore.
 */
data class SettingsUiState(
    val defaultSummaryMode: SummaryMode = SummaryMode.DETAILED,
    val topics: List<TopicToggle> = emptyList(),
    val keywords: List<String> = emptyList(),
    val offlineSlotsUsed: Int = 0,
    val offlineSlotsTotal: Int = 5,
    val queuedNotes: Int = 0,
    val language: String = "English",
    val languages: List<String> = listOf("English"),
    val biometricLock: Boolean = false,
    val fontSize: FontSizePreference = FontSizePreference.MEDIUM,
    val fontType: String = "Default",
    val fontTypes: List<String> = listOf("Default"),
    val highContrast: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

object SettingsSampleData {
    fun initialState(): SettingsUiState = SettingsUiState(
        defaultSummaryMode = SummaryMode.DETAILED,
        topics = listOf(
            TopicToggle("Technology", enabled = true),
            TopicToggle("SA Politics", enabled = true),
            TopicToggle("Bursaries", enabled = false),
            TopicToggle("Sports", enabled = false),
        ),
        keywords = listOf("internship", "load shedding", "bursary"),
        offlineSlotsUsed = 3,
        offlineSlotsTotal = 5,
        queuedNotes = 2,
        language = "English",
        languages = listOf("English", "Afrikaans", "isiZulu", "isiXhosa"),
        biometricLock = true,
        fontSize = FontSizePreference.MEDIUM,
        fontType = "Default",
        fontTypes = listOf("Default", "Serif", "Monospace"),
        highContrast = false,
        themeMode = ThemeMode.LIGHT,
    )
}
