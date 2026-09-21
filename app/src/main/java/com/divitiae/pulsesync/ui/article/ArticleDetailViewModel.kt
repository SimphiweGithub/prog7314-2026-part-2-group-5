package com.divitiae.pulsesync.ui.article

/*
 * ---------------------------------------------------------------------
 * CODE ATTRIBUTION
 * ---------------------------------------------------------------------
 * The ViewModel + StateFlow pattern, viewModelScope coroutine launching,
 * combine()-based UI-state derivation and the manual ViewModelProvider.Factory
 * in this file were adapted from:
 *
 * Android Developers (2026) ViewModel overview. [online]
 * Available at: https://developer.android.com/topic/libraries/architecture/viewmodel
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) StateFlow and SharedFlow. [online]
 * Available at: https://developer.android.com/kotlin/flow/stateflow-and-sharedflow
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) UI layer: expose UI state. [online]
 * Available at: https://developer.android.com/topic/architecture/ui-layer#expose-ui-state
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Create ViewModels with dependencies. [online]
 * Available at: https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories
 * [Accessed 20 September 2026].
 * ---------------------------------------------------------------------
 */

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.divitiae.pulsesync.data.domain.AppError
import com.divitiae.pulsesync.data.domain.Article
import com.divitiae.pulsesync.data.domain.DownloadSlot
import com.divitiae.pulsesync.data.domain.Note
import com.divitiae.pulsesync.data.domain.UserPreferences
import com.divitiae.pulsesync.data.repository.ArticleRepository
import com.divitiae.pulsesync.data.repository.DownloadRepository
import com.divitiae.pulsesync.data.repository.NoteRepository
import com.divitiae.pulsesync.data.repository.PreferencesRepository
import com.divitiae.pulsesync.ui.common.UiState
import com.divitiae.pulsesync.ui.common.toUiState
import com.divitiae.pulsesync.ui.feed.ArticleUi
import com.divitiae.pulsesync.ui.feed.NoteSyncState
import com.divitiae.pulsesync.ui.feed.SummaryMode
import com.divitiae.pulsesync.ui.feed.toUi
import com.divitiae.pulsesync.ui.mapper.toUi
import com.divitiae.pulsesync.ui.viewmodel.containerViewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

/**
 * What the Article Detail screen renders once the article has loaded.
 * [visibleSummary] is the DualMode result: the array currently selected out of
 * `aiSummary.detailed` / `aiSummary.condensed`.
 */
data class ArticleDetailUiState(
    val article: ArticleUi,
    val summaryMode: SummaryMode,
    val visibleSummary: List<String>,
    // Notes editor (title, body, tag chips) — mirrors Member 2's NotesEditorCard inputs.
    val noteTitle: String,
    val noteText: String,
    val noteTags: List<String>,
    val tagDraft: String,
    val noteSyncState: NoteSyncState,
    val titleError: Boolean,
    val bodyError: Boolean,
    val isSavingNote: Boolean,
)

/** One-shot outcomes of a Save Note tap, resolved to strings by the screen. */
sealed interface NoteSaveEvent {
    /** Title and/or body blank; the red highlights are already set in state. */
    data object Empty : NoteSaveEvent
    data object Synced : NoteSaveEvent
    data object SavedLocally : NoteSaveEvent
    data class Failed(val error: AppError) : NoteSaveEvent
}

/** Result of [ArticleDetailViewModel.addTag]; the screen shows a Toast for the two failures. */
enum class TagResult { ADDED, BLANK, DUPLICATE }

/**
 * Member 4 — hosts the three custom features for one article:
 *
 *  - **User Defined 1 (DualMode)**: [setSummaryMode]/[toggleSummaryMode] swap
 *    `aiSummary.detailed` ↔ `aiSummary.condensed` purely in memory; both
 *    arrays are already on the [ArticleUi], so the switch is instant and works
 *    offline. The initial mode comes from the Settings default.
 *  - **User Defined 2 (Resource intents)**: the ViewModel only exposes the
 *    list; launching happens in the screen via `ExternalLinks` because an
 *    Intent needs a Context.
 *  - **User Defined 3 (Contextual notes)**: [saveNote] runs
 *    `NoteRepository.saveNote` in `viewModelScope` (Kotlin coroutine on
 *    Dispatchers.IO). The repository writes Room first, then POSTs
 *    `/api/v1/notes`; whether the POST succeeded is reflected by
 *    `Note.isSynced`, which drives the Synced/Local chip.
 */
