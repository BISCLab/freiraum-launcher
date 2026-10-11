package com.bisc.flauncher.ui.home.dock

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.PathParser

/**
 * The dock pill's own silhouette, direct from the Figma source ("dock-smooth-hill-silhouette"): a capsule
 * whose top edge rises into a shallow hill at the center, where the orb sits, instead of a plain straight
 * top edge. Kept as raw path data (not a fixed Shape) because the bar is drawn with [androidx.compose.ui.graphics.drawscope.DrawScope.scale]
 * directly against this native viewBox rather than through Compose's clip/Shape system — see [SearchBar].
 */
object DockBarShapes {
    /** Native viewBox size of [plainHillPathData] and [auroraHillPathData]. */
    const val PLAIN_VIEWBOX_WIDTH = 390f
    const val PLAIN_VIEWBOX_HEIGHT = 92f

    const val AURORA_VIEWBOX_WIDTH = 426f
    const val AURORA_VIEWBOX_HEIGHT = 128f

    const val AURORA_EDGE_VIEWBOX_WIDTH = 407.6f
    const val AURORA_EDGE_VIEWBOX_HEIGHT = 109.6f

    const val AURORA_UNDERGLOW_VIEWBOX_WIDTH = 414f
    const val AURORA_UNDERGLOW_VIEWBOX_HEIGHT = 70f

    /** The plain dock style's pill — flat sides, shallow central hill. */
    private const val PLAIN_HILL_DATA =
        "M195 0.5C202.987 0.5 210.833 2.60228 217.75 6.5957L217.762 6.60254L217.774 6.60938C225.906 10.7131 232.054 10.5 254 10.5H360C376.292 10.5 389.5 23.7076 389.5 40V52C389.5 68.2924 376.292 81.5 360 81.5H254C232.054 81.5 225.906 81.2869 217.774 85.3906L217.762 85.3975L217.75 85.4043C210.833 89.3977 202.987 91.5 195 91.5C187.013 91.5 179.167 89.3977 172.25 85.4043L172.238 85.3975L172.226 85.3906L171.468 85.0186C163.694 81.3003 157.26 81.5 136 81.5H30C13.7076 81.5 0.5 68.2924 0.5 52V40C0.5 23.7076 13.7076 10.5 30 10.5H136C157.946 10.5 164.094 10.7131 172.226 6.60938L172.238 6.60254L172.25 6.5957C179.167 2.60228 187.013 0.5 195 0.5Z"

    /** The aurora/glass dock style's pill body (fill only; the thin inner highlight stroke is skipped — see [SearchBar]). */
    private const val AURORA_HILL_DATA =
        "M56 16H154C178 16 184 15.925 195.768 11.349C201.244 9.137 207.095 8 213 8C218.905 8 224.756 9.137 230.232 11.349C242 15.925 248 16 272 16H370C390.987 16 408 33.0132 408 54C408 74.9868 390.987 92 370 92H272C248 92 242 92.075 230.232 96.651C224.756 98.863 218.905 100 213 100C207.095 100 201.244 98.863 195.768 96.651C184 92.075 178 92 154 92H56C35.0132 92 18 74.9868 18 54C18 33.0132 35.0132 16 56 16Z"

    /** Colorful glow ring drawn just outside the aurora pill's own edge. */
    private const val AURORA_EDGE_DATA =
        "M46.8 16.8H144.8C168.8 16.8 174.8 16.725 186.568 12.149C192.044 9.937 197.895 8.8 203.8 8.8C209.705 8.8 215.556 9.937 221.032 12.149C232.8 16.725 238.8 16.8 262.8 16.8H360.8C381.787 16.8 398.8 33.8132 398.8 54.8C398.8 75.7868 381.787 92.8 360.8 92.8H262.8C238.8 92.8 232.8 92.875 221.032 97.451C215.556 99.663 209.705 100.8 203.8 100.8C197.895 100.8 192.044 99.663 186.568 97.451C174.8 92.875 168.8 92.8 144.8 92.8H46.8C25.8132 92.8 8.8 75.7868 8.8 54.8C8.8 33.8132 25.8132 16.8 46.8 16.8Z"

    /** Soft under-glow arc along the pill's top edge (open path, stroke-only). */
    private const val AURORA_UNDERGLOW_DATA =
        "M12 12C12 32.9868 29.0132 50 50 50H148C172 50 178 50.075 189.768 54.651C195.244 56.863 201.095 58 207 58C212.905 58 218.756 56.863 224.232 54.651C236 50.075 242 50 266 50H364C384.987 50 402 32.9868 402 12"

    val plainHillPath: Path by lazy { svgDataToPath(PLAIN_HILL_DATA) }
    val auroraHillPath: Path by lazy { svgDataToPath(AURORA_HILL_DATA) }
    val auroraEdgePath: Path by lazy { svgDataToPath(AURORA_EDGE_DATA) }
    val auroraUnderglowPath: Path by lazy { svgDataToPath(AURORA_UNDERGLOW_DATA) }
}

private fun svgDataToPath(data: String): Path = PathParser().parsePathString(data).toPath()
