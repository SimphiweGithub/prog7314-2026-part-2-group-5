package com.divitiae.pulsesync.ui.article

/**
 * Code Attribution No 17
 * This method was taken from "Scaffold and TopAppBar in Jetpack Compose"
 * https://developer.android.com/develop/ui/compose/components/scaffold
 * Android Developers
 */

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.divitiae.pulsesync.R
import com.divitiae.pulsesync.ui.common.UiStateBoundary
import com.divitiae.pulsesync.ui.common.toMessageRes
import com.divitiae.pulsesync.ui.components.PulseSyncDimens
import com.divitiae.pulsesync.ui.feed.ArticleUi
import com.divitiae.pulsesync.ui.feed.FeedSampleData
import com.divitiae.pulsesync.ui.feed.NoteSyncState
import com.divitiae.pulsesync.ui.feed.ResourceLinkUi
import com.divitiae.pulsesync.ui.feed.SummaryMode
import com.divitiae.pulsesync.ui.theme.PulseSyncTheme
import com.divitiae.pulsesync.ui.util.ExternalLinks
import kotlinx.coroutines.launch

/**
 * Article Detail & Interactive Workspace (Figma 6:35, dark 29:168).
 *
 * Hosts all three User Defined Features. The layout and the notes editor UX
 * (title, body, tag chips; empty title/body validation → red highlight +
 * Snackbar; blank or duplicate tag → Toast) are Member 2's; Member 4 backs the
 * screen with [ArticleDetailViewModel] so that:
 *
 *  - User Defined 1: [SummaryModeToggle] swaps `aiSummary.detailed` ↔
 *    `aiSummary.condensed` in memory.
 *  - User Defined 2: resource rows fire explicit ACTION_VIEW intents via
 *    [ExternalLinks], with a Snackbar when no app can handle the link.
 *  - User Defined 3: Save Note runs a coroutine that writes Room and POSTs
 *    `/api/v1/notes`; the Synced / Saved-locally chip reflects the result.
 *  - Robustness: [UiStateBoundary] renders Loading / Error / Success so a
 *    missing article never crashes the screen.
 */
@Composable
fun ArticleDetailScreen(
    articleId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArticleDetailViewModel = viewModel(
        key = "article-$articleId",
        factory = ArticleDetailViewModel.factory(articleId),
    ),
) {
    val context = LocalContext.current
    // Adapted from: Android Developers (2026) Snackbar. https://developer.android.com/develop/ui/compose/components/snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val state by viewModel.uiState.collectAsState()
    val noteEvent by viewModel.noteEvents.collectAsState()

    val emptyNoteMessage = stringResource(R.string.article_notes_empty_error)
    val noteSyncedMessage = stringResource(R.string.article_notes_saved)
    val noteLocalMessage = stringResource(R.string.article_notes_saved_local)
    val noteFailedTemplate = stringResource(R.string.article_notes_save_failed)
    val noteFailedDetail = (noteEvent as? NoteSaveEvent.Failed)?.let { stringResource(it.error.toMessageRes()) }
    val noAppMessage = stringResource(R.string.article_no_app_for_link)

    // One-shot feedback for the asynchronous note save.
    // Adapted from: Android Developers (2026) Side-effects in Compose. https://developer.android.com/develop/ui/compose/side-effects
    LaunchedEffect(noteEvent) {
        val text = when (noteEvent) {
            NoteSaveEvent.Empty -> emptyNoteMessage
            NoteSaveEvent.Synced -> noteSyncedMessage
            NoteSaveEvent.SavedLocally -> noteLocalMessage
            is NoteSaveEvent.Failed -> noteFailedTemplate.format(noteFailedDetail ?: "")
            null -> null
        }
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.consumeNoteEvent()
        }
    }

    UiStateBoundary(
        state = state,
        onRetry = onBack, // "Article not found" is not retryable; the pane offers a way out.
        modifier = modifier,
    ) { detail ->
        ArticleDetailContent(
            article = detail.article,
            summaryMode = detail.summaryMode,
            visibleSummary = detail.visibleSummary,
            onSummaryModeChange = viewModel::setSummaryMode,
            noteTitle = detail.noteTitle,
            onNoteTitleChange = viewModel::onNoteTitleChange,
            noteText = detail.noteText,
            onNoteTextChange = viewModel::onNoteTextChange,
            noteTags = detail.noteTags,
            tagDraft = detail.tagDraft,
            onTagDraftChange = viewModel::onTagDraftChange,
            onAddTag = {
                when (viewModel.addTag()) {
                    // Adapted from: Android Developers (2026) Toasts overview. https://developer.android.com/guide/topics/ui/notifiers/toasts
                    TagResult.BLANK ->
                        Toast.makeText(context, R.string.article_notes_tag_blank_error, Toast.LENGTH_SHORT).show()
                    TagResult.DUPLICATE ->
                        Toast.makeText(context, R.string.article_notes_tag_duplicate_error, Toast.LENGTH_SHORT).show()
                    TagResult.ADDED -> Unit
                }
            },
            onRemoveTag = viewModel::removeTag,
            noteSyncState = detail.noteSyncState,
            titleError = detail.titleError,
            bodyError = detail.bodyError,
            isSavingNote = detail.isSavingNote,
            snackbarHostState = snackbarHostState,
            onBack = onBack,
            onToggleSave = viewModel::toggleSave,
            onOpenSource = {
                if (!ExternalLinks.open(context, detail.article.sourceUrl)) {
                    scope.launch { snackbarHostState.showSnackbar(noAppMessage) }
                }
            },
            onOpenResource = { resource ->
                if (!ExternalLinks.open(context, resource.url, resource.type)) {
                    scope.launch { snackbarHostState.showSnackbar(noAppMessage) }
                }
            },
            onSaveNote = viewModel::saveNote,
        )
    }
}

