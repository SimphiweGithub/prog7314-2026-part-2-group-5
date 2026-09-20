package com.divitiae.pulsesync.ui.article

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.divitiae.pulsesync.R
import com.divitiae.pulsesync.ui.feed.SummaryMode

/**
 * User Defined Feature 1 — DualMode AI Summary Toggle for the detail screen.
 * A two-segment control; selecting a segment swaps which `aiSummary` array is
 * rendered. State lives in [ArticleDetailViewModel], not here.
 */
@Composable
fun SummaryModeToggle(
    mode: SummaryMode,
    onModeChange: (SummaryMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.article_summary_mode_label)
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = label },
    ) {
        SummaryMode.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                selected = entry == mode,
                onClick = { onModeChange(entry) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = SummaryMode.entries.size),
                icon = {
                    SegmentedButtonDefaults.Icon(active = entry == mode) {
                        Icon(
                            imageVector = when (entry) {
                                SummaryMode.DETAILED -> Icons.AutoMirrored.Rounded.Notes
                                SummaryMode.CONDENSED -> Icons.Rounded.FormatListBulleted
                            },
                            contentDescription = null,
                        )
                    }
                },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Text(
                    text = stringResource(
                        when (entry) {
                            SummaryMode.DETAILED -> R.string.article_summary_detailed
                            SummaryMode.CONDENSED -> R.string.article_summary_condensed
                        },
                    ),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}