package com.divitiae.pulsesync.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.divitiae.pulsesync.data.domain.AppError
import com.divitiae.pulsesync.data.domain.Category
import com.divitiae.pulsesync.data.domain.DownloadSlot
import com.divitiae.pulsesync.data.domain.Keyword
import com.divitiae.pulsesync.data.domain.Result
import com.divitiae.pulsesync.data.domain.UserPreferences
import com.divitiae.pulsesync.data.repository.CategoryRepository
import com.divitiae.pulsesync.data.repository.DownloadRepository
import com.divitiae.pulsesync.data.repository.KeywordRepository
import com.divitiae.pulsesync.data.repository.PreferencesRepository
import com.divitiae.pulsesync.ui.common.UiState
import com.divitiae.pulsesync.ui.common.toUiState
import com.divitiae.pulsesync.ui.feed.SummaryMode
import com.divitiae.pulsesync.ui.feed.toUi
import com.divitiae.pulsesync.ui.viewmodel.containerViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs Member 2's stateless [SettingsContent] with persisted data.
 *
 * Every control writes to a real store rather than screen memory:
 *  - preferences (summary default, language, biometric lock, theme, font,
 *    contrast) go to DataStore via [PreferencesRepository], then are mirrored
 *    to `PUT /api/v1/preferences` after a short debounce;
 *  - tracked keywords go to Room + `POST`/`DELETE /api/v1/keywords` via
 *    [KeywordRepository];
 *  - topic toggles flip the category subscription in Room.
 *
 * The screen only observes [uiState] and forwards taps, so a change made here
 * is also what the Feed and Article screens read (they observe the same
 * DataStore flow), and it survives process death.
 */
