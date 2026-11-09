package com.bisc.flauncher.ui.home.search

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity

/**
 * Where the keyboard ends in the results area: [restPx] is the area's height without the keyboard,
 * [overlapPx] how much the open keyboard (with its strip) takes from it, [keyboardMode] whether the
 * keyboard is up or on its way. [bottomPx] is where the results and the rail end right now and
 * [following] says they ride down with a closing keyboard instead of waiting for it.
 */
data class KeyboardRoom(
    val restPx: Float,
    val overlapPx: Float,
    val keyboardMode: Boolean,
    val bottomPx: Float = if (keyboardMode) restPx - overlapPx else restPx,
    val following: Boolean = false
) {
    /** The top of the keyboard (with its strip) once it is up, from the top of the area. */
    val keyboardTopPx: Float get() = restPx - overlapPx
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun rememberKeyboardSeen(expectKeyboard: Boolean, imeTarget: Int): Boolean {
    var keyboardSeen by remember { mutableStateOf(false) }
    if (imeTarget > 0) keyboardSeen = true
    LaunchedEffect(Unit) {
        // A keyboard that was expected but never came does not hold things up for long.
        kotlinx.coroutines.delay(900)
        keyboardSeen = true
    }
    return imeTarget > 0 || (expectKeyboard && !keyboardSeen)
}

/**
 * For an area that does not shrink with the keyboard (it ends above the navigation bar): opening,
 * the results and the rail end where the keyboard will end from the first frame on; closing, they
 * ride down with the keyboard's top, frame by frame. [capPx] is the strip's height, [rise] how much
 * of it shows at a keyboard height. [cachedOverlapPx] stands in until the keyboard says its height.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun rememberKeyboardRoomOverArea(
    areaPx: Float,
    expectKeyboard: Boolean,
    cachedOverlapPx: Int,
    onOverlap: (Int) -> Unit,
    capPx: Int,
    rise: (Int, Int) -> Int
): KeyboardRoom {
    val density = LocalDensity.current
    val imeNow = WindowInsets.ime.getBottom(density)
    val imeTarget = WindowInsets.imeAnimationTarget.getBottom(density)
    val nav = WindowInsets.navigationBars.getBottom(density)
    val keyboardMode = rememberKeyboardSeen(expectKeyboard, imeTarget)
    val targetOverlap = if (imeTarget > 0) (imeTarget - nav).coerceAtLeast(0) + capPx else 0
    LaunchedEffect(targetOverlap) {
        if (targetOverlap > 0 && kotlin.math.abs(targetOverlap - cachedOverlapPx) > 2) onOverlap(targetOverlap)
    }
    val overlap = (if (targetOverlap > 0) targetOverlap else cachedOverlapPx).toFloat()
    val liveOverlap = (imeNow + rise(imeNow, capPx) - nav).coerceAtLeast(0).toFloat()
    return KeyboardRoom(
        restPx = areaPx,
        overlapPx = overlap,
        keyboardMode = keyboardMode,
        bottomPx = if (keyboardMode) areaPx - overlap else areaPx - liveOverlap,
        following = !keyboardMode && imeNow > 0
    )
}

/** For an area that shrinks with the keyboard (the bar at the bottom): [areaPx] is its current
 *  height; what the keyboard took last time is remembered through [onOverlap]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun rememberKeyboardRoom(areaPx: Float, expectKeyboard: Boolean, cachedOverlapPx: Int, onOverlap: (Int) -> Unit): KeyboardRoom {
    val density = LocalDensity.current
    val imeNow = WindowInsets.ime.getBottom(density)
    val imeTarget = WindowInsets.imeAnimationTarget.getBottom(density)
    val keyboardMode = rememberKeyboardSeen(expectKeyboard, imeTarget)

    // The area's height without the keyboard, kept while the keyboard is up.
    var restPx by remember { mutableFloatStateOf(0f) }
    if (imeNow == 0 && imeTarget == 0) restPx = areaPx
    else if (restPx == 0f) restPx = areaPx + cachedOverlapPx
    val settled = imeNow == imeTarget && imeTarget > 0
    val measuredOverlap = (restPx - areaPx).toInt()
    LaunchedEffect(settled, measuredOverlap) {
        if (settled && measuredOverlap > 0 && kotlin.math.abs(measuredOverlap - cachedOverlapPx) > 2) onOverlap(measuredOverlap)
    }
    val overlap = when {
        settled -> restPx - areaPx
        cachedOverlapPx > 0 -> cachedOverlapPx.toFloat()
        else -> imeTarget - WindowInsets.navigationBars.getBottom(density).toFloat()
    }.coerceAtLeast(0f)
    return KeyboardRoom(restPx, overlap, keyboardMode)
}
