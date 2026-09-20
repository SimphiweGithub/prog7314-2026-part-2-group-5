package com.divitiae.pulsesync.ui.article

/*
 * ---------------------------------------------------------------------
 * CODE ATTRIBUTION
 * ---------------------------------------------------------------------
 * The Scaffold + SnackbarHost structure, Toast feedback, state hoisting, window-inset padding and preview annotations in this file were adapted from:
 *
 * Android Developers (2026) Scaffold. [online]
 * Available at: https://developer.android.com/develop/ui/compose/components/scaffold
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Snackbar. [online]
 * Available at: https://developer.android.com/develop/ui/compose/components/snackbar
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Toasts overview. [online]
 * Available at: https://developer.android.com/guide/topics/ui/notifiers/toasts
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Where to hoist state. [online]
 * Available at: https://developer.android.com/develop/ui/compose/state-hoisting
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) About window insets. [online]
 * Available at: https://developer.android.com/develop/ui/compose/system/insets
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.divitiae.pulsesync.R
import com.divitiae.pulsesync.ui.components.PulseSyncDimens
import com.divitiae.pulsesync.ui.feed.ArticleUi
import com.divitiae.pulsesync.ui.feed.FeedSampleData
import com.divitiae.pulsesync.ui.feed.NoteSyncState
import com.divitiae.pulsesync.ui.feed.NoteUi
import com.divitiae.pulsesync.ui.feed.ResourceLinkUi
import com.divitiae.pulsesync.ui.theme.PulseSyncTheme
import com.divitiae.pulsesync.ui.util.ExternalLinks
import kotlinx.coroutines.launch

/**
 * Article Detail & Interactive Workspace (Figma 6:35, dark 29:168).
 *
 * Hosts User Defined Features 2 and 3: the Extracted Resource Links Panel
 * (explicit intents) and the Embedded Contextual Notes Editor (title, body,
 * tag chips; empty title/body validation → red highlight + Snackbar; blank or
 * duplicate tag → Toast). Note persistence is delegated to [onSaveNote] for
 * Member 4's ViewModel.
 */
@Composable
fun ArticleDetailScreen(
    article: ArticleUi,
    onBack: () -> Unit,
    onToggleSave: (ArticleUi) -> Unit,
    onSaveNote: (ArticleUi, NoteUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Adapted from: Android Developers (2026) Snackbar. https://developer.android.com/develop/ui/compose/components/snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var isSaved by rememberSaveable(article.id) { mutableStateOf(article.isSaved) }
    var noteTitle by rememberSaveable(article.id) { mutableStateOf(article.note?.title.orEmpty()) }
    var noteText by rememberSaveable(article.id) { mutableStateOf(article.note?.text.orEmpty()) }
    var noteTags by rememberSaveable(article.id) { mutableStateOf(article.note?.tags.orEmpty()) }
    var tagDraft by rememberSaveable(article.id) { mutableStateOf("") }
    var syncState by rememberSaveable(article.id) { mutableStateOf(article.note?.syncState ?: NoteSyncState.LOCAL) }
    var titleError by rememberSaveable(article.id) { mutableStateOf(false) }
    var bodyError by rememberSaveable(article.id) { mutableStateOf(false) }

    val emptyNoteMessage = stringResource(R.string.article_notes_empty_error)
    val noteSavedMessage = stringResource(R.string.article_notes_saved)
    val noAppMessage = stringResource(R.string.article_no_app_for_link)

    ArticleDetailContent(
        article = article.copy(isSaved = isSaved),
        noteTitle = noteTitle,
        onNoteTitleChange = {
            noteTitle = it
            titleError = false // Clear the highlight as soon as the user types.
        },
        noteText = noteText,
        onNoteTextChange = {
            noteText = it
            bodyError = false
        },
        noteTags = noteTags,
        tagDraft = tagDraft,
        onTagDraftChange = { tagDraft = it },
        onAddTag = {
            val tag = tagDraft.trim()
            when {
                tag.isEmpty() -> {
                    // Adapted from: Android Developers (2026) Toasts overview. https://developer.android.com/guide/topics/ui/notifiers/toasts
                    Toast.makeText(context, R.string.article_notes_tag_blank_error, Toast.LENGTH_SHORT).show()
                }
                noteTags.any { it.equals(tag, ignoreCase = true) } -> {
                    Toast.makeText(context, R.string.article_notes_tag_duplicate_error, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    noteTags = noteTags + tag
                    tagDraft = ""
                }
            }
        },
        onRemoveTag = { tag -> noteTags = noteTags - tag },
        noteSyncState = syncState,
        titleError = titleError,
        bodyError = bodyError,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onToggleSave = {
            isSaved = !isSaved
            onToggleSave(article.copy(isSaved = isSaved))
        },
        onOpenSource = {
            if (!ExternalLinks.open(context, article.sourceUrl)) {
                scope.launch { snackbarHostState.showSnackbar(noAppMessage) }
            }
        },
        onOpenResource = { resource ->
            if (!ExternalLinks.open(context, resource.url, resource.type)) {
                scope.launch { snackbarHostState.showSnackbar(noAppMessage) }
            }
        },
        onSaveNote = {
            titleError = noteTitle.isBlank()
            bodyError = noteText.isBlank()
            if (titleError || bodyError) {
                scope.launch { snackbarHostState.showSnackbar(emptyNoteMessage) }
            } else {
                val note = NoteUi(
                    title = noteTitle.trim(),
                    text = noteText.trim(),
                    tags = noteTags,
                    syncState = NoteSyncState.LOCAL,
                )
                syncState = NoteSyncState.LOCAL
                onSaveNote(article, note)
                scope.launch { snackbarHostState.showSnackbar(noteSavedMessage) }
            }
        },
        modifier = modifier,
    )
}

@Composable
fun ArticleDetailContent(
    article: ArticleUi,
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

            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                article.body.forEach { paragraph ->
                    Text(
                        text = paragraph,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            ResourceLinksPanel(resources = article.resources, onOpen = onOpenResource)

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
                    focusManager.clearFocus()
                    onSaveNote()
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
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onToggleSave = {},
            onOpenSource = {},
            onOpenResource = {},
            onSaveNote = {},
        )
    }
}
