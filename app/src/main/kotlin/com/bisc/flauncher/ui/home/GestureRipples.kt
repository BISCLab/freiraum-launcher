package com.bisc.flauncher.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** What kind of touch a ripple answers. */
enum class RippleKind { TAP, DOUBLE_TAP, LONG_PRESS }

class GestureRipple(val x: Float, val y: Float, val kind: RippleKind, val startNanos: Long = System.nanoTime())

private fun durationNanos(kind: RippleKind): Long = when (kind) {
    RippleKind.TAP -> 450_000_000L
    RippleKind.DOUBLE_TAP -> 650_000_000L
    RippleKind.LONG_PRESS -> 750_000_000L
}

/** Rings spreading from a touch on the empty home screen, like a drop on water: one for a tap, two
 *  for a double tap, a soft swelling wave for a long press. Draws nothing once all have faded. */
@Composable
fun GestureRipples(ripples: SnapshotStateList<GestureRipple>, color: Color, modifier: Modifier = Modifier) {
    if (ripples.isEmpty()) return
    var now by remember { mutableLongStateOf(System.nanoTime()) }
    LaunchedEffect(Unit) {
        while (ripples.isNotEmpty()) {
            withFrameNanos { now = System.nanoTime() }
            ripples.removeAll { now - it.startNanos > durationNanos(it.kind) }
        }
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        ripples.forEach { ripple ->
            val t = ((now - ripple.startNanos).toFloat() / durationNanos(ripple.kind)).coerceIn(0f, 1f)
            val center = Offset(ripple.x, ripple.y)
            when (ripple.kind) {
                RippleKind.TAP -> ring(center, 48.dp.toPx() * ease(t), color, 1f - t)
                RippleKind.DOUBLE_TAP -> {
                    ring(center, 72.dp.toPx() * ease(t), color, 1f - t)
                    val second = ((t - 0.2f) / 0.8f).coerceIn(0f, 1f)
                    if (second > 0f) ring(center, 72.dp.toPx() * ease(second), color, 1f - second)
                }
                RippleKind.LONG_PRESS -> {
                    val radius = 120.dp.toPx() * ease(t)
                    drawCircle(color.copy(alpha = 0.16f * (1f - t)), radius, center)
                    ring(center, radius, color, 1f - t)
                }
            }
        }
    }
}

private fun DrawScope.ring(center: Offset, radius: Float, color: Color, alpha: Float) {
    drawCircle(color.copy(alpha = 0.55f * alpha), radius, center, style = Stroke(width = 2.dp.toPx()))
}

/** Fast start, gentle end. */
private fun ease(t: Float): Float = 1f - (1f - t) * (1f - t) * (1f - t)