@Composable
fun ArticleDetailContent(
    article: ArticleUi,
    summaryMode: SummaryMode,
    visibleSummary: List<String>,
    onSummaryModeChange: (SummaryMode) -> Unit,
    noteTitle: String,
    onNoteTitleChange: (String) -> Unit,
    noteText: String,
    onNoteTextChange: (String) -> Unit,
    noteTags: List<String>,
    tagDraft: String,
    onTagDraftChange: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    noteSyncState: NoteSyncState,
    titleError: Boolean,
    bodyError: Boolean,
    isSavingNote: Boolean,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onToggleSave: () -> Unit,
    onOpenSource: () -> Unit,
    onOpenResource: (ResourceLinkUi) -> Unit,
    onSaveNote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ArticleTopBar(isSaved = article.isSaved, onBack = onBack, onToggleSave = onToggleSave)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PulseSyncDimens.ScreenPadding)
                .padding(top = 16.dp, bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            SourceLinkRow(source = article.source, onClick = onOpenSource)
            AiDisclosureNotice()
            OfflineBadge(slotsUsed = article.offlineSlotsUsed, slotsTotal = article.offlineSlotsTotal)

            // User Defined Feature 1 — DualMode toggle + whichever aiSummary array is selected.
            SummaryModeToggle(mode = summaryMode, onModeChange = onSummaryModeChange)
            Column(
                verticalArrangement = Arrangement.spacedBy(
                    if (summaryMode == SummaryMode.DETAILED) 20.dp else 8.dp,
                ),
            ) {
                visibleSummary.forEach { line ->
                    Text(
                        text = if (summaryMode == SummaryMode.CONDENSED) {
                            stringResource(R.string.feed_bullet, line)
                        } else {
                            line
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            // User Defined Feature 2 — one explicit intent per extracted link.
            ResourceLinksPanel(resources = article.resources, onOpen = onOpenResource)

            // User Defined Feature 3 — contextual notes editor (Member 2's card, unchanged).
            NotesEditorCard(
                title = noteTitle,
                onTitleChange = onNoteTitleChange,
                text = noteText,
                onTextChange = onNoteTextChange,
                tags = noteTags,
                tagDraft = tagDraft,
                onTagDraftChange = onTagDraftChange,
                onAddTag = onAddTag,
                onRemoveTag = onRemoveTag,
                syncState = noteSyncState,
                isTitleError = titleError,
                isBodyError = bodyError,
                onSave = {
                    if (!isSavingNote) {
                        focusManager.clearFocus()
                        onSaveNote()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "Article · Light", showBackground = true, widthDp = 360, heightDp = 1000)
@Preview(
    name = "Article · Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 1000,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ArticleDetailPreview() {
    val article = FeedSampleData.articles.first()
    PulseSyncTheme {
        ArticleDetailContent(
            article = article,
            summaryMode = SummaryMode.DETAILED,
            visibleSummary = article.summaryFor(SummaryMode.DETAILED),
            onSummaryModeChange = {},
            noteTitle = article.note?.title.orEmpty(),
            onNoteTitleChange = {},
            noteText = article.note?.text.orEmpty(),
            onNoteTextChange = {},
            noteTags = article.note?.tags.orEmpty(),
            tagDraft = "",
            onTagDraftChange = {},
            onAddTag = {},
            onRemoveTag = {},
            noteSyncState = NoteSyncState.SYNCED,
            titleError = false,
            bodyError = false,
            isSavingNote = false,
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onToggleSave = {},
            onOpenSource = {},
            onOpenResource = {},
            onSaveNote = {},
        )
    }
}

@Preview(name = "Article · Condensed", showBackground = true, widthDp = 360, heightDp = 1000)
@Composable
private fun ArticleDetailCondensedPreview() {
    val article = FeedSampleData.articles.first()
    PulseSyncTheme {
        ArticleDetailContent(
            article = article,
            summaryMode = SummaryMode.CONDENSED,
            visibleSummary = article.summaryFor(SummaryMode.CONDENSED),
            onSummaryModeChange = {},
            noteTitle = "",
            onNoteTitleChange = {},
            noteText = "",
            onNoteTextChange = {},
            noteTags = emptyList(),
            tagDraft = "",
            onTagDraftChange = {},
            onAddTag = {},
            onRemoveTag = {},
            noteSyncState = NoteSyncState.LOCAL,
            titleError = false,
            bodyError = false,
            isSavingNote = false,
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onToggleSave = {},
            onOpenSource = {},
            onOpenResource = {},
            onSaveNote = {},
        )
    }
}

@Preview(name = "Article · Empty note error", showBackground = true, widthDp = 360, heightDp = 1000)
@Composable
private fun ArticleDetailErrorPreview() {
    val article = FeedSampleData.articles[1]
    PulseSyncTheme {
        ArticleDetailContent(
            article = article,
            summaryMode = SummaryMode.DETAILED,
            visibleSummary = article.summaryFor(SummaryMode.DETAILED),
            onSummaryModeChange = {},
            noteTitle = "",
            onNoteTitleChange = {},
            noteText = "",
            onNoteTextChange = {},
            noteTags = emptyList(),
            tagDraft = "",
            onTagDraftChange = {},
            onAddTag = {},
            onRemoveTag = {},
            noteSyncState = NoteSyncState.LOCAL,
            titleError = true,
            bodyError = true,
            isSavingNote = false,
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onToggleSave = {},
            onOpenSource = {},
            onOpenResource = {},
            onSaveNote = {},
        )
    }
}