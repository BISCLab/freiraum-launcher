package com.bisc.flauncher.ui.home

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
 * gesture silently stopped firing everywhere. This instead reads raw pointer moves directly, but only
 * at [PointerEventPass.Final] — the pass that runs *after* every descendant (including a hosted
 * widget's own native scrolling) has already had its chance to consume the touch — so a widget's own
 * scroll still wins outright; this only reacts to whatever's left unclaimed.
 */
@Composable
fun Modifier.detectHomeGestures(
    onLongPress: () -> Unit,
    onDoubleTap: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit
): Modifier {
    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { 96.dp.toPx() } }
    return this
        .pointerInput(thresholdPx, onSwipeUp, onSwipeDown) {
            awaitEachGesture {
                var accumulated = 0f
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                    val change = event.changes.firstOrNull() ?: break
                    if (!change.pressed) break
                    if (change.isConsumed) continue
                    accumulated += change.positionChange().y
                    if (accumulated <= -thresholdPx) {
                        onSwipeUp()
                        break
                    }
                    if (accumulated >= thresholdPx) {
                        onSwipeDown()
                        break
                    }
                }
            }
        }
        .pointerInput(onLongPress, onDoubleTap) {
            detectTapGestures(onLongPress = { onLongPress() }, onDoubleTap = { onDoubleTap() })
        }
}
