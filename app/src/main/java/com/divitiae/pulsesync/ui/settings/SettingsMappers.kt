package com.divitiae.pulsesync.ui.settings

import com.divitiae.pulsesync.data.domain.FontSizePref
import com.divitiae.pulsesync.data.domain.SummaryModePref
import com.divitiae.pulsesync.data.domain.ThemePref
import com.divitiae.pulsesync.ui.feed.SummaryMode

/**
 * Two-way bridges between the DataStore preference enums (domain layer) and
 * the enums Member 2's Settings composables render. Kept as tiny pure
 * functions so the mapping is unit-testable and the ViewModel stays free of
 * `when` noise.
 */

fun ThemePref.toUi(): ThemeMode = when (this) {
    ThemePref.LIGHT -> ThemeMode.LIGHT
    ThemePref.DARK -> ThemeMode.DARK
    ThemePref.SYSTEM -> ThemeMode.SYSTEM
}

fun ThemeMode.toPref(): ThemePref = when (this) {
    ThemeMode.LIGHT -> ThemePref.LIGHT
    ThemeMode.DARK -> ThemePref.DARK
    ThemeMode.SYSTEM -> ThemePref.SYSTEM
}

fun FontSizePref.toUi(): FontSizePreference = when (this) {
    FontSizePref.SMALL -> FontSizePreference.SMALL
    FontSizePref.MEDIUM -> FontSizePreference.MEDIUM
    FontSizePref.LARGE -> FontSizePreference.LARGE
}

fun FontSizePreference.toPref(): FontSizePref = when (this) {
    FontSizePreference.SMALL -> FontSizePref.SMALL
    FontSizePreference.MEDIUM -> FontSizePref.MEDIUM
    FontSizePreference.LARGE -> FontSizePref.LARGE
}

fun SummaryMode.toPref(): SummaryModePref = when (this) {
    SummaryMode.DETAILED -> SummaryModePref.DETAILED
    SummaryMode.CONDENSED -> SummaryModePref.CONDENSED
}
