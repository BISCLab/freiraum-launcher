package com.bisc.flauncher.ui.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** One choice in a [ChoicePanel]. */
class ChoiceOption(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val summary: String? = null,
    val fontFamily: FontFamily? = null,
    val leading: (@Composable () -> Unit)? = null
)

private val PANEL_SHAPE = RoundedCornerShape(30.dp)
private val PIECE_GAP = 3.dp

/**
 * The settings' choice window: a title, then every option as a piece of its own with a fine gap,
 * the first and last rounded off like a group; the chosen one tinted with a check. A tap on an
 * option picks it, a tap beside the window closes it. [footer] holds a further action, if any.
 */
@Composable
fun ChoicePanel(title: String, options: List<ChoiceOption>, onDismiss: () -> Unit, footer: (@Composable () -> Unit)? = null) {
    Dialog(onDismissRequest = onDismiss) {
        val appear = remember { Animatable(0f) }
        LaunchedEffect(Unit) { appear.animateTo(1f, tween(180)) }
        Column(
            modifier = Modifier
                .graphicsLayer {
                    alpha = appear.value
                    val s = 0.94f + 0.06f * appear.value
                    scaleX = s; scaleY = s
                }
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clip(PANEL_SHAPE)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp, top = 6.dp, bottom = 16.dp))
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PIECE_GAP)
            ) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (option.selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable(onClick = option.onClick)
                            .heightIn(min = 60.dp)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        option.leading?.let {
                            Box(modifier = Modifier.padding(end = 14.dp).size(32.dp), contentAlignment = Alignment.Center) { it() }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                option.label,
                                style = MaterialTheme.typography.titleMedium.let { s -> option.fontFamily?.let { s.copy(fontFamily = it) } ?: s },
                                color = if (option.selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            option.summary?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (option.selected) {
                            Box(
                                modifier = Modifier.padding(start = 10.dp).size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp)) }
                        } else {
                            Spacer(Modifier.size(28.dp))
                        }
                    }
                }
            }
            if (footer != null) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) { footer() }
            }
        }
    }
}

/** A plain action under the choices, e.g. going back to the default. */
@Composable
fun ChoicePanelAction(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(label) }
}
