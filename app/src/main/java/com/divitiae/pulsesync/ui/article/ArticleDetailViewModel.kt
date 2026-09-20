package com.divitiae.pulsesync.ui.article

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

/**
 * What the Article Detail screen renders once the article has loaded.
 * [visibleSummary] is the DualMode result: the array currently selected out of
 * `aiSummary.detailed` / `aiSummary.condensed`.
 */
data class ArticleDetailUiState(
    val article: ArticleUi,
    val summaryMode: SummaryMode,
    val visibleSummary: List<String>,
    val noteText: String,
    val noteSyncState: NoteSyncState,
    val noteError: Boolean,
    val isSavingNote: Boolean,
)

/** One-shot outcomes of a Save Note tap, resolved to strings by the screen. */
sealed interface NoteSaveEvent {
    data object Empty : NoteSaveEvent
    data object Synced : NoteSaveEvent
    data object SavedLocally : NoteSaveEvent
    data class Failed(val error: AppError) : NoteSaveEvent
}

/**
 * Member 4 — hosts the three custom features for one article:
 *
 *  - **User Defined 1 (DualMode)**: [toggleSummaryMode] swaps
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

    private data class Editor(
        /** null = user has not toggled; use the Settings default. */
        val summaryMode: SummaryMode? = null,
        /** null = user has not typed; show the stored note text. */
        val draft: String? = null,
        val noteError: Boolean = false,
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

        return UiState.Success(
            ArticleDetailUiState(
                article = articleUi,
                summaryMode = mode,
                visibleSummary = articleUi.summaryFor(mode),
                noteText = ed.draft ?: latest?.plainText.orEmpty(),
                noteSyncState = when {
                    ed.draft != null && ed.draft != latest?.plainText -> NoteSyncState.LOCAL // unsaved edits
                    latest?.isSynced == true -> NoteSyncState.SYNCED
                    else -> NoteSyncState.LOCAL
                },
                noteError = ed.noteError,
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

    fun onNoteTextChange(text: String) {
        editor.update { it.copy(draft = text, noteError = if (text.isNotBlank()) false else it.noteError) }
    }

    /**
     * Validates, then persists asynchronously. Empty content is rejected with
     * the red highlight + [NoteSaveEvent.Empty] (Member 2's existing UX).
     */
    fun saveNote() {
        val state = (uiState.value as? UiState.Success)?.data ?: return
        val text = state.noteText.trim()
        if (text.isBlank()) {
            editor.update { it.copy(noteError = true) }
            _noteEvents.value = NoteSaveEvent.Empty
            return
        }
        if (state.isSavingNote) return

        editor.update { it.copy(noteError = false, isSaving = true) }
        viewModelScope.launch {
            val event = try {
                val saved = noteRepository.saveNote(
                    title = state.article.title.take(MAX_TITLE_LENGTH),
                    bodyHtml = text,
                    plainText = text,
                    tags = existingNote?.tags ?: emptyList(),
                    articleId = articleId,
                    isVaultNote = false,
                    localId = existingNote?.localId ?: java.util.UUID.randomUUID().toString(),
                )
                // Clear the draft so the stored copy (now the source of truth) is shown.
                editor.update { it.copy(draft = null) }
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
        private const val MAX_TITLE_LENGTH = 80

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