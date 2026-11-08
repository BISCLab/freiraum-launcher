package com.bisc.flauncher.ui.theme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** What 100 % means for the corners of every part: the same radius everywhere, so a value looks
 *  the same on a widget, a card, the Dock or a search field. */
val FULL_CORNER_RADIUS = 28.dp

/** [percent] of [full], but never rounder than fully round for a part of this [size]. */
data class PartCornerShape(val percent: Int, val full: Dp = FULL_CORNER_RADIUS) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val radius = minOf(with(density) { full.toPx() } * percent.coerceIn(0, 100) / 100f, size.minDimension / 2)
        return Outline.Rounded(RoundRect(size.toRect(), CornerRadius(radius)))
    }
}

/** The radius [percent] stands for on a part whose shorter side is [shortSide]. */
fun partCornerRadius(percent: Int, shortSide: Dp): Dp = minOf(FULL_CORNER_RADIUS * (percent.coerceIn(0, 100) / 100f), shortSide / 2)
