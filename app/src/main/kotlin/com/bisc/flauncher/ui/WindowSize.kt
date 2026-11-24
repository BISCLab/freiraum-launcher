package com.bisc.flauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp

/** The width of the window the launcher is drawn in (in split screen: its part only). */
@Composable
fun windowWidth(): Dp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }

/** The height of the window the launcher is drawn in. */
@Composable
fun windowHeight(): Dp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }

/** How tall the content of a sheet that slides up from below may get: most of the screen, so short
 *  lists like the symbols show whole instead of scrolling. */
@Composable
fun sheetMaxHeight(): Dp = (windowHeight() * 0.85f - androidx.compose.ui.unit.Dp(96f)).coerceAtLeast(androidx.compose.ui.unit.Dp(320f))
