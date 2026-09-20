package com.divitiae.pulsesync.ui.settings

/*
 * ---------------------------------------------------------------------
 * CODE ATTRIBUTION
 * ---------------------------------------------------------------------
 * The Toast feedback, FlowRow keyword chips, Scaffold structure, local UI state and preview annotations in this file were adapted from:
 *
 * Android Developers (2026) Toasts overview. [online]
 * Available at: https://developer.android.com/guide/topics/ui/notifiers/toasts
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Flow layouts in Compose. [online]
 * Available at: https://developer.android.com/develop/ui/compose/layouts/flow
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Scaffold. [online]
 * Available at: https://developer.android.com/develop/ui/compose/components/scaffold
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) State and Jetpack Compose. [online]
 * Available at: https://developer.android.com/develop/ui/compose/state
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Preview your UI with composable previews. [online]
 * Available at: https://developer.android.com/develop/ui/compose/tooling/previews
 * [Accessed 20 September 2026].
 * ---------------------------------------------------------------------
 */

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.divitiae.pulsesync.R
import com.divitiae.pulsesync.ui.components.BottomDestination
import com.divitiae.pulsesync.ui.components.PulseSyncBottomBar
import com.divitiae.pulsesync.ui.components.PulseSyncDimens
import com.divitiae.pulsesync.ui.feed.SummaryMode
import com.divitiae.pulsesync.ui.theme.PulseSyncTheme

