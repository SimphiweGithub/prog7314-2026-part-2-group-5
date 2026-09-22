package com.divitiae.pulsesync

import com.divitiae.pulsesync.data.domain.AppError
import com.divitiae.pulsesync.data.domain.Category
import com.divitiae.pulsesync.data.domain.DownloadSlot
import com.divitiae.pulsesync.data.domain.FontSizePref
import com.divitiae.pulsesync.data.domain.Keyword
import com.divitiae.pulsesync.data.domain.Result
import com.divitiae.pulsesync.data.domain.SummaryModePref
import com.divitiae.pulsesync.data.domain.ThemePref
import com.divitiae.pulsesync.data.domain.UserPreferences
import com.divitiae.pulsesync.data.repository.CategoryRepository
import com.divitiae.pulsesync.data.repository.DownloadRepository
import com.divitiae.pulsesync.data.repository.KeywordRepository
import com.divitiae.pulsesync.data.repository.PreferencesRepository
import com.divitiae.pulsesync.testutil.MainDispatcherRule
import com.divitiae.pulsesync.ui.common.UiState
import com.divitiae.pulsesync.ui.feed.SummaryMode
import com.divitiae.pulsesync.ui.settings.FontSizePreference
import com.divitiae.pulsesync.ui.settings.KeywordValidation
import com.divitiae.pulsesync.ui.settings.SettingsViewModel
import com.divitiae.pulsesync.ui.settings.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * [SettingsViewModel] state and persistence tests (Feature: Settings menu).
 *
 * Verifies that every Settings control ends up in a real store rather than
 * screen memory: preferences go to DataStore and are mirrored to the cloud,
 * keywords are validated then written through the keyword repository, and
 * topic toggles flip the category subscription.
 *
 * Code Attribution No 2
 * This method was taken from "Testing ViewModels with StateFlow and runTest"
 * https://developer.android.com/topic/architecture/ui-layer/state-production#testing
 * Android Developers
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class Fixture(
        val preferences: PreferencesRepository,
        val keywords: KeywordRepository,
        val categories: CategoryRepository,
        val downloads: DownloadRepository,
        val viewModel: SettingsViewModel,
    )

    /** Suspend so the `pushToCloud` (suspend) stub can be set inside the test's coroutine. */
    private suspend fun fixture(
        prefs: UserPreferences = UserPreferences(),
        keywords: List<Keyword> = emptyList(),
        categories: List<Category> = emptyList(),
        slots: List<DownloadSlot> = emptyList(),
        push: Result<Unit> = Result.Success(Unit),
    ): Fixture {
        val preferencesRepository: PreferencesRepository = mock()
        val keywordRepository: KeywordRepository = mock()
        val categoryRepository: CategoryRepository = mock()
        val downloadRepository: DownloadRepository = mock()

        whenever(preferencesRepository.preferences).thenReturn(flowOf(prefs))
        whenever(preferencesRepository.pushToCloud()).thenReturn(push)
        whenever(keywordRepository.observeAll()).thenReturn(flowOf(keywords))
        whenever(categoryRepository.observeAll()).thenReturn(flowOf(categories))
        whenever(downloadRepository.observeSlots()).thenReturn(flowOf(slots))

        return Fixture(
            preferences = preferencesRepository,
            keywords = keywordRepository,
            categories = categoryRepository,
            downloads = downloadRepository,
            viewModel = SettingsViewModel(
                preferencesRepository = preferencesRepository,
                keywordRepository = keywordRepository,
                categoryRepository = categoryRepository,
                downloadRepository = downloadRepository,
            ),
        )
    }

    /** `stateIn(WhileSubscribed)` only collects upstream once something subscribes. */
    private fun TestScope.subscribe(fixture: Fixture) {
        backgroundScope.launch { fixture.viewModel.uiState.collect { } }
        advanceUntilIdle()
    }

    private val eskom = Keyword(id = "kw-1", keyword = "eskom")
    private val nsfas = Keyword(id = "kw-2", keyword = "nsfas")
    private val bursaries = Category(slug = "bursaries", nameEn = "Bursaries", nameZu = "", nameAf = "", isSubscribed = true)

    // ---- State mapping ----------------------------------------------------------------

    @Test
    fun uiState_startsLoadingThenMapsStoredValues() = runTest {
        val fixture = fixture(
            prefs = UserPreferences(
                defaultSummaryMode = SummaryModePref.CONDENSED,
                language = "Afrikaans",
                biometricLock = true,
                themeMode = ThemePref.DARK,
                fontSize = FontSizePref.LARGE,
                fontType = "Serif",
                highContrast = true,
            ),
            keywords = listOf(eskom, nsfas),
            categories = listOf(bursaries),
            slots = listOf(DownloadSlot("a1", 10, 0L), DownloadSlot("a2", 10, 0L)),
        )
        assertTrue("Before subscription the state is Loading", fixture.viewModel.uiState.value is UiState.Loading)

        subscribe(fixture)

        val state = fixture.viewModel.uiState.value
        assertTrue("Stored values must produce Success", state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals(SummaryMode.CONDENSED, data.defaultSummaryMode)
        assertEquals("Afrikaans", data.language)
        assertEquals(true, data.biometricLock)
        assertEquals(ThemeMode.DARK, data.themeMode)
        assertEquals(FontSizePreference.LARGE, data.fontSize)
        assertEquals("Serif", data.fontType)
        assertEquals(true, data.highContrast)
        assertEquals(listOf("eskom", "nsfas"), data.keywords)
        assertEquals(1, data.topics.size)
        assertEquals("Bursaries", data.topics.first().name)
        assertEquals(true, data.topics.first().enabled)
        assertEquals(2, data.offlineSlotsUsed)
    }

    // ---- Preferences: DataStore write + cloud mirror -----------------------------------

    @Test
    fun setDefaultSummaryMode_writesDataStoreAndMirrorsToCloud() = runTest {
        val fixture = fixture()
        subscribe(fixture)

        fixture.viewModel.setDefaultSummaryMode(SummaryMode.CONDENSED)
        advanceUntilIdle()

        verify(fixture.preferences).setSummaryMode(SummaryModePref.CONDENSED)
        verify(fixture.preferences).pushToCloud()
        assertEquals(SettingsViewModel.SyncEvent.Synced, fixture.viewModel.syncEvent.value)

        fixture.viewModel.consumeSyncEvent()
        assertNull("consumeSyncEvent must clear the event", fixture.viewModel.syncEvent.value)
    }

    @Test
    fun setThemeMode_writesThemePref() = runTest {
        val fixture = fixture()
        subscribe(fixture)

        fixture.viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        verify(fixture.preferences).setThemeMode(ThemePref.LIGHT)
    }

    @Test
    fun burstOfChanges_isDebouncedIntoOneCloudPush() = runTest {
        val fixture = fixture()
        subscribe(fixture)

        fixture.viewModel.setBiometricLock(true)
        fixture.viewModel.setHighContrast(true)
        fixture.viewModel.setFontSize(FontSizePreference.SMALL)
        fixture.viewModel.setLanguage("isiZulu")
        advanceUntilIdle()

        verify(fixture.preferences).setBiometricLock(true)
        verify(fixture.preferences).setHighContrast(true)
        verify(fixture.preferences).setFontSize(FontSizePref.SMALL)
        verify(fixture.preferences).setLanguage("isiZulu")
        verify(fixture.preferences, times(1)).pushToCloud()
    }

    @Test
    fun cloudPushFailure_keepsDeviceCopyAndEmitsFailedEvent() = runTest {
        val fixture = fixture(push = Result.Failure(AppError.Network("offline")))
        subscribe(fixture)

        fixture.viewModel.setFontType("Monospace")
        advanceUntilIdle()

        verify(fixture.preferences).setFontType("Monospace")
        val event = fixture.viewModel.syncEvent.value
        assertTrue("A failed mirror must surface as SyncEvent.Failed", event is SettingsViewModel.SyncEvent.Failed)
        assertEquals("offline", (event as SettingsViewModel.SyncEvent.Failed).error.message)
    }

    // ---- Keywords: validate, then persist -----------------------------------------------

    @Test
    fun addKeyword_blankIsRejectedWithoutPersisting() = runTest {
        val fixture = fixture(keywords = listOf(eskom))
        subscribe(fixture)

        val result = fixture.viewModel.addKeyword("   ")
        advanceUntilIdle()

        assertEquals(KeywordValidation.Result.Blank, result)
        verify(fixture.keywords, never()).addKeyword(any())
    }

    @Test
    fun addKeyword_duplicateIsRejectedCaseInsensitively() = runTest {
        val fixture = fixture(keywords = listOf(eskom))
        subscribe(fixture)

        val result = fixture.viewModel.addKeyword("  ESKOM ")
        advanceUntilIdle()

        assertEquals(KeywordValidation.Result.Duplicate, result)
        verify(fixture.keywords, never()).addKeyword(any())
    }

    @Test
    fun addKeyword_validIsNormalisedAndPersisted() = runTest {
        val fixture = fixture(keywords = listOf(eskom))
        whenever(fixture.keywords.addKeyword("nsfas")).thenReturn(nsfas)
        subscribe(fixture)

        val result = fixture.viewModel.addKeyword("  NSFAS ")
        advanceUntilIdle()

        assertEquals(KeywordValidation.Result.Valid("nsfas"), result)
        verify(fixture.keywords).addKeyword("nsfas")
    }

    @Test
    fun removeKeyword_resolvesIdFromTrackedList() = runTest {
        val fixture = fixture(keywords = listOf(eskom, nsfas))
        subscribe(fixture)

        fixture.viewModel.removeKeyword("nsfas")
        fixture.viewModel.removeKeyword("not-tracked")
        advanceUntilIdle()

        verify(fixture.keywords).removeKeyword("kw-2")
        verify(fixture.keywords, times(1)).removeKeyword(any())
    }

    // ---- Topics -----------------------------------------------------------------------

    @Test
    fun toggleTopic_flipsCategorySubscriptionBySlug() = runTest {
        val fixture = fixture(categories = listOf(bursaries))
        subscribe(fixture)

        fixture.viewModel.toggleTopic("Bursaries", enabled = false)
        fixture.viewModel.toggleTopic("Unknown", enabled = true)
        advanceUntilIdle()

        verify(fixture.categories).setSubscribed("bursaries", false)
        verify(fixture.categories, times(1)).setSubscribed(any(), any())
    }
}
