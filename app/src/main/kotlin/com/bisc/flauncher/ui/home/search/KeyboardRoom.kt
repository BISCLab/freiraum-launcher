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
 * Where the keyboard will end in the results area, known before it gets there: [restPx] is the
 * area's height without the keyboard, [overlapPx] how much the open keyboard takes from it, and
 * [keyboardMode] whether the keyboard is up or on its way. The rail and the strip over the keyboard
 * both place themselves by it, so they are in place at once instead of chasing the keyboard.
 */
data class KeyboardRoom(val restPx: Float, val overlapPx: Float, val keyboardMode: Boolean) {
    /** The top of the keyboard (with its strip) once it is up, from the top of the area. */
    val keyboardTopPx: Float get() = restPx - overlapPx
}

/** [areaPx] is the results area's current height; [cachedOverlapPx] what the keyboard took last
 *  time (0 = not known), updated through [onOverlap] once the keyboard is fully up. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun rememberKeyboardRoom(areaPx: Float, expectKeyboard: Boolean, cachedOverlapPx: Int, onOverlap: (Int) -> Unit): KeyboardRoom {
    val density = LocalDensity.current
    // The keyboard's final height is known from the start of its animation; until it starts, the
    // height it took last time stands in.
    val imeNow = WindowInsets.ime.getBottom(density)
    val imeTarget = WindowInsets.imeAnimationTarget.getBottom(density)
    var keyboardSeen by remember { mutableStateOf(false) }
    if (imeTarget > 0) keyboardSeen = true
    LaunchedEffect(Unit) {
        // A keyboard that was expected but never came does not hold things up for long.
        kotlinx.coroutines.delay(900)
        keyboardSeen = true
    }
    val keyboardMode = imeTarget > 0 || (expectKeyboard && !keyboardSeen)

    // The area's height without the keyboard, kept while the keyboard is up.
    var restPx by remember { mutableFloatStateOf(0f) }
    if (imeNow == 0 && imeTarget == 0) restPx = areaPx
    else if (restPx == 0f) restPx = areaPx + cachedOverlapPx
    // Once the keyboard is fully up, how much it really takes (navigation bar, the strip on top of
    // it and all) is remembered for the next time.
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
