package com.bisc.flauncher.ui.home.search

import com.bisc.flauncher.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.KeyboardLayout
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.data.settings.IconShape
import com.bisc.flauncher.ui.home.dock.resolveElementColors

private const val ROW_1 = "qwertyuiop"
private const val ROW_2 = "asdfghjkl"
private const val ROW_3 = "zxcvbnm"

private const val ROW_1_QWERTZ = "qwertzuiop"
private const val ROW_3_QWERTZ = "yxcvbnm"

private const val NUM_ROW_0 = "1234567890"
private const val NUM_ROW_1 = "+-×÷=%"
private const val NUM_ROW_2 = "().,!?"

private val DIACRITICS: Map<Char, List<Char>> = mapOf(
    'a' to listOf('ä', 'á', 'à', 'â'),
    'e' to listOf('é', 'è', 'ê'),
    'i' to listOf('í', 'î'),
    'o' to listOf('ö', 'ó', 'ò', 'ô'),
    'u' to listOf('ü', 'ú', 'ù', 'û'),
    's' to listOf('ß', 'š'),
    'c' to listOf('ç'),
    'n' to listOf('ñ'),
    'y' to listOf('ý'),
)

private val KEY_HEIGHT = 46.dp
private val ROW_GAP = 6.dp
private val KEY_GAP = 4.dp
private val CONTENT_INSET = 14.dp

/** The keyboard itself keeps one flat, uniform, fully-opaque background. */

