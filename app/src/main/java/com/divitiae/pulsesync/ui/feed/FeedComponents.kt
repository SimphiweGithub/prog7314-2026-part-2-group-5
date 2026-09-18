package com.divitiae.pulsesync.ui.feed

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.divitiae.pulsesync.R
import com.divitiae.pulsesync.ui.components.PulseSyncDimens
import com.divitiae.pulsesync.ui.theme.PulseSyncTextStyles

private val CardShape = RoundedCornerShape(14.dp)
private val ChipShape = RoundedCornerShape(8.dp)
private val TabShape = RoundedCornerShape(16.dp)

/** Navy app bar with title and 36 dp pill search field (Figma 1:19 / 29:75). */
@Composable
fun FeedTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = contentColor,
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = contentColor),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
        ) { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp),
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.feed_search_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.75f),
                        )
                    }
                    innerTextField()
                }
            }
        }
    }
}

/** Horizontally scrolling category chips (Figma 1:23 / 29:80). */
@Composable
fun CategoryStrip(
    categories: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            val isSelected = category == selected
            val background = if (isSelected) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            }
            val foreground = if (isSelected) {
                MaterialTheme.colorScheme.onSecondary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            Text(
                text = category,
                style = PulseSyncTextStyles.tab,
                color = foreground,
                modifier = Modifier
                    .background(background, TabShape)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(category) })
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/**
 * Article card (Figma 2:3 / 29:90). [summaryMode] drives the DualMode AI
 * Summary Toggle: detailed paragraphs vs. three bullets.
 */
@Composable
fun ArticleCard(
    article: ArticleUi,
    summaryMode: SummaryMode,
    onToggleSummary: () -> Unit,
    onOpenSource: () -> Unit,
    onAddNote: () -> Unit,
    onOpenResources: () -> Unit,
    onToggleSave: () -> Unit,
    onOpenArticle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val teal = MaterialTheme.colorScheme.secondary

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = onSurface,
        shadowElevation = 2.dp,
        onClick = onOpenArticle,
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
                Text(
                    text = stringResource(R.string.feed_meta, article.source, article.category),
                    style = MaterialTheme.typography.bodySmall,
                    color = teal,
                )
                Text(
                    text = article.timeAgo,
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurface.copy(alpha = 0.5f),
                )
            }

            Text(text = article.title, style = MaterialTheme.typography.titleSmall)

            AiSummarizedBadge()

            SummaryBody(article = article, mode = summaryMode)

            Row(
                modifier = Modifier
                    .clickable(onClick = onToggleSummary)
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.SwapVert,
                    contentDescription = null,
                    tint = teal,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(
                        if (summaryMode == SummaryMode.DETAILED) {
                            R.string.feed_toggle_to_condensed
                        } else {
                            R.string.feed_toggle_to_detailed
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = teal,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SentimentChip(sentiment = article.sentiment)
                article.keywords.forEach { keyword -> KeywordChip(keyword = keyword) }
            }

            HorizontalDivider(color = onSurface.copy(alpha = 0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardAction(
                    icon = Icons.Rounded.OpenInNew,
                    label = stringResource(R.string.feed_action_source),
                    onClick = onOpenSource,
                )
                CardAction(
                    icon = Icons.Rounded.EditNote,
                    label = stringResource(R.string.feed_action_note),
                    onClick = onAddNote,
                )
                CardAction(
                    icon = Icons.Rounded.Download,
                    label = stringResource(
                        R.string.feed_action_resources,
                        article.resourcesExtracted,
                        article.resourcesTotal,
                    ),
                    onClick = onOpenResources,
                )
                CardAction(
                    icon = if (article.isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    label = stringResource(
                        if (article.isSaved) R.string.feed_action_saved else R.string.feed_action_save,
                    ),
                    onClick = onToggleSave,
                    tint = if (article.isSaved) teal else onSurface,
                )
            }
        }
    }
}

@Composable
private fun SummaryBody(article: ArticleUi, mode: SummaryMode) {
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    Column(verticalArrangement = Arrangement.spacedBy(if (mode == SummaryMode.DETAILED) 20.dp else 0.dp)) {
        when (mode) {
            SummaryMode.DETAILED -> article.detailedSummary.forEach { paragraph ->
                Text(text = paragraph, style = MaterialTheme.typography.bodyMedium, color = textColor)
            }
            SummaryMode.CONDENSED -> article.condensedSummary.forEach { bullet ->
                Text(
                    text = stringResource(R.string.feed_bullet, bullet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
            }
        }
    }
}

/** "✦ AI Summarized" disclaimer pill (Figma 2:8 / 29:95). */
@Composable
private fun AiSummarizedBadge() {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .background(onSurface.copy(alpha = 0.10f), ChipShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = onSurface,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = stringResource(R.string.feed_ai_summarized),
            style = PulseSyncTextStyles.chip,
            color = onSurface,
        )
    }
}

@Composable
private fun SentimentChip(sentiment: Sentiment) {
    val scheme = MaterialTheme.colorScheme
    val (background, dot, label) = when (sentiment) {
        Sentiment.POSITIVE -> Triple(scheme.tertiary, scheme.onTertiary, R.string.feed_sentiment_positive)
        Sentiment.NEUTRAL -> Triple(scheme.onSurface.copy(alpha = 0.10f), scheme.onSurface, R.string.feed_sentiment_neutral)
        Sentiment.NEGATIVE -> Triple(scheme.error.copy(alpha = 0.18f), scheme.error, R.string.feed_sentiment_negative)
    }
    Row(
        modifier = Modifier
            .background(background, ChipShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dot, CircleShape),
        )
        Text(
            text = stringResource(label),
            style = PulseSyncTextStyles.chip,
            color = if (sentiment == Sentiment.POSITIVE) scheme.onTertiary else scheme.onSurface,
        )
    }
}

@Composable
private fun KeywordChip(keyword: String) {
    val teal = MaterialTheme.colorScheme.secondary
    Text(
        text = stringResource(R.string.feed_keyword, keyword),
        style = PulseSyncTextStyles.chip,
        color = teal,
        modifier = Modifier
            .background(teal.copy(alpha = 0.12f), ChipShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun CardAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = tint)
    }
}

/** Loading placeholder with the same silhouette as [ArticleCard]. */
@Composable
fun ArticleCardSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
    val bone = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f * alpha / 0.75f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Bone(width = 120.dp, height = 12.dp, color = bone)
                Bone(width = 40.dp, height = 12.dp, color = bone)
            }
            Bone(width = 280.dp, height = 16.dp, color = bone)
            Bone(width = 200.dp, height = 16.dp, color = bone)
            Bone(width = 90.dp, height = 18.dp, color = bone, radius = 8.dp)
            Bone(width = 300.dp, height = 12.dp, color = bone)
            Bone(width = 290.dp, height = 12.dp, color = bone)
            Bone(width = 240.dp, height = 12.dp, color = bone)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Bone(width = 60.dp, height = 18.dp, color = bone, radius = 8.dp)
                Bone(width = 50.dp, height = 18.dp, color = bone, radius = 8.dp)
                Bone(width = 50.dp, height = 18.dp, color = bone, radius = 8.dp)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                repeat(4) { Bone(width = 48.dp, height = 12.dp, color = bone) }
            }
        }
    }
}

@Composable
private fun Bone(width: Dp, height: Dp, color: Color, radius: Dp = 4.dp) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .background(color, RoundedCornerShape(radius)),
    )
}

/** Shown when a category has no articles. */
@Composable
fun FeedEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PulseSyncDimens.ScreenPadding, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Inbox,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = stringResource(R.string.feed_empty_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.feed_empty_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}
