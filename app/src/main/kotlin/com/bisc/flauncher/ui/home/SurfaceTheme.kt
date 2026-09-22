package com.bisc.flauncher.ui.home

import androidx.compose.runtime.compositionLocalOf

/** Whether the surface behind unboxed home content is dark; null if unknown. */
val LocalSurfaceIsDark = compositionLocalOf<Boolean?> { null }

/**
 * True only while the real page background this frame's [LocalSurfaceIsDark] would be based on is
 * still loading from disk (so `null` there is a temporary unknown, not "no page context here").
 */
val LocalSurfaceIsDarkLoading = compositionLocalOf { false }
