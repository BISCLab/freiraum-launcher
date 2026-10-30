package com.bisc.flauncher.ui.home.dock

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/** Which edge of a screen-flush surface is free (not against a screen edge). */
enum class FreeEdge { TOP, BOTTOM }

/**
 * A border along only the free edge of a surface that sits flush against the screen's other edges
 * (the Panel and the search bar at the top, the Dock at the bottom). The stroke runs around the
 * rounded corners ([radius]) and on along both sides to the opposite edge, just outside the
 * surface's sides - which lie off-screen, so the line simply disappears into the screen edges
 * instead of ending visibly where the rounding starts. Across the free edge itself it stays within
 * the surface's bounds, so an enter/exit animation clipping to those bounds can't cut it off.
 * Must be applied before the surface's own shape clip.
 */
fun Modifier.freeEdgeBorder(width: Dp, color: Color, radius: Dp, edge: FreeEdge): Modifier = drawWithContent {
    drawContent()
    val stroke = width.toPx()
    val half = stroke / 2f
    val w = size.width
    val h = size.height
    val r = radius.toPx().coerceIn(0f, minOf(w, h) / 2f)
    val outer = r + half
    val path = Path().apply {
        when (edge) {
            FreeEdge.BOTTOM -> {
                val cy = h - half - outer
                moveTo(-half, 0f)
                lineTo(-half, cy)
                arcTo(Rect(r - outer, cy - outer, r + outer, cy + outer), 180f, -90f, false)
                lineTo(w - r, h - half)
                arcTo(Rect(w - r - outer, cy - outer, w - r + outer, cy + outer), 90f, -90f, false)
                lineTo(w + half, 0f)
            }
            FreeEdge.TOP -> {
                val cy = half + outer
                moveTo(-half, h)
                lineTo(-half, cy)
                arcTo(Rect(r - outer, cy - outer, r + outer, cy + outer), 180f, 90f, false)
                lineTo(w - r, half)
                arcTo(Rect(w - r - outer, cy - outer, w - r + outer, cy + outer), 270f, 90f, false)
                lineTo(w + half, h)
            }
        }
    }
    drawPath(path, color, style = Stroke(width = stroke))
}