/**
 * Preferences & Settings (Figma 9:35, dark 29:295).
 *
 * Prototype state lives here; the theme choice is hoisted through
 * [onThemeModeChange] so the whole app re-themes immediately. Member 4's
 * SettingsViewModel replaces the local state without touching [SettingsContent].
 */
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onNavigate: (BottomDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember { mutableStateOf(SettingsSampleData.initialState().copy(themeMode = themeMode)) }
    var keywordDraft by rememberSaveable { mutableStateOf("") }
    var keywordError by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    SettingsContent(
        state = state.copy(themeMode = themeMode),
        keywordDraft = keywordDraft,
        keywordError = keywordError,
        onKeywordDraftChange = {
            keywordDraft = it
            keywordError = false // Clear the highlight as soon as the user edits.
        },
        onAddKeyword = {
            when (val result = KeywordValidation.validate(keywordDraft, state.keywords)) {
                is KeywordValidation.Result.Valid -> {
                    state = state.copy(keywords = state.keywords + result.keyword)
                    keywordDraft = ""
                    keywordError = false
                }
                KeywordValidation.Result.Blank -> {
                    keywordError = true
                    // Adapted from: Android Developers (2026) Toasts overview. https://developer.android.com/guide/topics/ui/notifiers/toasts
                    Toast.makeText(context, R.string.settings_keyword_blank_error, Toast.LENGTH_SHORT).show()
                }
                KeywordValidation.Result.Duplicate -> {
                    // Keep the draft so the user can see what was rejected.
                    keywordError = true
                    Toast.makeText(context, R.string.settings_keyword_duplicate_error, Toast.LENGTH_SHORT).show()
                }
            }
        },
        onRemoveKeyword = { keyword -> state = state.copy(keywords = state.keywords - keyword) },
        onToggleTopic = { name, enabled ->
            state = state.copy(topics = state.topics.map { if (it.name == name) it.copy(enabled = enabled) else it })
        },
        onDefaultSummaryChange = { state = state.copy(defaultSummaryMode = it) },
        onLanguageChange = { state = state.copy(language = it) },
        onBiometricLockChange = { state = state.copy(biometricLock = it) },
        onFontSizeChange = { state = state.copy(fontSize = it) },
        onFontTypeChange = { state = state.copy(fontType = it) },
        onHighContrastChange = { state = state.copy(highContrast = it) },
        onThemeModeChange = onThemeModeChange,
        onNavigate = onNavigate,
        modifier = modifier,
    )
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    keywordDraft: String,
    keywordError: Boolean,
    onKeywordDraftChange: (String) -> Unit,
    onAddKeyword: () -> Unit,
    onRemoveKeyword: (String) -> Unit,
    onToggleTopic: (name: String, enabled: Boolean) -> Unit,
    onDefaultSummaryChange: (SummaryMode) -> Unit,
    onLanguageChange: (String) -> Unit,
    onBiometricLockChange: (Boolean) -> Unit,
    onFontSizeChange: (FontSizePreference) -> Unit,
    onFontTypeChange: (String) -> Unit,
    onHighContrastChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onNavigate: (BottomDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SettingsTopBar() },
        bottomBar = { PulseSyncBottomBar(selected = BottomDestination.SETTINGS, onSelect = onNavigate) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PulseSyncDimens.ScreenPadding, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Default Summary View (Figma 9:45) — default for User Defined Feature 1.
            SettingsCard(icon = Icons.Rounded.Summarize, title = stringResource(R.string.settings_summary_title)) {
                SegmentedControl(
                    options = SummaryMode.entries,
                    selected = state.defaultSummaryMode,
                    onSelect = onDefaultSummaryChange,
                    label = { mode ->
                        stringResource(
                            if (mode == SummaryMode.DETAILED) {
                                R.string.settings_summary_detailed
                            } else {
                                R.string.settings_summary_condensed
                            },
                        )
                    },
                )
            }

            // Topics & Tracking Keywords (Figma 10:35).
            SettingsCard(icon = Icons.Rounded.Sell, title = stringResource(R.string.settings_topics_title)) {
                state.topics.forEach { topic ->
                    SettingsRow(label = topic.name) {
                        PulseSyncSwitch(
                            checked = topic.enabled,
                            onCheckedChange = { onToggleTopic(topic.name, it) },
                        )
                    }
                }
                AddKeywordField(
                    value = keywordDraft,
                    onValueChange = onKeywordDraftChange,
                    onSubmit = onAddKeyword,
                    isError = keywordError,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.keywords.forEach { keyword ->
                        KeywordChip(keyword = keyword, onRemove = { onRemoveKeyword(keyword) })
                    }
                }
            }

            // Storage & Note Manager (Figma 10:68).
            SettingsCard(icon = Icons.Rounded.Storage, title = stringResource(R.string.settings_storage_title)) {
                SettingsRow(label = stringResource(R.string.settings_offline_articles)) {
                    Text(
                        text = stringResource(
                            R.string.settings_slots_used,
                            state.offlineSlotsUsed,
                            state.offlineSlotsTotal,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                SlotProgressBar(used = state.offlineSlotsUsed, total = state.offlineSlotsTotal)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(R.string.settings_queued_notes, state.queuedNotes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    )
                }
            }

            // Language & Security (Figma 10:80).
            SettingsCard(icon = Icons.Rounded.Tune, title = stringResource(R.string.settings_language_title)) {
                SettingsRow(label = stringResource(R.string.settings_app_language)) {
                    DropdownChip(value = state.language, options = state.languages, onSelect = onLanguageChange)
                }
                SettingsRow(label = stringResource(R.string.settings_biometric_lock)) {
                    PulseSyncSwitch(checked = state.biometricLock, onCheckedChange = onBiometricLockChange)
                }
            }

            // Accessibility (Figma 23:68).
            SettingsCard(
                icon = Icons.Rounded.AccessibilityNew,
                title = stringResource(R.string.settings_accessibility_title),
            ) {
                SettingsRow(label = stringResource(R.string.settings_font_size)) {
                    FontSizeStepper(value = state.fontSize, onChange = onFontSizeChange)
                }
                SettingsRow(label = stringResource(R.string.settings_font_type)) {
                    DropdownChip(value = state.fontType, options = state.fontTypes, onSelect = onFontTypeChange)
                }
                SettingsRow(label = stringResource(R.string.settings_high_contrast)) {
                    PulseSyncSwitch(checked = state.highContrast, onCheckedChange = onHighContrastChange)
                }
                SegmentedControl(
                    options = ThemeMode.entries,
                    selected = state.themeMode,
                    onSelect = onThemeModeChange,
                    label = { stringResource(it.labelRes()) },
                )
            }
        }
    }
}

@Preview(name = "Settings · Light", showBackground = true, widthDp = 360, heightDp = 1020)
@Preview(
    name = "Settings · Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 1020,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SettingsPreview() {
    PulseSyncTheme {
        SettingsContent(
            state = SettingsSampleData.initialState(),
            keywordDraft = "",
            keywordError = false,
            onKeywordDraftChange = {},
            onAddKeyword = {},
            onRemoveKeyword = {},
            onToggleTopic = { _, _ -> },
            onDefaultSummaryChange = {},
            onLanguageChange = {},
            onBiometricLockChange = {},
            onFontSizeChange = {},
            onFontTypeChange = {},
            onHighContrastChange = {},
            onThemeModeChange = {},
            onNavigate = {},
        )
    }
}

