package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.ui.theme.DEFAULT_ACCENT_COLOR
import com.bisc.flauncher.ui.theme.noRippleClickable

/** The preset accent colors, FLauncher's standard blue first. */
private val ACCENT_PRESETS = listOf(
    DEFAULT_ACCENT_COLOR,
    0xFF7C3AED.toInt(), // violet
    0xFFDB2777.toInt(), // pink
    0xFFDC2626.toInt(), // red
    0xFFEA580C.toInt(), // orange
    0xFFCA8A04.toInt(), // amber
    0xFF16A34A.toInt(), // green
    0xFF0D9488.toInt(), // teal
    0xFF475569.toInt()  // slate
)

private const val PALETTE_COLUMNS = 5

/** Accent colors as tappable swatches instead of a list row: the presets, then a "+" swatch that
 *  opens the full picker (shown filled with the current color once that color isn't a preset).
 *  Rendered as one card row of a [SettingsPreferenceGroup]. */
@Composable
fun AccentPalette(selected: Int, onSelect: (Int) -> Unit, onCustom: () -> Unit) {
    val isCustom = selected !in ACCENT_PRESETS
    val cells: List<Int?> = ACCENT_PRESETS + listOf(null)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        cells.chunked(PALETTE_COLUMNS).forEach { rowCells ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowCells.forEach { color ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (color != null) {
                            Swatch(color = Color(color), selected = color == selected, onClick = { onSelect(color) })
                        } else {
                            CustomSwatch(current = if (isCustom) Color(selected) else null, onClick = onCustom)
                        }
                    }
                }
                // Keep the last row's swatches the same width as the full rows above.
                repeat(PALETTE_COLUMNS - rowCells.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
            .noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = if (color.luminance() > 0.5f) Color.Black else Color.White)
        }
    }
}

@Composable
private fun CustomSwatch(current: Color?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(current ?: MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(if (current != null) 3.dp else 1.dp, if (current != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline, CircleShape)
            .noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (current != null) Icons.Filled.Check else Icons.Filled.Add,
            contentDescription = stringResource(R.string.settings_accent_custom),
            tint = when {
                current == null -> MaterialTheme.colorScheme.onSurfaceVariant
                current.luminance() > 0.5f -> Color.Black
                else -> Color.White
            }
        )
    }
}

/** The corner-roundness presets offered as tiles. */
private val CORNER_PRESETS = listOf(0, 25, 50, 75, 100)

/** Corner roundness as a row of tiles that each show their own rounding, instead of a percent
 *  slider. A value between presets selects no tile. Rendered as one card row of a group. */
@Composable
fun CornerShapeTiles(percent: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CORNER_PRESETS.forEach { preset ->
            val selected = preset == percent
            // 100% = a fully round corner; RoundedCornerShape's percent tops out at 50 (half the side).
            val shape = RoundedCornerShape(percent = preset / 2)
            Column(
                modifier = Modifier.weight(1f).noRippleClickable { onSelect(preset) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(shape)
                        .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest)
                        .border(
                            if (selected) 2.dp else 1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape
                        )
                )
                Text(
                    stringResource(R.string.settings_corner_radius_value, preset),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Left/right alignment as two miniature app lists - a few app rows plus the A–Z rail on the
 *  chosen side - instead of a Left | Right switch. Rendered as one card row of a group. */
@Composable
fun HandednessPreviews(selected: com.bisc.flauncher.data.settings.Handedness, onSelect: (com.bisc.flauncher.data.settings.Handedness) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        listOf(
            com.bisc.flauncher.data.settings.Handedness.LEFT to R.string.settings_app_overview_align_left,
            com.bisc.flauncher.data.settings.Handedness.RIGHT to R.string.settings_app_overview_align_right
        ).forEach { (side, labelRes) ->
            val isSelected = side == selected
            val shape = RoundedCornerShape(16.dp)
            Column(
                modifier = Modifier.weight(1f).noRippleClickable { onSelect(side) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.8f)
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .border(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape
                        )
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (side == com.bisc.flauncher.data.settings.Handedness.LEFT) MiniRail(isSelected)
                    MiniAppRows(modifier = Modifier.weight(1f))
                    if (side == com.bisc.flauncher.data.settings.Handedness.RIGHT) MiniRail(isSelected)
                }
                Text(
                    stringResource(labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** A few app rows: an icon dot and a name bar each. */
@Composable
private fun MiniAppRows(modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0.8f, 0.55f, 0.7f, 0.45f, 0.65f).forEach { length ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(ink.copy(alpha = 0.45f)))
                Box(modifier = Modifier.fillMaxWidth(length).height(6.dp).clip(CircleShape).background(ink.copy(alpha = 0.3f)))
            }
        }
    }
}

/** The A–Z rail: a slim full-height bar, in the accent color while its side is selected. */
@Composable
private fun MiniRail(highlighted: Boolean) {
    Box(
        modifier = Modifier
            .width(8.dp)
            .fillMaxHeight()
            .clip(CircleShape)
            .background(if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    )
}
