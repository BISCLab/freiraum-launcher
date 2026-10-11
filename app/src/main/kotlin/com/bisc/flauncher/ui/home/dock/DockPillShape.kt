package com.bisc.flauncher.ui.home.dock

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * The dock pill's own silhouette: a true stadium — both ends genuinely semicircular, not Figma's actual
 * exported corners (its path curves to full radius over a shorter horizontal run than the bar's own half
 * height, so its ends read as softly rounded but not *fully* round) — with a shallow, smooth hill rising
 * on both the top and bottom edge where the orb sits. Built procedurally (RoundRect union Ellipse) rather
 * than reusing Figma's path data for this reason: fully-round ends were an explicit, deliberate departure
 * from the Figma export, not an approximation of it. The ellipse is wide enough that its own curvature is
 * gentle (reads as a soft dome, not a ball with a seam) but still contained to a local bump around the
 * orb — a wider ellipse here reads as swallowing the whole pill into one blob instead of sitting on it.
 *
 * Being a real [Shape] (not a raw Canvas draw) means the pill gets a genuine native drop shadow via
 * [androidx.compose.ui.draw.shadow] — see [SearchBar]'s use of this. [createOutline] is given the *full*
 * Box size, which must already include [HILL_EXTRA_FRACTION] of the flat bar height as padding above and
 * below it (see [SearchBar]'s `Box(... .height(d.barHeight * (1f + DockPillShape.HILL_EXTRA_FRACTION * 2f)))`)
 * — expressed as a fraction of the bar height, not a fixed dp, so the hill's rise scales right along with
 * everything else on a wider or narrower dock instead of staying a constant size.
 */
object DockPillShape : Shape {
    const val HILL_EXTRA_FRACTION = 0.2f

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val barHeight = size.height / (1f + HILL_EXTRA_FRACTION * 2f)
        val hillExtraPx = barHeight * HILL_EXTRA_FRACTION
        val barTop = hillExtraPx
        val cornerRadius = CornerRadius(barHeight / 2f)
        val barPath = Path().apply {
            addRoundRect(
                RoundRect(Rect(0f, barTop, size.width, barTop + barHeight), cornerRadius, cornerRadius, cornerRadius, cornerRadius)
            )
        }
        val hubCenter = Offset(size.width / 2f, barTop + barHeight / 2f)
        val hillRadiusY = barHeight / 2f + hillExtraPx
        val hillRadiusX = hillRadiusY * 1.18f
        val hillPath = Path().apply {
            addOval(
                Rect(
                    left = hubCenter.x - hillRadiusX,
                    top = hubCenter.y - hillRadiusY,
                    right = hubCenter.x + hillRadiusX,
                    bottom = hubCenter.y + hillRadiusY
                )
            )
        }
        val fused = Path().apply { op(barPath, hillPath, PathOperation.Union) }
        return Outline.Generic(fused)
    }
}
