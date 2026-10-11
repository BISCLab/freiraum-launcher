package com.bisc.flauncher.ui.home.dock

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * The dock pill's own silhouette — direct from Figma's "dock-smooth-hill-silhouette" vector (the
 * `dock-bar` frame, 390x92 reference), not a hand-approximated RoundRect/ellipse union. Two previous
 * attempts at approximating this (hand-tuned cubic beziers, then a RoundRect unioned with an ellipse)
 * both read as noticeably less polished than the real thing, which is why this reuses Figma's actual path
 * data instead: a capsule whose top AND bottom edge rise into a shallow, smooth hill at the center, where
 * the orb sits — see [DockDimens] for how icons/orb line up against it (both center on the path's own
 * y=46, exactly the vertical middle of its 92-unit height).
 *
 * [createOutline] scales the fixed 390x92 path to whatever [Size] it's asked to fill via a plain
 * [Matrix] transform — non-uniform if width/height don't scale by the same factor, matching how Figma
 * itself fit this path into frames of differing aspect ratio.
 */
object DockPillShape : Shape {
    private const val VIEWBOX_WIDTH = 390f
    private const val VIEWBOX_HEIGHT = 92f

    private const val PATH_DATA =
        "M195 0.5C202.987 0.5 210.833 2.60228 217.75 6.5957L217.762 6.60254L217.774 6.60938C225.906 10.7131 232.054 10.5 254 10.5H360C376.292 10.5 389.5 23.7076 389.5 40V52C389.5 68.2924 376.292 81.5 360 81.5H254C232.054 81.5 225.906 81.2869 217.774 85.3906L217.762 85.3975L217.75 85.4043C210.833 89.3977 202.987 91.5 195 91.5C187.013 91.5 179.167 89.3977 172.25 85.4043L172.238 85.3975L172.226 85.3906L171.468 85.0186C163.694 81.3003 157.26 81.5 136 81.5H30C13.7076 81.5 0.5 68.2924 0.5 52V40C0.5 23.7076 13.7076 10.5 30 10.5H136C157.946 10.5 164.094 10.7131 172.226 6.60938L172.238 6.60254L172.25 6.5957C179.167 2.60228 187.013 0.5 195 0.5Z"

    private val rawPath: Path by lazy { PathParser().parsePathString(PATH_DATA).toPath() }

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val scaleX = size.width / VIEWBOX_WIDTH
        val scaleY = size.height / VIEWBOX_HEIGHT
        val scaled = Path().apply {
            addPath(rawPath)
            transform(Matrix().apply { scale(x = scaleX, y = scaleY) })
        }
        return Outline.Generic(scaled)
    }
}
