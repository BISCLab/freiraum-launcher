package com.bisc.flauncher.data.model

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

/** Whether a [ColorFill] is one flat color or a two-color gradient. */
enum class FillMode { SOLID, GRADIENT }

/** Which way a [FillMode.GRADIENT] runs. */
enum class GradientDirection { TOP_TO_BOTTOM, LEFT_TO_RIGHT }

/** A solid color or a two-color gradient with direction. */
data class ColorFill(
    val mode: FillMode = FillMode.SOLID,
    val color: Int = android.graphics.Color.WHITE,
    val secondaryColor: Int = android.graphics.Color.BLACK,
    val direction: GradientDirection = GradientDirection.TOP_TO_BOTTOM
)

fun ColorFill.asBrush(): Brush = when (mode) {
    FillMode.SOLID -> SolidColor(Color(color))
    FillMode.GRADIENT -> when (direction) {
        GradientDirection.TOP_TO_BOTTOM -> Brush.verticalGradient(listOf(Color(color), Color(secondaryColor)))
        GradientDirection.LEFT_TO_RIGHT -> Brush.horizontalGradient(listOf(Color(color), Color(secondaryColor)))
    }
}
