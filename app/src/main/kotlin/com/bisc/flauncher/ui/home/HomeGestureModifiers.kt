package com.bisc.flauncher.ui.home

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

/** Recognizes the four rebindable [com.bisc.flauncher.data.model.HomeGesture]s directly on the home screen background. */
@Composable
fun Modifier.detectHomeGestures(
    onLongPress: () -> Unit,
    onDoubleTap: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit
): Modifier {
    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { 96.dp.toPx() } }
    val nestedScrollConnection = remember(thresholdPx, onSwipeUp, onSwipeDown) {
        object : NestedScrollConnection {
            var accumulated = 0f

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y == 0f) {
                    accumulated = 0f
                    return Offset.Zero
                }
                if (accumulated != 0f && (accumulated > 0f) != (available.y > 0f)) accumulated = 0f
                accumulated += available.y
                when {
                    accumulated <= -thresholdPx -> {
                        accumulated = 0f
                        onSwipeUp()
                    }
                    accumulated >= thresholdPx -> {
                        accumulated = 0f
                        onSwipeDown()
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                accumulated = 0f
                return Velocity.Zero
            }
        }
    }
    return this
        .nestedScroll(nestedScrollConnection)
        .pointerInput(onLongPress, onDoubleTap) {
            detectTapGestures(onLongPress = { onLongPress() }, onDoubleTap = { onDoubleTap() })
        }
}
