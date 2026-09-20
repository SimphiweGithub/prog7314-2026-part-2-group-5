package com.divitiae.pulsesync.ui.article

/*
 * ---------------------------------------------------------------------
 * CODE ATTRIBUTION
 * ---------------------------------------------------------------------
 * The BasicTextField decoration-box pattern, error semantics, FlowRow tag chips and layout composition in this file were adapted from:
 *
 * Android Developers (2026) Configure text fields. [online]
 * Available at: https://developer.android.com/develop/ui/compose/text/user-input
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Flow layouts in Compose. [online]
 * Available at: https://developer.android.com/develop/ui/compose/layouts/flow
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Semantics in Compose. [online]
 * Available at: https://developer.android.com/develop/ui/compose/accessibility/semantics
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Compose layout basics. [online]
 * Available at: https://developer.android.com/develop/ui/compose/layouts/basics
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Compose modifiers. [online]
 * Available at: https://developer.android.com/develop/ui/compose/modifiers
 * [Accessed 20 September 2026].
 * ---------------------------------------------------------------------
 */

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divitiae.pulsesync.R
import com.divitiae.pulsesync.ui.feed.NoteSyncState
import com.divitiae.pulsesync.ui.feed.ResourceLinkUi
import com.divitiae.pulsesync.ui.feed.ResourceType
import com.divitiae.pulsesync.ui.theme.PulseSyncTextStyles

private val PanelShape = RoundedCornerShape(12.dp)
private val CardShape = RoundedCornerShape(14.dp)
private val ChipShape = RoundedCornerShape(8.dp)
private val NoticeShape = RoundedCornerShape(10.dp)

/** 48 dp navy bar: back, centred title, bookmark toggle (Figma 6:42 / 29:175). */
@Composable
fun ArticleTopBar(
    isSaved: Boolean,
    onBack: () -> Unit,
    onToggleSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .statusBarsPadding()
            .height(48.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.article_back),
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = stringResource(R.string.article_title),
            style = MaterialTheme.typography.titleSmall,
            color = contentColor,
            modifier = Modifier.align(Alignment.Center),
        )
        IconButton(onClick = onToggleSave, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)) {
            Icon(
                imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                contentDescription = stringResource(
                    if (isSaved) R.string.article_unsave else R.string.article_save,
                ),
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** "News24 — View original source ↗" teal link row (Figma 6:48). */
@Composable
fun SourceLinkRow(source: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val teal = MaterialTheme.colorScheme.secondary
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.article_view_source, source),
            style = MaterialTheme.typography.bodySmall,
            color = teal,
        )
        Icon(
            imageVector = Icons.Rounded.OpenInNew,
            contentDescription = null,
            tint = teal,
            modifier = Modifier.size(12.dp),
        )
    }
}

/** Mint 35 % info banner (Figma 6:51 / 29:184). */
@Composable
fun AiDisclosureNotice(modifier: Modifier = Modifier) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f), NoticeShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = onSurface,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.article_ai_disclosure),
            style = MaterialTheme.typography.bodySmall,
            color = onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

/** "Downloaded for offline · 3/5 slots used" pill (Figma 6:54). */
@Composable
fun OfflineBadge(slotsUsed: Int, slotsTotal: Int, modifier: Modifier = Modifier) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .background(onSurface.copy(alpha = 0.08f), ChipShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.DownloadDone,
            contentDescription = null,
            tint = onSurface,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(R.string.article_offline_badge, slotsUsed, slotsTotal),
            style = MaterialTheme.typography.bodySmall,
            color = onSurface,
        )
    }
}

/**
 * User Defined Feature 2: Extracted Resource Links Panel (Figma 7:35 / 29:191).
 * Mint panel with one tappable row per link; each row hands off to an
 * explicit ACTION_VIEW intent via [onOpen].
 */
@Composable
fun ResourceLinksPanel(
    resources: List<ResourceLinkUi>,
    onOpen: (ResourceLinkUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiary, PanelShape)
            .border(1.dp, Color(0x26174B7A), PanelShape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Link,
                contentDescription = null,
                tint = onSurface,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.article_resources_title),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = PulseSyncTextStyles.panelTitleSize),
                color = onSurface,
            )
        }
        if (resources.isEmpty()) {
            Text(
                text = stringResource(R.string.article_resources_empty),
                style = MaterialTheme.typography.bodySmall,
                color = onSurface.copy(alpha = 0.7f),
            )
        }
        resources.forEach { resource ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.6f), ChipShape)
                    .clickable(onClick = { onOpen(resource) })
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = resource.type.icon(),
                        contentDescription = null,
                        tint = onSurface,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = resource.title,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                        color = onSurface,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = onSurface,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun ResourceType.icon(): ImageVector = when (this) {
    ResourceType.PDF -> Icons.Rounded.Description
    ResourceType.PORTAL -> Icons.Rounded.HowToReg
    ResourceType.WEB -> Icons.Rounded.Language
    ResourceType.VIDEO -> Icons.Rounded.PlayCircle
}

