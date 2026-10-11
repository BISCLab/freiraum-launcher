package com.bisc.flauncher.ui.home.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.BackgroundSource
import com.bisc.flauncher.data.model.ColorFill
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.FillMode
import com.bisc.flauncher.ui.home.dock.rememberBackgroundLuminance
import com.bisc.flauncher.ui.home.dock.resolveElementColors

/** Custom appearance of the A-Z rail; null selects the automatic colors. */
val LocalRailAppearance = compositionLocalOf<ElementAppearance?> { null }

/** The A–Z rail's box, outline and letter colours. */
data class RailColors(
    val box: Brush,
    val boxAlpha: Float,
    val border: Color,
    val borderWidth: Dp,
    val showBorder: Boolean,
    val text: Color
)

val LocalRailColors = compositionLocalOf { automaticRailColors(false) }

/** Text colour on a light / dark surface. */
val TextOnLight = Color.Black.copy(alpha = 0.87f)
val TextOnDark = Color.White

private val RailBoxOnDark = Color(0xFF2B2B30)

fun automaticRailColors(surfaceIsDark: Boolean): RailColors =
    if (surfaceIsDark) RailColors(SolidColor(RailBoxOnDark), 1f, Color.White.copy(alpha = 0.3f), 1.dp, true, TextOnDark)
    else RailColors(SolidColor(Color.White), 1f, Color.Black, 1.dp, true, TextOnLight)

/** Concrete appearance values equivalent to the automatic rail colors. */
fun automaticRailAppearance(surfaceIsDark: Boolean): ElementAppearance =
    if (surfaceIsDark) {
        ElementAppearance(fill = ColorFill(color = RailBoxOnDark.toArgb()), borderEnabled = true, borderColor = Color.White.copy(alpha = 0.3f).toArgb())
    } else {
        ElementAppearance(fill = ColorFill(color = Color.White.toArgb()), borderEnabled = true, borderColor = Color.Black.toArgb())
    }

/** Rail colors resolved from a custom appearance. */
fun customRailColors(appearance: ElementAppearance, surfaceIsDark: Boolean): RailColors {
    val c = resolveElementColors(appearance, surfaceIsDark)
    return RailColors(c.background, c.backgroundAlpha, c.borderColor, c.borderWidth, appearance.borderEnabled, c.contentColor)
}

private fun ColorFill.averageLuminance(): Float = when (mode) {
    FillMode.SOLID -> Color(color).luminance()
    FillMode.GRADIENT -> (Color(color).luminance() + Color(secondaryColor).luminance()) / 2f
}

/** Whether the surface behind the browse list reads as dark. */
@Composable
fun rememberSurfaceIsDark(fill: ElementAppearance, page: BackgroundConfig): Boolean {
    val context = LocalContext.current
    val systemDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    // Decoded once (off the main thread) and cached, see BackgroundImageStore.
    val imageLum = rememberBackgroundLuminance(if (page.source == BackgroundSource.IMAGE) page.imagePath else null)
    if (fill.fillMode == ElementFillMode.COLOR && fill.opacity >= 0.5f) {
        return fill.fill.averageLuminance() < 0.5f
    }
    if (page.opacity < 0.5f) return systemDark
    val pageLum = when (page.source) {
        BackgroundSource.COLOR -> page.fill.averageLuminance()
        BackgroundSource.IMAGE -> imageLum ?: return systemDark
    }
    return pageLum * (1f - page.darken) < 0.5f
}
