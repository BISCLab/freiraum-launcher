package com.bisc.flauncher.ui.home.search

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val SPARK_LIFE_NANOS = 380_000_000L
private const val PARTICLES_PER_BURST = 7
private const val MAX_BURSTS = 12

private class SparkBurst(val origin: Offset, val startNanos: Long, val seed: Int)

/** The bursts currently in the air. */
class KeySparks {
    internal val bursts = mutableStateListOf<Any>()

    fun spawn(origin: Offset) {
        if (bursts.size >= MAX_BURSTS) bursts.removeAt(0)
        bursts.add(SparkBurst(origin, System.nanoTime(), Random.nextInt()))
    }
}

@Composable
fun rememberKeySparks(): KeySparks = remember { KeySparks() }

/** A quick shower of small sparks where a key is pressed. */
@Composable
fun KeySparkOverlay(sparks: KeySparks, color: Color, modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(System.nanoTime()) }
    LaunchedEffect(sparks.bursts.isNotEmpty()) {
        while (sparks.bursts.isNotEmpty()) {
            withFrameNanos { }
            now = System.nanoTime()
            sparks.bursts.removeAll { (it as SparkBurst).startNanos + SPARK_LIFE_NANOS < now }
        }
    }
    Canvas(modifier = modifier) {
        val time = now
        val glow = lerp(color, Color.White, 0.45f)
        sparks.bursts.toList().forEach { any ->
            val burst = any as SparkBurst
            val t = ((time - burst.startNanos).toFloat() / SPARK_LIFE_NANOS).coerceIn(0f, 1f)
            if (t >= 1f) return@forEach
            val eased = 1f - (1f - t) * (1f - t)
            val random = Random(burst.seed)
            repeat(PARTICLES_PER_BURST) { i ->
                val angle = (i + random.nextFloat() * 0.8f) / PARTICLES_PER_BURST * 2f * Math.PI.toFloat()
                val reach = (15f + random.nextFloat() * 20f).dp.toPx()
                val tail = (5f + random.nextFloat() * 5f).dp.toPx()
                val direction = Offset(cos(angle), sin(angle))
                val head = burst.origin + direction * (reach * eased)
                val start = burst.origin + direction * (reach * eased - tail * (1f - t)).coerceAtLeast(0f)
                val alpha = (1f - t)
                drawLine(
                    color = lerp(color, glow, t).copy(alpha = alpha),
                    start = start,
                    end = head,
                    strokeWidth = (1.6f + random.nextFloat() * 1.2f).dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(glow.copy(alpha = alpha * 0.9f), radius = 1.4.dp.toPx(), center = head)
            }
        }
    }
}