/** Basic in-app QWERTY layout, replacing the system IME for search input. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomKeyboard(
    query: String,
    placeholder: String,
    onKeyPress: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onHome: () -> Unit,
    showNumberRow: Boolean,
    layout: KeyboardLayout = KeyboardLayout.QWERTY,
    handedness: Handedness,
    iconShape: IconShape,
    appearance: ElementAppearance = ElementAppearance(),
    autoOpen: Boolean = true,
    hapticsEnabled: Boolean = true,
    sparksEnabled: Boolean = true,
    sparkColor: Color = Color(0xFF34D058),
    modifier: Modifier = Modifier
) {
    val haptic = rememberKeyHaptic(hapticsEnabled)
    val sparks = rememberKeySparks()
    var lastDown by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    val tick: () -> Unit = {
        haptic()
        if (sparksEnabled) lastDown?.let(sparks::spawn)
    }
    val onKeyPress: (Char) -> Unit = { char -> tick(); onKeyPress(char) }
    val onBackspace: () -> Unit = { tick(); onBackspace() }
    val onSpace: () -> Unit = { tick(); onSpace() }
    val onHome: () -> Unit = { tick(); onHome() }
    var showingNumbersPage by remember { mutableStateOf(false) }
    // Seeded from autoOpen once per search session, not re-synced if the setting changes later — see
    // SearchDimens/searchKeyboardAutoOpen: when false, the keyboard starts collapsed to just the input
    // pill and expands on tap, instead of showing the full key grid immediately.
    var expanded by remember { mutableStateOf(autoOpen) }
    val colors = resolveElementColors(appearance)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = SearchDimens.horizontalMargin, end = SearchDimens.horizontalMargin, bottom = SearchDimens.bottomMargin),
        contentAlignment = Alignment.Center
    ) {
        val d = searchDimens(maxWidth)
        val keyShape = RoundedCornerShape(d.cornerRadius)

        CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
        val cardShape = RoundedCornerShape(d.cornerRadius)
        Box(
            modifier = Modifier
                .width(d.pillWidth)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.firstOrNull { it.changedToDown() }?.let { lastDown = it.position }
                        }
                    }
                }
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(colors.background, alpha = colors.backgroundAlpha)
                .then(if (appearance.borderEnabled) Modifier.border(colors.borderWidth, colors.borderColor, cardShape) else Modifier),
            verticalArrangement = Arrangement.spacedBy(ROW_GAP)
        ) {
            SearchInputDisplay(
                query = query,
                placeholder = placeholder,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = CONTENT_INSET, end = CONTENT_INSET, top = CONTENT_INSET)
                    .then(if (!expanded) Modifier.clickable { expanded = true } else Modifier)
            )

            if (expanded) {
            Column(
                modifier = Modifier.padding(start = CONTENT_INSET, end = CONTENT_INSET),
                verticalArrangement = Arrangement.spacedBy(ROW_GAP)
            ) {
                if (showingNumbersPage) {
                    KeyRow(NUM_ROW_0, onKeyPress, keyShape)
                    KeyRow(NUM_ROW_1, onKeyPress, keyShape)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
                        NUM_ROW_2.forEach { char -> Key(char.toString(), keyShape, modifier = Modifier.weight(1f), onClick = { onKeyPress(char) }) }
                        IconKey(keyShape, modifier = Modifier.weight(1.5f), onClick = onBackspace) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = stringResource(R.string.search_action_backspace))
                        }
                    }
                } else {
                    if (showNumberRow) {
                        KeyRow(NUM_ROW_0, onKeyPress, keyShape)
                    }
                    KeyRow(if (layout == KeyboardLayout.QWERTZ) ROW_1_QWERTZ else ROW_1, onKeyPress, keyShape)
                    KeyRow(ROW_2, onKeyPress, keyShape)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
                        (if (layout == KeyboardLayout.QWERTZ) ROW_3_QWERTZ else ROW_3).forEach { char ->
                            Key(
                                label = char.toString(),
                                shape = keyShape,
                                alternates = DIACRITICS[char] ?: emptyList(),
                                onAlternateSelected = onKeyPress,
                                modifier = Modifier.weight(1f),
                                onClick = { onKeyPress(char) }
                            )
                        }
                        IconKey(keyShape, modifier = Modifier.weight(1.5f), onClick = onBackspace) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = stringResource(R.string.search_action_backspace))
                        }
                    }
                }
            }

            KeyboardActionBar(
                onHome = onHome,
                onTogglePage = { tick(); showingNumbersPage = !showingNumbersPage },
                onSpace = onSpace,
                showingNumbersPage = showingNumbersPage,
                handedness = handedness,
                iconShape = iconShape,
                d = d,
                background = colors.solidBackground,
                border = colors.borderColor,
                dividerColor = colors.dividerColor,
                modifier = Modifier.padding(
                    start = d.innerHorizontalPadding,
                    end = d.innerHorizontalPadding,
                    bottom = d.innerVerticalPadding,
                    top = ROW_GAP
                )
            )
            }
        }
        if (sparksEnabled) KeySparkOverlay(sparks, sparkColor, Modifier.matchParentSize())
        }
        }
    }
}

@Composable
private fun KeyRow(chars: String, onKeyPress: (Char) -> Unit, shape: Shape) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
        chars.forEach { char ->
            Key(
                label = char.toString(),
                shape = shape,
                alternates = DIACRITICS[char] ?: emptyList(),
                onAlternateSelected = onKeyPress,
                modifier = Modifier.weight(1f),
                onClick = { onKeyPress(char) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Key(
    label: String,
    shape: Shape,
    modifier: Modifier = Modifier,
    alternates: List<Char> = emptyList(),
    onAlternateSelected: (Char) -> Unit = {},
    onClick: () -> Unit
) {
    var showAlternates by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val popupOffsetPx = remember(density) { with(density) { (KEY_HEIGHT + 6.dp).roundToPx() } }

    Box(
        modifier = modifier
            .height(KEY_HEIGHT)
            .clip(shape)
            .background(LocalContentColor.current.copy(alpha = 0.08f))
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (alternates.isNotEmpty()) ({ showAlternates = true }) else null
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)

        if (showAlternates) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, -popupOffsetPx),
                onDismissRequest = { showAlternates = false }
            ) {
                Row(
                    modifier = Modifier
                        .clip(shape)
                        .background(LocalContentColor.current.copy(alpha = 0.12f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(KEY_GAP)
                ) {
                    alternates.forEach { alt ->
                        Box(
                            modifier = Modifier
                                .height(KEY_HEIGHT)
                                .width(KEY_HEIGHT)
                                .clip(shape)
                                .background(LocalContentColor.current.copy(alpha = 0.18f))
                                .clickable {
                                    onAlternateSelected(alt)
                                    showAlternates = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(alt.toString(), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconKey(shape: Shape, modifier: Modifier = Modifier, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Row(
        modifier = modifier
            .height(KEY_HEIGHT)
            .clip(shape)
            .background(LocalContentColor.current.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
    }
}

@Composable
private fun rememberKeyHaptic(enabled: Boolean): () -> Unit {
    val view = androidx.compose.ui.platform.LocalView.current
    return remember(enabled, view) {
        if (!enabled) {
            {}
        } else {
            ({ view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP); Unit })
        }
    }
}
