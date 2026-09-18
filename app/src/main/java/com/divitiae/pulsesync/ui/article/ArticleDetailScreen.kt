package com.divitiae.pulsesync.ui.article

import android.content.res.Configuration
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
 * (explicit intents) and the Embedded Contextual Notes Editor (empty-note
 * validation → red highlight + Snackbar). Note persistence is delegated to
 * [onSaveNote] for Member 4's ViewModel.
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var isSaved by rememberSaveable(article.id) { mutableStateOf(article.isSaved) }
    var noteText by rememberSaveable(article.id) { mutableStateOf(article.note?.text.orEmpty()) }
    var syncState by rememberSaveable(article.id) { mutableStateOf(article.note?.syncState ?: NoteSyncState.LOCAL) }
    var noteError by rememberSaveable(article.id) { mutableStateOf(false) }

    val emptyNoteMessage = stringResource(R.string.article_notes_empty_error)
    val noteSavedMessage = stringResource(R.string.article_notes_saved)
    val noAppMessage = stringResource(R.string.article_no_app_for_link)

    ArticleDetailContent(
        article = article.copy(isSaved = isSaved),
        noteText = noteText,
        onNoteTextChange = {
            noteText = it
            if (it.isNotBlank()) noteError = false
        },
        noteSyncState = syncState,
        noteError = noteError,
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
            if (noteText.isBlank()) {
                noteError = true
                scope.launch { snackbarHostState.showSnackbar(emptyNoteMessage) }
            } else {
                noteError = false
                val note = NoteUi(text = noteText.trim(), tag = article.note?.tag, syncState = NoteSyncState.LOCAL)
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
    noteText: String,
    onNoteTextChange: (String) -> Unit,
    noteSyncState: NoteSyncState,
    noteError: Boolean,
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
                text = noteText,
                onTextChange = onNoteTextChange,
                tag = article.note?.tag,
                syncState = noteSyncState,
                isError = noteError,
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
            noteText = article.note?.text.orEmpty(),
            onNoteTextChange = {},
            noteSyncState = NoteSyncState.SYNCED,
            noteError = false,
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
            noteText = "",
            onNoteTextChange = {},
            noteSyncState = NoteSyncState.LOCAL,
            noteError = true,
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onToggleSave = {},
            onOpenSource = {},
            onOpenResource = {},
            onSaveNote = {},
        )
    }
}
