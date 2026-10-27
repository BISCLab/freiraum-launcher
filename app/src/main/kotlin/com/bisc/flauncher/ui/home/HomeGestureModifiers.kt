package com.bisc.flauncher.ui.home

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Recognizes the four rebindable [com.bisc.flauncher.data.model.HomeGesture]s directly on the home
 * screen background.
 *
 * Reads raw pointer moves directly rather than nested-scroll leftovers: pages don't scroll, so there
 * is no scrollable descendant that would drive a nested-scroll dispatch.
 *
 * Uses [PointerEventPass.Main], not [PointerEventPass.Final]. Pointer passes for one event run
 * Initial (root → leaf), then Main (leaf → root), then Final (root → leaf) again. This modifier sits on
 * a page's content Box, which is a *descendant* of [HorizontalPager] but an *ancestor* of any widget on
 * that page. Reading at Main means: any widget's own scrollable — deeper in the tree — already had its
 * Main-pass turn and could've consumed the event before this code runs, so a real widget scroll still
 * wins outright; but the pager itself — further up the tree — hasn't had its Main-pass turn *yet*, so
 * this gets first claim on a clearly-vertical drag instead of the pager's own ~8dp horizontal-only touch
 * slop grabbing it first regardless of how much more vertical the motion actually was. Final was tried
 * first specifically to be conservative about not competing with widgets, but it runs after the pager's
 * own Main-pass claim too, so it could only ever react to drags the pager never wanted in the first
 * place — which is exactly why a mostly-vertical swipe kept losing to the pager as a page-swipe.
 */
@Composable
fun Modifier.detectHomeGestures(
    onLongPress: () -> Unit,
    onDoubleTap: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    // Excludes a strip at the top/bottom edge from triggering onSwipeDown/onSwipeUp when the drag
    // *starts* inside it, so Android's own edge gestures (status bar reveal when we've hidden it;
    // the gesture-nav task switcher) stay reachable even though this detector otherwise covers the
    // whole home content area. 0 (the default) means no exclusion.
    topSafeZonePx: Float = 0f,
    bottomSafeZonePx: Float = 0f
): Modifier {
    val density = LocalDensity.current
    // Was 96dp, back when this read PointerEventPass.Final (after the pager's own Main-pass claim had
    // already consumed everything). Now that this reads Main pass and reliably sees events before the
    // pager does, a smaller threshold just makes the gesture feel snappier without giving up much —
    // real horizontal swipes still won't accumulate meaningful Y delta here, so this rarely fires by
    // accident on one.
    val thresholdPx = remember(density) { with(density) { 36.dp.toPx() } }
    // pointerInput(key) restarts its coroutine, dropping any swipe in progress, whenever `key`
    // changes. The callbacks are new lambdas on every recomposition of HomeScreen, so they are read
    // through rememberUpdatedState instead of being keys: the detector only restarts when the
    // threshold changes.
    val currentOnSwipeUp = rememberUpdatedState(onSwipeUp)
    val currentOnSwipeDown = rememberUpdatedState(onSwipeDown)
    val currentOnLongPress = rememberUpdatedState(onLongPress)
    val currentOnDoubleTap = rememberUpdatedState(onDoubleTap)
    return this
        .pointerInput(thresholdPx, topSafeZonePx, bottomSafeZonePx) {
            awaitEachGesture {
                // Captured once, from the very first event of the gesture — used only to check
                // whether the drag started inside a safe zone, never updated afterward.
                val firstChange = awaitPointerEvent(PointerEventPass.Main).changes.firstOrNull() ?: return@awaitEachGesture
                if (!firstChange.pressed) return@awaitEachGesture
                val startY = firstChange.position.y
                var accumulated = 0f
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.firstOrNull() ?: break
                    if (!change.pressed) break
                    if (change.isConsumed) continue
                    accumulated += change.positionChange().y
                    if (accumulated <= -thresholdPx) {
                        if (startY < size.height - bottomSafeZonePx) currentOnSwipeUp.value()
                        break
                    }
                    if (accumulated >= thresholdPx) {
                        if (startY > topSafeZonePx) currentOnSwipeDown.value()
                        break
                    }
                }
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(onLongPress = { currentOnLongPress.value() }, onDoubleTap = { currentOnDoubleTap.value() })
        }
}

