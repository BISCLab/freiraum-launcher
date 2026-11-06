package com.bisc.flauncher.ui.home

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
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
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** What kind of touch a ripple answers. */
enum class RippleKind { TAP, DOUBLE_TAP, LONG_PRESS }

class GestureRipple(val x: Float, val y: Float, val kind: RippleKind, val startNanos: Long = System.nanoTime())

private fun durationNanos(kind: RippleKind): Long = when (kind) {
    RippleKind.TAP -> 700_000_000L
    RippleKind.DOUBLE_TAP -> 900_000_000L
    RippleKind.LONG_PRESS -> 1_100_000_000L
}

/** How far a drop's wave travels. */
private fun reachDp(kind: RippleKind) = when (kind) {
    RippleKind.TAP -> 90f
    RippleKind.DOUBLE_TAP -> 120f
    RippleKind.LONG_PRESS -> 170f
}

/** The real drop on water needs Android 13 (runtime shaders); older phones get the rings. */
private val shaderDrops get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !shaderBroken

/** Set if the phone can't build the water shader; the rings take over then. */
@Volatile private var shaderBroken = false

/** A frame clock that runs only while ripples are alive and drops the finished ones. */
@Composable
private fun rippleClock(ripples: SnapshotStateList<GestureRipple>): Long {
    var now by remember { mutableLongStateOf(System.nanoTime()) }
    LaunchedEffect(ripples.isNotEmpty()) {
        while (ripples.isNotEmpty()) {
            withFrameNanos { now = System.nanoTime() }
            ripples.removeAll { now - it.startNanos > durationNanos(it.kind) }
        }
    }
    return now
}

/** Bends what's underneath like water around the newest drop: two or three crests that spread and
 *  die out, lit on one side so they show on plain backgrounds too. Android 13 and newer only; does
 *  nothing while no drop is running. */
@Composable
fun Modifier.waterRipples(ripples: SnapshotStateList<GestureRipple>): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || shaderBroken || ripples.isEmpty()) return this
    val now = rippleClock(ripples)
    val density = LocalDensity.current.density
    val shader = remember { runCatching { RuntimeShader(WATER_AGSL) }.onFailure { shaderBroken = true }.getOrNull() } ?: return this
    val drop = ripples.lastOrNull() ?: return this
    val t = ((now - drop.startNanos).toFloat() / durationNanos(drop.kind)).coerceIn(0f, 1f)
    val effect = runCatching {
        shader.setFloatUniform("center", drop.x, drop.y)
        shader.setFloatUniform("progress", t)
        shader.setFloatUniform("reach", reachDp(drop.kind) * density)
        shader.setFloatUniform("wave", (if (drop.kind == RippleKind.LONG_PRESS) 22f else 15f) * density)
        shader.setFloatUniform("strength", (if (drop.kind == RippleKind.TAP) 5f else 7f) * density)
        shader.setFloatUniform("second", if (drop.kind == RippleKind.DOUBLE_TAP) 0.25f else -1f)
        RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
    }.onFailure { shaderBroken = true }.getOrNull() ?: return this
    return graphicsLayer { renderEffect = effect }
}

/** Rings spreading from a touch on the empty home screen. On Android 13 and newer the drop itself is
 *  drawn by [waterRipples]; here only the older phones get soft double rings that thin out. */
@Composable
fun GestureRipples(ripples: SnapshotStateList<GestureRipple>, color: Color, modifier: Modifier = Modifier) {
    if (ripples.isEmpty()) return
    val now = rippleClock(ripples)
    if (shaderDrops) return
    Canvas(modifier = modifier.fillMaxSize()) {
        ripples.forEach { ripple ->
            val t = ((now - ripple.startNanos).toFloat() / durationNanos(ripple.kind)).coerceIn(0f, 1f)
            val center = Offset(ripple.x, ripple.y)
            val reach = reachDp(ripple.kind).dp.toPx()
            drops(ripple.kind).forEach { delay ->
                val local = ((t - delay) / (1f - delay)).coerceIn(0f, 1f)
                if (local > 0f) {
                    val fade = (1f - local) * (1f - local)
                    ring(center, reach * ease(local), color, fade, 2.5f * (1f - local) + 0.5f)
                    ring(center, reach * 0.62f * ease(local), color, fade * 0.5f, 1.5f * (1f - local) + 0.5f)
                    if (ripple.kind == RippleKind.LONG_PRESS) drawCircle(color.copy(alpha = 0.12f * fade), reach * ease(local), center)
                }
            }
        }
    }
}

/** When each wave of a drop starts, as a share of its run: a double tap sends two. */
private fun drops(kind: RippleKind) = if (kind == RippleKind.DOUBLE_TAP) listOf(0f, 0.25f) else listOf(0f)

private fun DrawScope.ring(center: Offset, radius: Float, color: Color, alpha: Float, widthDp: Float) {
    drawCircle(color.copy(alpha = 0.5f * alpha), radius, center, style = Stroke(width = widthDp.dp.toPx()))
}

/** Fast start, gentle end. */
private fun ease(t: Float): Float = 1f - (1f - t) * (1f - t) * (1f - t)

// A packet of 2-3 crests around the wave front: pixels are pushed along the radius by the wave's
// height, and lit or shaded by its slope. Everything fades as the wave spreads.
private const val WATER_AGSL = """
uniform shader content;
uniform float2 center;
uniform float progress;
uniform float reach;
uniform float wave;
uniform float strength;
uniform float second;

float ease(float t) { float u = 1.0 - t; return 1.0 - u * u * u; }

// Height (x) and slope (y) of one wave at distance [dist] from the drop, [t] into its run.
float2 wave2(float dist, float t) {
    if (t <= 0.0 || t >= 1.0) return float2(0.0);
    float x = dist - reach * ease(t);
    // Crests behind the front, none ahead of it.
    float env = exp(-(x * x) / (wave * wave * 2.2)) * (1.0 - smoothstep(-wave * 0.2, wave * 0.8, x));
    float fade = (1.0 - t) * (1.0 - t);
    float phase = x / wave * 6.2831;
    return float2(sin(phase), cos(phase)) * env * fade;
}

half4 main(float2 p) {
    float2 d = p - center;
    float dist = length(d);
    float2 w = wave2(dist, progress);
    if (second >= 0.0) w += wave2(dist, (progress - second) / (1.0 - second)) * 0.8;
    float2 n = dist > 0.001 ? d / dist : float2(0.0);
    half4 c = content.eval(p - n * w.x * strength);
    half light = half(w.y * 0.07);
    c.rgb = clamp(c.rgb + half3(light), half3(0.0), half3(c.a));
    return c;
}
"""
