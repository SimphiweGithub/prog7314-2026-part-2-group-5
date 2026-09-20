package com.divitiae.pulsesync.ui.settings

/*
 * ---------------------------------------------------------------------
 * CODE ATTRIBUTION
 * ---------------------------------------------------------------------
 * The animated switch, DropdownMenu, CompositionLocal theme lookup, BasicTextField decoration box and text-style handling in this file were adapted from:
 *
 * Android Developers (2026) Value-based animations. [online]
 * Available at: https://developer.android.com/develop/ui/compose/animation/value-based
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Switch. [online]
 * Available at: https://developer.android.com/develop/ui/compose/components/switch
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Menus. [online]
 * Available at: https://developer.android.com/develop/ui/compose/components/menu
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Locally scoped data with CompositionLocal. [online]
 * Available at: https://developer.android.com/develop/ui/compose/compositionlocal
 * [Accessed 20 September 2026].
 *
 * Android Developers (2026) Configure text fields. [online]
 * Available at: https://developer.android.com/develop/ui/compose/text/user-input
 * [Accessed 20 September 2026].
 * ---------------------------------------------------------------------
 */

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divitiae.pulsesync.R
import com.divitiae.pulsesync.ui.theme.LocalIsDarkTheme
import com.divitiae.pulsesync.ui.theme.PulseSyncTextStyles
import com.divitiae.pulsesync.ui.theme.PureWhite

private val CardShape = RoundedCornerShape(14.dp)
private val ChipShape = RoundedCornerShape(8.dp)
private val SegmentShape = RoundedCornerShape(10.dp)

/** Navy header bar with the screen title (Figma 9:42 / 29:302). */
@Composable
fun SettingsTopBar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleMedium.copy(lineHeight = 26.sp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** White/navy card with a teal icon and 14 sp Medium heading (Figma 9:45…). */
@Composable
fun SettingsCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(lineHeight = 22.sp),
                )
            }
            content()
        }
    }
}

/** Label on the left, any control on the right. */
@Composable
fun SettingsRow(
    label: String,
    modifier: Modifier = Modifier,
    control: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        control()
    }
}

/** 36 × 20 dp teal switch matching the Figma toggle asset. */
@Composable
fun PulseSyncSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Adapted from: Android Developers (2026) Value-based animations - animate*AsState. https://developer.android.com/develop/ui/compose/animation/value-based
    val track by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
        label = "switchTrack",
    )
    val knobOffset by animateDpAsState(targetValue = if (checked) 18.dp else 2.dp, label = "switchKnob")
    Box(
        modifier = modifier
            .width(36.dp)
            .height(20.dp)
            .background(track, CircleShape)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .align(Alignment.CenterStart)
                .size(16.dp)
                .background(PureWhite, CircleShape),
        )
    }
}

/**
 * Pill-style segmented control (Figma 9:49 / 23:89). The selected segment
 * is white in light mode and teal-tinted in dark mode.
 */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val selectedBackground = if (LocalIsDarkTheme.current) {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
    } else {
        PureWhite
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(onSurface.copy(alpha = 0.08f), SegmentShape)
            .padding(3.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (isSelected) selectedBackground else Color.Transparent, ChipShape)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option) })
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) onSurface else onSurface.copy(alpha = 0.55f),
                    maxLines = 1,
                )
            }
        }
    }
}

/** "English ⌄" chip that opens a dropdown (Figma 10:86 / 23:80). */
@Composable
fun DropdownChip(
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val onSurface = MaterialTheme.colorScheme.onSurface
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .background(onSurface.copy(alpha = 0.08f), ChipShape)
                .clickable(role = Role.DropdownList) { expanded = true }
                .padding(start = 8.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = value, style = MaterialTheme.typography.bodySmall, color = onSurface)
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = onSurface,
                modifier = Modifier.size(14.dp),
            )
        }
        // Adapted from: Android Developers (2026) Menus - DropdownMenu. https://developer.android.com/develop/ui/compose/components/menu
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

/** Mint keyword chip with a remove affordance (Figma 10:59). */
@Composable
fun KeywordChip(keyword: String, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val onTertiary = MaterialTheme.colorScheme.onTertiary
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.tertiary, ChipShape)
            .padding(start = 8.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = keyword, style = MaterialTheme.typography.bodySmall, color = onTertiary)
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(R.string.settings_remove_keyword, keyword),
            tint = onTertiary,
            modifier = Modifier
                .size(14.dp)
                .clickable(onClick = onRemove),
        )
    }
}

/** 36 dp "+ Add a keyword to track" input (Figma 10:55). Submits on IME done. */
@Composable
fun AddKeywordField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val errorColor = MaterialTheme.colorScheme.error
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
    ) { innerTextField ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(
                    if (isError) errorColor.copy(alpha = 0.10f) else onSurface.copy(alpha = 0.06f),
                    ChipShape,
                )
                .then(if (isError) Modifier.border(1.dp, errorColor, ChipShape) else Modifier)
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = if (isError) errorColor else onSurface,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onSubmit),
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_add_keyword_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurface.copy(alpha = 0.6f),
                    )
                }
                innerTextField()
            }
        }
    }
}

/** 6 dp teal progress track (Figma 10:75). */
@Composable
fun SlotProgressBar(used: Int, total: Int, modifier: Modifier = Modifier) {
    val fraction = if (total <= 0) 0f else (used.toFloat() / total).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), RoundedCornerShape(3.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(3.dp)),
        )
    }
}

/** "A  Medium  A" stepper (Figma 23:72). */
@Composable
fun FontSizeStepper(
    value: FontSizePreference,
    onChange: (FontSizePreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val buttonBackground = if (LocalIsDarkTheme.current) {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
    } else {
        PureWhite
    }
    val entries = FontSizePreference.entries
    val index = entries.indexOf(value)

    Row(
        modifier = modifier
            .background(onSurface.copy(alpha = 0.06f), SegmentShape)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(
            label = "A",
            fontSize = 11.sp,
            background = buttonBackground,
            enabled = index > 0,
            contentDescription = stringResource(R.string.settings_font_smaller),
            onClick = { onChange(entries[index - 1]) },
        )
        Text(
            text = stringResource(value.labelRes()),
            style = MaterialTheme.typography.bodySmall,
            color = onSurface,
        )
        StepButton(
            label = "A",
            fontSize = 17.sp,
            background = buttonBackground,
            enabled = index < entries.lastIndex,
            contentDescription = stringResource(R.string.settings_font_larger),
            onClick = { onChange(entries[index + 1]) },
        )
    }
}

@Composable
private fun StepButton(
    label: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    background: Color,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(26.dp)
            .background(background, ChipShape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = PulseSyncTextStyles.tab.copy(fontSize = fontSize),
            color = if (enabled) onSurface else onSurface.copy(alpha = 0.35f),
        )
    }
}

fun FontSizePreference.labelRes(): Int = when (this) {
    FontSizePreference.SMALL -> R.string.settings_font_small
    FontSizePreference.MEDIUM -> R.string.settings_font_medium
    FontSizePreference.LARGE -> R.string.settings_font_large
}

fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
    ThemeMode.SYSTEM -> R.string.settings_theme_system
}