class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val keywordRepository: KeywordRepository,
    private val categoryRepository: CategoryRepository,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    /** One-shot outcome of the debounced cloud mirror; the screen shows a Toast on failure. */
    sealed interface SyncEvent {
        data object Synced : SyncEvent
        data class Failed(val error: AppError) : SyncEvent
    }

    init {
        Log.d(TAG, "SettingsViewModel initialized")
    }

    private val _syncEvent = MutableStateFlow<SyncEvent?>(null)
    val syncEvent: StateFlow<SyncEvent?> = _syncEvent.asStateFlow()

    // Latest snapshots so keyword/topic actions can resolve ids without a
    // second database round-trip.
    @Volatile
    private var latestKeywords: List<Keyword> = emptyList()

    @Volatile
    private var latestCategories: List<Category> = emptyList()

    val uiState: StateFlow<UiState<SettingsUiState>> = combine(
        preferencesRepository.preferences,
        keywordRepository.observeAll(),
        categoryRepository.observeAll(),
        downloadRepository.observeSlots(),
        ::buildState,
    )
        .catch { e -> emit(AppError.Unknown(e.message, e).toUiState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private fun buildState(
        prefs: UserPreferences,
        keywords: List<Keyword>,
        categories: List<Category>,
        slots: List<DownloadSlot>,
    ): UiState<SettingsUiState> {
        latestKeywords = keywords
        latestCategories = categories
        return UiState.Success(
            SettingsUiState(
                defaultSummaryMode = prefs.defaultSummaryMode.toUi(),
                topics = categories.map { TopicToggle(name = it.nameEn, enabled = it.isSubscribed) },
                keywords = keywords.map { it.keyword },
                offlineSlotsUsed = slots.size,
                offlineSlotsTotal = OFFLINE_SLOTS_TOTAL,
                queuedNotes = 0,
                language = prefs.language,
                languages = LANGUAGES,
                biometricLock = prefs.biometricLock,
                fontSize = prefs.fontSize.toUi(),
                fontType = prefs.fontType,
                fontTypes = FONT_TYPES,
                highContrast = prefs.highContrast,
                themeMode = prefs.themeMode.toUi(),
            ),
        )
    }

    // ---- Preferences (DataStore first, then cloud mirror) ---------------------------------

    fun setDefaultSummaryMode(mode: SummaryMode) =
        persist("defaultSummaryMode=$mode") { preferencesRepository.setSummaryMode(mode.toPref()) }

    fun setLanguage(language: String) =
        persist("language=$language") { preferencesRepository.setLanguage(language) }

    fun setBiometricLock(enabled: Boolean) =
        persist("biometricLock=$enabled") { preferencesRepository.setBiometricLock(enabled) }

    fun setThemeMode(mode: ThemeMode) =
        persist("themeMode=$mode") { preferencesRepository.setThemeMode(mode.toPref()) }

    fun setFontSize(size: FontSizePreference) =
        persist("fontSize=$size") { preferencesRepository.setFontSize(size.toPref()) }

    fun setFontType(type: String) =
        persist("fontType=$type") { preferencesRepository.setFontType(type) }

    fun setHighContrast(enabled: Boolean) =
        persist("highContrast=$enabled") { preferencesRepository.setHighContrast(enabled) }

    private var pushJob: Job? = null

    private fun persist(label: String, write: suspend () -> Unit) {
        viewModelScope.launch {
            Log.d(TAG, "persist: $label")
            write()
            schedulePush()
        }
    }

    /**
     * Debounced so a burst of toggles becomes one PUT. The on-device value is
     * already saved before this runs, which is why a failure is only a Toast
     * and never blocks the UI.
     */
    private fun schedulePush() {
        pushJob?.cancel()
        pushJob = viewModelScope.launch {
            delay(PUSH_DEBOUNCE_MS)
            when (val result = preferencesRepository.pushToCloud()) {
                is Result.Success -> {
                    Log.i(TAG, "schedulePush: preferences mirrored to cloud")
                    _syncEvent.value = SyncEvent.Synced
                }

                is Result.Failure -> {
                    Log.w(TAG, "schedulePush: cloud mirror failed: ${result.error}")
                    _syncEvent.value = SyncEvent.Failed(result.error)
                }
            }
        }
    }

    fun consumeSyncEvent() {
        _syncEvent.value = null
    }

    // ---- Tracked keywords ----------------------------------------------------------------

    /**
     * Validates [draft] against the keywords currently tracked (blank /
     * duplicate, case-insensitive) and, when valid, persists it. The result is
     * returned synchronously so the screen can show its Toast / red highlight.
     */
    fun addKeyword(draft: String): KeywordValidation.Result {
        val result = KeywordValidation.validate(draft, latestKeywords.map { it.keyword })
        if (result is KeywordValidation.Result.Valid) {
            viewModelScope.launch {
                Log.d(TAG, "addKeyword: persisting '${result.keyword}'")
                keywordRepository.addKeyword(result.keyword)
            }
        } else {
            Log.w(TAG, "addKeyword: rejected draft '$draft' ($result)")
        }
        return result
    }

    fun removeKeyword(keyword: String) {
        val target = KeywordValidation.normalise(keyword)
        val match = latestKeywords.firstOrNull { KeywordValidation.normalise(it.keyword) == target }
        if (match == null) {
            Log.w(TAG, "removeKeyword: '$keyword' is not tracked")
            return
        }
        viewModelScope.launch {
            Log.d(TAG, "removeKeyword: removing '${match.keyword}' (id=${match.id})")
            keywordRepository.removeKeyword(match.id)
        }
    }

    // ---- Topic subscriptions -------------------------------------------------------------

    fun toggleTopic(name: String, enabled: Boolean) {
        val category = latestCategories.firstOrNull { it.nameEn == name }
        if (category == null) {
            Log.w(TAG, "toggleTopic: unknown topic '$name'")
            return
        }
        viewModelScope.launch {
            Log.d(TAG, "toggleTopic: ${category.slug} subscribed=$enabled")
            categoryRepository.setSubscribed(category.slug, enabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "SettingsViewModel onCleared")
    }

    companion object {
        private const val TAG = "SettingsViewModel"
        private const val PUSH_DEBOUNCE_MS = 500L
        private const val OFFLINE_SLOTS_TOTAL = 5

        val LANGUAGES: List<String> = listOf("English", "Afrikaans", "isiZulu", "isiXhosa")
        val FONT_TYPES: List<String> = listOf("Default", "Serif", "Monospace")

        val Factory: ViewModelProvider.Factory = containerViewModelFactory { c ->
            SettingsViewModel(
                preferencesRepository = c.preferencesRepository,
                keywordRepository = c.keywordRepository,
                categoryRepository = c.categoryRepository,
                downloadRepository = c.downloadRepository,
            )
        }
    }
}
