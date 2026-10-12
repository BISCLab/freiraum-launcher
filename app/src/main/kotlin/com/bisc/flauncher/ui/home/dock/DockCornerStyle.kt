package com.bisc.flauncher.ui.home.dock

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How rounded the dock bar's own corners are — a user choice, applied via [LocalDockCornerRadius] (see
 * HomeScreen, which provides [radius] through that composition local) so every surface that already reads
 * [dockDimens]'s `cornerRadius` — the dock itself, the text-search field, etc. — picks it up for free.
 */
enum class DockCornerStyle {
    SHARP,
    ROUNDED,
    FULLY_ROUND;

    /** FULLY_ROUND uses a deliberately oversized radius — [androidx.compose.foundation.shape.RoundedCornerShape] clamps it to half the shape's own shorter side, which is exactly a true stadium with no need to know the bar's height in advance. */
    val radius: Dp
        get() = when (this) {
            SHARP -> 0.dp
            ROUNDED -> 20.dp
            FULLY_ROUND -> 999.dp
        }
}
