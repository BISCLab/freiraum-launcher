package com.bisc.flauncher.ui

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

/** The accessibility mode: every element solid with a strong border, larger icons, no blur.
 *  Set from the settings in MainActivity; read wherever elements are drawn. */
object AccessibilityMode {
    var on by mutableStateOf(false)

    /** How much larger app icons are while it is on. */
    const val ICON_SCALE = 1.25f
}

/** A softer shade of a text or symbol color, except in the accessibility mode, where text and
 *  symbols always keep their full color. */
fun Color.softened(alpha: Float): Color = if (AccessibilityMode.on) this else copy(alpha = alpha)

/** A button's size, at least 48 dp in the accessibility mode so it is easy to hit. */
fun Dp.touchTarget(): Dp = if (AccessibilityMode.on) maxOf(this, 48.dp) else this

/** App names: larger and heavier in the accessibility mode. */
fun TextStyle.forAppName(): TextStyle = if (!AccessibilityMode.on) this
    else copy(fontWeight = FontWeight.SemiBold, fontSize = if (fontSize.isSpecified) fontSize * 1.15f else fontSize)

/** A visible outline around a settings card or tile in the accessibility mode. */
@Composable
fun Modifier.accessibleOutline(shape: Shape): Modifier =
    if (!AccessibilityMode.on) this else this.border(1.5.dp, MaterialTheme.colorScheme.onSurface, shape)
