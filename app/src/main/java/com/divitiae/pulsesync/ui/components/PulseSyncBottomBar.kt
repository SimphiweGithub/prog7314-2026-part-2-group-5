package com.divitiae.pulsesync.ui.components

/*
 * ---------------------------------------------------------------------
 * CODE ATTRIBUTION
 * ---------------------------------------------------------------------
 * The selectable tab row, Material 3 colour-role usage and enum-backed destinations in this file were adapted from:
 *
 * Android Developers (2026) Material Design 3 in Compose. [online]
 * Available at: https://developer.android.com/develop/ui/compose/designsystems/material3
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Compose layout basics. [online]
 * Available at: https://developer.android.com/develop/ui/compose/layouts/basics
 * [Accessed 20 September 2026].
 *
 * JetBrains (n.d.) Enum classes. [online]
 * Available at: https://kotlinlang.org/docs/enum-classes.html
 * [Accessed 20 September 2026].
 * ---------------------------------------------------------------------
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DynamicFeed
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.divitiae.pulsesync.R

/** Top-level destinations shown in the bottom bar (Figma 2:73 / 29:125). */
enum class BottomDestination(val labelRes: Int, val icon: ImageVector) {
    FEED(R.string.nav_feed, Icons.Rounded.DynamicFeed),
    VAULT(R.string.nav_vault, Icons.Rounded.Lock),
    SETTINGS(R.string.nav_settings, Icons.Rounded.Settings),
}

/**
 * 64 dp bottom navigation with a hairline top border. Selected item is teal,
 * the rest sit at 50 % of the on-surface colour.
 */
@Composable
fun PulseSyncBottomBar(
    selected: BottomDestination,
    onSelect: (BottomDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomDestination.entries.forEach { destination ->
                val isSelected = destination == selected
                val tint = if (isSelected) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
                Column(
                    modifier = Modifier
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { onSelect(destination) },
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(destination.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                    )
                }
            }
        }
    }
}
