package com.bisc.flauncher.ui.home.dock

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.bisc.flauncher.data.model.LauncherPageKind

/**
 * The 4 page-icon symbols from Figma (`hIu9AMwmphER50czNQGeBe`, frames 81:109/91:2/95:2/95:33) —
 * drawn procedurally from their exact proportions rather than imported as multi-color gradient
 * SVGs, since every dock icon is rendered through a single flat [tint] (same as every Material
 * icon used elsewhere in the dock) and the designs are simple enough (a ring, a 3×3 grid, three
 * rows, four panels) that hand-transcribing 9+ translated gradient paths would be far more
 * fragile than just reproducing the geometry at the right ratios.
 */
enum class PageIconGlyph {
    SEARCH, APP_DRAWER, APP_LIST, WIDGET_PAGE;

    companion object {
        /** The glyph a page shows by default, before any per-page override. */
        fun forPageKind(kind: LauncherPageKind): PageIconGlyph = when (kind) {
            LauncherPageKind.ROWS -> WIDGET_PAGE
            LauncherPageKind.APP_LIST -> APP_LIST
            LauncherPageKind.SEARCH -> SEARCH
        }
    }
}

@Composable
fun PageIconGlyphImage(glyph: PageIconGlyph, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        when (glyph) {
            PageIconGlyph.SEARCH -> drawSearchGlyph(tint)
            PageIconGlyph.APP_DRAWER -> drawAppDrawerGlyph(tint)
            PageIconGlyph.APP_LIST -> drawAppListGlyph(tint)
            PageIconGlyph.WIDGET_PAGE -> drawWidgetPageGlyph(tint)
        }
    }
}

/** Figma "Search Ring / Smooth Glass Fold" (88:7), reduced to its base ring (outer r=68.34/140,
 *  inner r=39.77/140 of the 140×140 viewBox) — the gradient fold/reflection layers on top of it
 *  only make sense in full color, not as a flat tint. */
private fun DrawScope.drawSearchGlyph(color: Color) {
    val d = size.minDimension
    drawCircle(color = color, radius = d * 0.386f, style = Stroke(width = d * 0.204f))
}

/** Figma "Appdrawer / Clean 3×3" (91:14): nine identical rounded tiles, margin 19.92%, tile
 *  16.41%, gap 5.47% of the 256×256 frame. */
private fun DrawScope.drawAppDrawerGlyph(color: Color) {
    val margin = size.width * 0.1992f
    val marginY = size.height * 0.1992f
    val tile = size.width * 0.1641f
    val tileY = size.height * 0.1641f
    val gap = size.width * 0.0547f
    val gapY = size.height * 0.0547f
    val corner = CornerRadius(tile * 0.286f, tileY * 0.286f)
    for (row in 0..2) {
        for (col in 0..2) {
            drawRoundRect(
                color = color,
                topLeft = Offset(margin + col * (tile + gap), marginY + row * (tileY + gapY)),
                size = Size(tile, tileY),
                cornerRadius = corner
            )
        }
    }
}

/** Figma "Appliste / Three App Rows" (95:23): three rows, each a small square tile + a wide
 *  "name" pill, row height 14.06%, tops at 20.31%/42.97%/65.63% of the 256×256 frame. */
private fun DrawScope.drawAppListGlyph(color: Color) {
    val rowHeight = size.height * 0.1406f
    val rowTops = listOf(0.2031f, 0.4297f, 0.6563f)
    val tileLeft = size.width * 0.1875f
    val pillLeft = size.width * 0.40625f
    val pillWidth = size.width * 0.40625f
    rowTops.forEach { topRatio ->
        val top = size.height * topRatio
        drawRoundRect(
            color = color,
            topLeft = Offset(tileLeft, top),
            size = Size(rowHeight, rowHeight),
            cornerRadius = CornerRadius(rowHeight * 0.278f)
        )
        val pillHeight = rowHeight * 0.333f
        drawRoundRect(
            color = color,
            topLeft = Offset(pillLeft, top + rowHeight * 0.333f),
            size = Size(pillWidth, pillHeight),
            cornerRadius = CornerRadius(pillHeight / 2f)
        )
    }
}

/** Figma "Widgets / Modular Four-Panel Layout" (95:54): one wide top panel, one tall left panel,
 *  two small right panels — exact ratios of the 256×256 frame. */
private fun DrawScope.drawWidgetPageGlyph(color: Color) {
    val corner = CornerRadius(size.width * 0.0469f, size.height * 0.0469f)
    fun panel(left: Float, top: Float, right: Float, bottom: Float) {
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * left, size.height * top),
            size = Size(size.width * (right - left), size.height * (bottom - top)),
            cornerRadius = corner
        )
    }
    panel(0.1992f, 0.1992f, 0.8008f, 0.3867f)
    panel(0.1992f, 0.4414f, 0.4492f, 0.8008f)
    panel(0.5039f, 0.4414f, 0.8008f, 0.5938f)
    panel(0.5039f, 0.6484f, 0.8008f, 0.8008f)
}