/**
 * User Defined Feature 3: Embedded Contextual Notes Editor
 * (Figma 7:49 / 29:205). Title field, body field, removable tag chips with
 * an "+ Add tag" input, and the Save button. [isTitleError] / [isBodyError]
 * paint the red validation highlight on the offending field when a save is
 * attempted with empty content. Tag validation feedback (Toast) is owned by
 * the stateful caller so this composable stays stateless.
 */
@Composable
fun NotesEditorCard(
    title: String,
    onTitleChange: (String) -> Unit,
    text: String,
    onTextChange: (String) -> Unit,
    tags: List<String>,
    tagDraft: String,
    onTagDraftChange: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    syncState: NoteSyncState,
    isTitleError: Boolean,
    isBodyError: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val errorColor = MaterialTheme.colorScheme.error

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = onSurface,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EditNote,
                        contentDescription = null,
                        tint = onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(text = stringResource(R.string.article_notes_title), style = MaterialTheme.typography.titleSmall)
                }
                SyncChip(syncState = syncState)
            }

            NoteField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = stringResource(R.string.article_notes_title_placeholder),
                isError = isTitleError,
                singleLine = true,
            )

            NoteField(
                value = text,
                onValueChange = onTextChange,
                placeholder = stringResource(R.string.article_notes_placeholder),
                isError = isBodyError,
                singleLine = false,
                minHeight = 64.dp,
            )
            if (isTitleError || isBodyError) {
                Text(
                    text = stringResource(R.string.article_notes_empty_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = errorColor,
                )
            }

            // Adapted from: Android Developers (2026) Flow layouts in Compose. https://developer.android.com/develop/ui/compose/layouts/flow
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (tags.isEmpty()) {
                    Text(
                        text = stringResource(R.string.article_notes_no_tag),
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                tags.forEach { tag ->
                    NoteTagChip(tag = tag, onRemove = { onRemoveTag(tag) })
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AddTagField(
                    value = tagDraft,
                    onValueChange = onTagDraftChange,
                    onSubmit = onAddTag,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    onClick = onSave,
                    shape = ChipShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Save,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(text = stringResource(R.string.article_notes_save), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/**
 * Shared note input styling used by both the title and body fields.
 * Adapted from: Android Developers (2026) Configure text fields.
 * https://developer.android.com/develop/ui/compose/text/user-input
 */
@Composable
private fun NoteField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
    minHeight: Dp = 36.dp,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val errorColor = MaterialTheme.colorScheme.error
    val emptyError = stringResource(R.string.article_notes_empty_error)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodySmall.copy(
            color = onSurface.copy(alpha = 0.8f),
            lineHeight = 20.sp,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (isError) error(emptyError) },
    ) { innerTextField ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeight)
                .background(
                    if (isError) errorColor.copy(alpha = 0.10f) else onSurface.copy(alpha = 0.06f),
                    ChipShape,
                )
                .then(if (isError) Modifier.border(1.dp, errorColor, ChipShape) else Modifier)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                    color = onSurface.copy(alpha = 0.4f),
                )
            }
            innerTextField()
        }
    }
}

/** Removable tag chip: label icon, tag text and a close icon. */
@Composable
private fun NoteTagChip(tag: String, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .background(onSurface.copy(alpha = 0.08f), ChipShape)
            .padding(start = 8.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Label,
            contentDescription = null,
            tint = onSurface,
            modifier = Modifier.size(12.dp),
        )
        Text(text = tag, style = MaterialTheme.typography.bodySmall, color = onSurface)
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(R.string.article_notes_remove_tag, tag),
            tint = onSurface,
            modifier = Modifier
                .size(14.dp)
                .clickable(onClick = onRemove),
        )
    }
}

/** 36 dp "+ Add tag" input. Submits on IME done or by tapping the plus icon. */
@Composable
private fun AddTagField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        modifier = modifier.height(36.dp),
    ) { innerTextField ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(onSurface.copy(alpha = 0.06f), ChipShape)
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.article_notes_add_tag),
                tint = onSurface,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onSubmit),
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.article_notes_add_tag_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurface.copy(alpha = 0.4f),
                    )
                }
                innerTextField()
            }
        }
    }
}

@Composable
private fun SyncChip(syncState: NoteSyncState) {
    val (background, foreground, icon, label) = when (syncState) {
        NoteSyncState.SYNCED -> SyncChipStyle(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary,
            Icons.Rounded.CloudDone,
            R.string.article_notes_synced,
        )
        NoteSyncState.LOCAL -> SyncChipStyle(
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.onSurface,
            Icons.Rounded.CloudOff,
            R.string.article_notes_local,
        )
    }
    Row(
        modifier = Modifier
            .background(background, ChipShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = foreground, modifier = Modifier.size(12.dp))
        Text(text = stringResource(label), style = PulseSyncTextStyles.chip, color = foreground)
    }
}

private data class SyncChipStyle(
    val background: Color,
    val foreground: Color,
    val icon: ImageVector,
    val label: Int,
)