class ArticleDetailViewModel(
    private val articleId: String,
    private val articleRepository: ArticleRepository,
    private val noteRepository: NoteRepository,
    private val downloadRepository: DownloadRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    init {
        Log.d(TAG, "ArticleDetailViewModel initialized for articleId=$articleId")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ArticleDetailViewModel onCleared for articleId=$articleId")
    }

    /**
     * Ephemeral editor state. Nullable fields mean "user hasn't touched this
     * yet — show the stored note's value"; once the user types, the draft wins
     * until a successful save clears it back to null.
     */
    private data class Editor(
        val summaryMode: SummaryMode? = null,
        val titleDraft: String? = null,
        val bodyDraft: String? = null,
        val tagsDraft: List<String>? = null,
        val tagInput: String = "",
        val titleError: Boolean = false,
        val bodyError: Boolean = false,
        val isSaving: Boolean = false,
    )

    private val editor = MutableStateFlow(Editor())

    private val _noteEvents = MutableStateFlow<NoteSaveEvent?>(null)
    val noteEvents: StateFlow<NoteSaveEvent?> = _noteEvents.asStateFlow()

    /** The most recent contextual note for this article, kept so saves update rather than duplicate. */
    private var existingNote: Note? = null

    val uiState: StateFlow<UiState<ArticleDetailUiState>> = combine(
        articleRepository.observeArticle(articleId),
        noteRepository.observeForArticle(articleId),
        downloadRepository.observeSlots(),
        preferencesRepository.preferences,
        editor,
        ::buildState,
    )
        .catch { e -> emit(AppError.Unknown(e.message, e).toUiState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private fun buildState(
        article: Article?,
        notes: List<Note>,
        slots: List<DownloadSlot>,
        prefs: UserPreferences,
        ed: Editor,
    ): UiState<ArticleDetailUiState> {
        if (article == null) {
            return UiState.Error(
                message = "Article not found",
                error = AppError.Http(404, "Article $articleId is not cached"),
                retryable = false,
            )
        }
        val latest = notes.maxByOrNull { it.updatedAt }
        existingNote = latest

        val articleUi = article.toUi(offlineSlotsUsed = slots.size, note = latest?.toUi())
        val mode = ed.summaryMode ?: prefs.defaultSummaryMode.toUi()

        val title = ed.titleDraft ?: latest?.title.orEmpty()
        val body = ed.bodyDraft ?: latest?.plainText.orEmpty()
        val tags = ed.tagsDraft ?: latest?.tags.orEmpty()
        val dirty = ed.titleDraft != null || ed.bodyDraft != null || ed.tagsDraft != null

        return UiState.Success(
            ArticleDetailUiState(
                article = articleUi,
                summaryMode = mode,
                visibleSummary = articleUi.summaryFor(mode),
                noteTitle = title,
                noteText = body,
                noteTags = tags,
                tagDraft = ed.tagInput,
                noteSyncState = when {
                    dirty -> NoteSyncState.LOCAL // unsaved edits are by definition not on the server
                    latest?.isSynced == true -> NoteSyncState.SYNCED
                    else -> NoteSyncState.LOCAL
                },
                titleError = ed.titleError,
                bodyError = ed.bodyError,
                isSavingNote = ed.isSaving,
            ),
        )
    }

    // ---- User Defined 1: DualMode toggle -------------------------------------------------

    fun toggleSummaryMode() {
        val current = (uiState.value as? UiState.Success)?.data?.summaryMode ?: SummaryMode.DETAILED
        editor.update { it.copy(summaryMode = current.toggled()) }
    }

    fun setSummaryMode(mode: SummaryMode) = editor.update { it.copy(summaryMode = mode) }

    // ---- Bookmark -----------------------------------------------------------------------------

    fun toggleSave() {
        val saved = (uiState.value as? UiState.Success)?.data?.article?.isSaved ?: return
        viewModelScope.launch { articleRepository.setBookmarked(articleId, !saved) }
    }

    // ---- User Defined 3: contextual notes -----------------------------------------------------

    fun onNoteTitleChange(text: String) =
        editor.update { it.copy(titleDraft = text, titleError = false) } // clear highlight as the user types

    fun onNoteTextChange(text: String) =
        editor.update { it.copy(bodyDraft = text, bodyError = false) }

    fun onTagDraftChange(text: String) = editor.update { it.copy(tagInput = text) }

    /** Validates the tag draft (blank / duplicate, case-insensitive) and appends it. */
    fun addTag(): TagResult {
        val state = (uiState.value as? UiState.Success)?.data ?: return TagResult.BLANK
        val tag = state.tagDraft.trim()
        return when {
            tag.isEmpty() -> TagResult.BLANK
            state.noteTags.any { it.equals(tag, ignoreCase = true) } -> TagResult.DUPLICATE
            else -> {
                editor.update { it.copy(tagsDraft = state.noteTags + tag, tagInput = "") }
                TagResult.ADDED
            }
        }
    }

    fun removeTag(tag: String) {
        val current = (uiState.value as? UiState.Success)?.data?.noteTags ?: return
        editor.update { it.copy(tagsDraft = current - tag) }
    }

    /**
     * Validates title and body (Member 2's rule: both required → red highlight
     * + Snackbar), then persists asynchronously. Runs on viewModelScope so a
     * rotation mid-save does not cancel the POST.
     */
    fun saveNote() {
        val state = (uiState.value as? UiState.Success)?.data ?: return
        val title = state.noteTitle.trim()
        val body = state.noteText.trim()
        val titleBlank = title.isBlank()
        val bodyBlank = body.isBlank()
        if (titleBlank || bodyBlank) {
            editor.update { it.copy(titleError = titleBlank, bodyError = bodyBlank) }
            _noteEvents.value = NoteSaveEvent.Empty
            return
        }
        if (state.isSavingNote) return

        editor.update { it.copy(titleError = false, bodyError = false, isSaving = true) }
        viewModelScope.launch {
            val event = try {
                val saved = noteRepository.saveNote(
                    title = title,
                    bodyHtml = body,
                    plainText = body,
                    tags = state.noteTags,
                    articleId = articleId,
                    isVaultNote = false,
                    localId = existingNote?.localId ?: UUID.randomUUID().toString(),
                )
                // Drop the drafts so the stored copy (now the source of truth) is shown.
                editor.update { it.copy(titleDraft = null, bodyDraft = null, tagsDraft = null) }
                if (saved.isSynced) NoteSaveEvent.Synced else NoteSaveEvent.SavedLocally
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                NoteSaveEvent.Failed(AppError.Network(e.message))
            } catch (e: Exception) {
                NoteSaveEvent.Failed(AppError.Unknown(e.message, e))
            } finally {
                editor.update { it.copy(isSaving = false) }
            }
            _noteEvents.value = event
        }
    }

    fun consumeNoteEvent() {
        _noteEvents.value = null
    }

    companion object {
        private const val TAG = "ArticleDetailViewModel"
        fun factory(articleId: String): ViewModelProvider.Factory = containerViewModelFactory { c ->
            ArticleDetailViewModel(
                articleId = articleId,
                articleRepository = c.articleRepository,
                noteRepository = c.noteRepository,
                downloadRepository = c.downloadRepository,
                preferencesRepository = c.preferencesRepository,
            )
        }
    }
}

/**
 * User Defined Feature 1 — the actual DualMode selection. Pure function so it
 * is unit-testable without Android. Falls back sensibly when the backend
 * omits one of the arrays.
 */
fun ArticleUi.summaryFor(mode: SummaryMode): List<String> = when (mode) {
    SummaryMode.DETAILED -> detailedSummary.ifEmpty { body }
    SummaryMode.CONDENSED -> condensedSummary.ifEmpty { detailedSummary.take(3) }
}