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
 * Swipe up/down used to be read off [androidx.compose.ui.input.nestedscroll.NestedScrollConnection]'s
 * onPostScroll — leftover scroll a descendant *didn't* consume. That relied on some descendant actually
 * being a scrollable that drives the nested-scroll dispatch in the first place; once pages stopped
 * scrolling (no more [PageRowsColumn]-level verticalScroll) there was nothing left to drive it, so the
 * gesture silently stopped firing everywhere. This instead reads raw pointer moves directly.
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
    onSwipeDown: () -> Unit
): Modifier {
    val density = LocalDensity.current
    // Was 96dp, back when this read PointerEventPass.Final (after the pager's own Main-pass claim had
    // already consumed everything). Now that this reads Main pass and reliably sees events before the
    // pager does, a smaller threshold just makes the gesture feel snappier without giving up much —
    // real horizontal swipes still won't accumulate meaningful Y delta here, so this rarely fires by
    // accident on one.
    val thresholdPx = remember(density) { with(density) { 36.dp.toPx() } }
    // pointerInput(key) cancels and restarts its whole coroutine — including any gesture already
    // mid-flight — every time `key` changes identity. Keying directly on the callback lambdas (as this
    // used to) meant every recomposition of the caller (HomeScreen recomposes constantly: appListOpen,
    // overviewEntry, pages, ... all churn) handed in a *new* lambda instance, silently tearing down and
    // restarting this detector — losing whatever partial swipe was already being tracked. That's exactly
    // why this got noticeably less reliable specifically on repeat swipes in the same session (first one
    // after a fresh recomposition-quiet moment worked, later ones raced against a restart) rather than
    // being uniformly flaky. rememberUpdatedState always calls the *latest* callback without needing the
    // coroutine itself to restart, so the detector now only restarts when the threshold actually changes.
    val currentOnSwipeUp = rememberUpdatedState(onSwipeUp)
    val currentOnSwipeDown = rememberUpdatedState(onSwipeDown)
    val currentOnLongPress = rememberUpdatedState(onLongPress)
    val currentOnDoubleTap = rememberUpdatedState(onDoubleTap)
    return this
        .pointerInput(thresholdPx) {
            awaitEachGesture {
                var accumulated = 0f
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.firstOrNull() ?: break
                    if (!change.pressed) break
                    if (change.isConsumed) continue
                    accumulated += change.positionChange().y
                    if (accumulated <= -thresholdPx) {
                        currentOnSwipeUp.value()
                        break
                    }
                    if (accumulated >= thresholdPx) {
                        currentOnSwipeDown.value()
                        break
                    }
                }
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(onLongPress = { currentOnLongPress.value() }, onDoubleTap = { currentOnDoubleTap.value() })
        }
}
