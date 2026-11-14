package com.bisc.flauncher.ui.home.dock

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.asBrush

data class ElementColors(
    val background: Brush,
    val backgroundAlpha: Float = 1f,
    val solidBackground: Color,
    val borderColor: Color,
    val borderWidth: Dp = 1.dp,
    val dividerColor: Color,
    val contentColor: Color,
    /** Whether a border is drawn: the element's own choice, or always in the accessibility mode. */
    val showBorder: Boolean = false
)

private fun ElementAppearance.fillLuminance(): Float = when (fill.mode) {
    com.bisc.flauncher.data.model.FillMode.SOLID -> Color(fill.color).luminance()
    com.bisc.flauncher.data.model.FillMode.GRADIENT -> (Color(fill.color).luminance() + Color(fill.secondaryColor).luminance()) / 2f
}

/** Resolves fill, border and content colors of an element; automatic colors follow the surface behind it. */
@Composable
fun resolveElementColors(appearance: ElementAppearance): ElementColors {
    val surfaceDark = com.bisc.flauncher.ui.home.LocalSurfaceIsDark.current
        ?: (MaterialTheme.colorScheme.background.luminance() < 0.5f)
    return resolveElementColors(appearance, surfaceDark)
}

/** Resolves fill, border and content colors of an element; automatic colors follow the surface behind it. */
fun resolveElementColors(appearance: ElementAppearance, surfaceDark: Boolean): ElementColors {
    // Accessibility mode: no see-through elements, a strong border on every one.
    val a11y = com.bisc.flauncher.ui.AccessibilityMode.on
    val look = if (!a11y) appearance else appearance.copy(
        fillMode = ElementFillMode.COLOR,
        fill = if (appearance.fillMode == ElementFillMode.NONE) com.bisc.flauncher.data.model.ColorFill(color = if (surfaceDark) 0xFF1C1C1E.toInt() else android.graphics.Color.WHITE) else appearance.fill,
        opacity = 1f,
        borderEnabled = true,
        borderWidth = maxOf(appearance.borderWidth, 2f)
    )
    val surfaceLuminance = if (surfaceDark) 0.05f else 0.9f
    val solidBackground = when (look.fillMode) {
        ElementFillMode.NONE -> Color.Transparent
        ElementFillMode.COLOR -> Color(look.fill.color)
    }
    val alpha = if (look.fillMode == ElementFillMode.COLOR) look.opacity.coerceIn(0f, 1f) else 0f
    val effectiveLuminance = if (look.fillMode == ElementFillMode.COLOR) {
        look.fillLuminance() * alpha + surfaceLuminance * (1f - alpha)
    } else surfaceLuminance
    val isLight = effectiveLuminance > 0.5f
    val autoBorder = if (isLight) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = if (look.fillMode == ElementFillMode.NONE) 0.3f else 0.2f)
    val autoContent = if (isLight) Color.Black.copy(alpha = 0.8f) else Color.White
    val fillLight = if (look.fillMode == ElementFillMode.COLOR) solidBackground.luminance() > 0.5f else !surfaceDark
    return ElementColors(
        background = if (look.fillMode == ElementFillMode.COLOR) look.fill.asBrush() else SolidColor(Color.Transparent),
        backgroundAlpha = if (look.fillMode == ElementFillMode.COLOR) alpha else 1f,
        solidBackground = solidBackground,
        borderColor = if (a11y) autoContent.copy(alpha = 0.9f) else look.borderColor?.let { Color(it) } ?: autoBorder,
        borderWidth = look.borderWidth.dp,
        dividerColor = if (fillLight) Color.Black else Color.White,
        contentColor = if (a11y) (if (isLight) Color.Black else Color.White) else look.contentColor?.let { Color(it) } ?: autoContent,
        showBorder = look.borderEnabled
    )
}

