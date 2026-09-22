package com.divitiae.pulsesync.ui.settings

/**
 * Code Attribution No 25
 * This method was taken from "Toasts overview and Flow layouts in Compose"
 * https://developer.android.com/guide/topics/ui/notifiers/toasts
 * Android Developers
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.divitiae.pulsesync.R
import com.divitiae.pulsesync.ui.common.UiStateBoundary
import com.divitiae.pulsesync.ui.common.toMessageRes
import com.divitiae.pulsesync.ui.components.BottomDestination
import com.divitiae.pulsesync.ui.components.PulseSyncBottomBar
import com.divitiae.pulsesync.ui.components.PulseSyncDimens
import com.divitiae.pulsesync.ui.feed.SummaryMode
import com.divitiae.pulsesync.ui.theme.PulseSyncTheme

/**
 * Preferences & Settings (Figma 9:35, dark 29:295).
 *
 * The stateful host: observes [SettingsViewModel.uiState] (DataStore + Room)
 * and forwards every control to the ViewModel, which persists it on-device
 * and mirrors preferences to the cloud. Only the keyword draft and its error
 * highlight are screen-local, because they are transient input state.
 * [SettingsContent] below is Member 2's stateless layout, unchanged.
 */
@Composable
fun SettingsScreen(
    onNavigate: (BottomDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    val syncEvent by viewModel.syncEvent.collectAsState()
    var keywordDraft by rememberSaveable { mutableStateOf("") }
    var keywordError by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    // Cloud mirror outcome: the device copy is already saved, so a failure is
    // only informational. Success stays quiet (it is logged by the ViewModel).
    val syncFailedTemplate = stringResource(R.string.settings_cloud_sync_failed)
    val syncFailedDetail = (syncEvent as? SettingsViewModel.SyncEvent.Failed)
        ?.let { stringResource(it.error.toMessageRes()) }
    LaunchedEffect(syncEvent) {
        when (syncEvent) {
            is SettingsViewModel.SyncEvent.Failed -> {
                Toast.makeText(context, syncFailedTemplate.format(syncFailedDetail ?: ""), Toast.LENGTH_SHORT).show()
                viewModel.consumeSyncEvent()
            }
            SettingsViewModel.SyncEvent.Synced -> viewModel.consumeSyncEvent()
            null -> Unit
        }
    }

    // DataStore/Room flows do not fail in practice; the boundary is here so a
    // storage error renders a pane instead of crashing the screen.
    UiStateBoundary(state = state, onRetry = {}, modifier = modifier) { settings ->
        SettingsContent(
            state = settings,
            keywordDraft = keywordDraft,
            keywordError = keywordError,
            onKeywordDraftChange = {
                keywordDraft = it
                keywordError = false // Clear the highlight as soon as the user edits.
            },
            onAddKeyword = {
                when (viewModel.addKeyword(keywordDraft)) {
                    is KeywordValidation.Result.Valid -> {
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
            onRemoveKeyword = viewModel::removeKeyword,
            onToggleTopic = viewModel::toggleTopic,
            onDefaultSummaryChange = viewModel::setDefaultSummaryMode,
            onLanguageChange = viewModel::setLanguage,
            onBiometricLockChange = viewModel::setBiometricLock,
            onFontSizeChange = viewModel::setFontSize,
            onFontTypeChange = viewModel::setFontType,
            onHighContrastChange = viewModel::setHighContrast,
            onThemeModeChange = viewModel::setThemeMode,
            onNavigate = onNavigate,
        )
    }
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
