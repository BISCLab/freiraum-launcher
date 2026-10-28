package com.bisc.flauncher.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlin.random.Random
import kotlin.math.sin
import kotlin.math.PI
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.compose.LifecycleResumeEffect

private const val CYCLE_MILLIS = 20_000
private const val BUBBLE_COUNT = 26

private class Bubble(
    val x: Float,
    val radiusDp: Float,
    val trips: Int,
    val phase: Float,
    val swayCycles: Int,
    val swayPhase: Float,
    val swayDp: Float
)

private fun makeBubbles(): List<Bubble> {
    val random = Random(7)
    return List(BUBBLE_COUNT) {
        val radius = 5f + random.nextFloat() * 17f
        Bubble(
            x = random.nextFloat(),
            radiusDp = radius,
            trips = if (radius > 15f) 2 else if (radius > 9f) 3 else 4,
            phase = random.nextFloat(),
            swayCycles = 2 + random.nextInt(4),
            swayPhase = random.nextFloat(),
            swayDp = 6f + random.nextFloat() * 14f
        )
    }
}

/** Rising bubbles and a bottom glow while the device charges; active only while the launcher is resumed. */
@Composable
fun ChargingGlow(enabled: Boolean, color: Color = Color(com.bisc.flauncher.ui.theme.DEFAULT_ACCENT_COLOR), modifier: Modifier = Modifier) {
    val bubbleLight = androidx.compose.ui.graphics.lerp(color, Color.White, 0.55f)
    val context = LocalContext.current
    var charging by remember { mutableStateOf(false) }
    var resumed by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                charging = intent.isCharging()
            }
        }
        // ACTION_BATTERY_CHANGED is sticky.
        val sticky = ContextCompat.registerReceiver(
            context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        charging = sticky?.isCharging() ?: false
        resumed = true
        onPauseOrDispose {
            resumed = false
            context.unregisterReceiver(receiver)
        }
    }

    if (enabled && charging && resumed) {
        val bubbles = remember { makeBubbles() }
        val transition = rememberInfiniteTransition(label = "chargingBubbles")
        val time = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(CYCLE_MILLIS, easing = LinearEasing), RepeatMode.Restart),
            label = "chargingBubblesTime"
        )
        // Steady, not pulsing.
        val glowAlpha = 0.19f
        Box(
            modifier = modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(color.copy(alpha = glowAlpha), Color.Transparent),
                            center = Offset(size.width / 2f, size.height),
                            radius = size.width * 0.9f
                        )
                    )
                    val t = time.value
                    bubbles.forEach { bubble ->
                        val progress = (t * bubble.trips + bubble.phase) % 1f
                        val fadeIn = (progress / 0.08f).coerceIn(0f, 1f)
                        val fadeOut = ((1f - progress) / 0.25f).coerceIn(0f, 1f)
                        val alpha = fadeIn * fadeOut
                        if (alpha > 0.01f) {
                            val radius = bubble.radiusDp.dp.toPx() * (0.85f + 0.3f * progress)
                            val sway = sin(2f * PI.toFloat() * (t * bubble.swayCycles + bubble.swayPhase)) * bubble.swayDp.dp.toPx()
                            val center = Offset(
                                x = bubble.x * size.width + sway,
                                y = size.height + radius - progress * (size.height + 2f * radius)
                            )
                            drawCircle(color.copy(alpha = 0.16f * alpha), radius, center)
                            drawCircle(bubbleLight.copy(alpha = 0.65f * alpha), radius, center, style = Stroke(width = 1.6.dp.toPx()))
                            drawCircle(
                                Color.White.copy(alpha = 0.55f * alpha),
                                radius * 0.18f,
                                Offset(center.x - radius * 0.38f, center.y - radius * 0.38f)
                            )
                        }
                    }
                }
        )
    }
}

private fun Intent.isCharging(): Boolean = getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
