package com.bisc.flauncher.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.data.design.DesignEffects
import com.bisc.flauncher.data.design.EffectMotion
import com.bisc.flauncher.ui.LocalMotion
import com.bisc.flauncher.ui.MotionLevel

/** The applied design's effects; null when none is applied (the normal, calm look). */
val LocalDesignEffects = staticCompositionLocalOf<DesignEffects?> { null }

/** A sweep gradient turned by [degrees] - the glowing border's light running round its edge. */
private class TurningSweep(private val colors: IntArray, private val stops: FloatArray, private val degrees: Float) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val cx = size.width / 2f
        val cy = size.height / 2f
        return android.graphics.SweepGradient(cx, cy, colors, stops).apply {
            setLocalMatrix(android.graphics.Matrix().apply { setRotate(degrees, cx, cy) })
        }
    }
}

/** Like a neon tube: mostly the faint base color, the design's colors in one stretch of the edge. */
private fun sweepColors(effects: DesignEffects): Pair<IntArray, FloatArray> {
    val glow = effects.glowColors.ifEmpty { listOf(0xFF4DFFB0.toInt(), 0xFF4DB8FF.toInt(), 0xFFB47BFF.toInt(), 0xFFFF6AD5.toInt()) }
    val base = effects.baseColor
    val colors = mutableListOf(base, base)
    val stops = mutableListOf(0f, 0.5f)
    glow.forEachIndexed { i, c ->
        colors += c
        stops += 0.6f + 0.3f * i / (glow.size - 1).coerceAtLeast(1)
    }
    colors += base; stops += 0.97f
    colors += base; stops += 1f
    return colors.toIntArray() to stops.toFloatArray()
}

/** The border's turning angle: still, one turn when it appears, or slowly round for good. */
@Composable
private fun rememberGlowAngle(effects: DesignEffects): State<Float> {
    val motion = if (LocalMotion.current == MotionLevel.OFF) EffectMotion.STILL else effects.motion
    return when (motion) {
        EffectMotion.SLOW -> rememberInfiniteTransition(label = "glow").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween((effects.speedSeconds * 1000).toInt(), easing = LinearEasing), RepeatMode.Restart),
            label = "glowAngle"
        )
        EffectMotion.ONCE -> {
            val angle = remember { Animatable(0f) }
            LaunchedEffect(Unit) { angle.animateTo(360f, tween(1400, easing = com.bisc.flauncher.ui.EmphasizedEasing)) }
            angle.asState()
        }
        EffectMotion.STILL -> remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }
}

/**
 * The design's glowing border on a surface of [shape] - put it before the surface's own clip so the
 * glow can spread past the edge. [target] is the kind of surface (dock, panel, search, widgets,
 * folders, cards, buttons); the design decides which ones glow. Does nothing without a design.
 */
@Composable
fun Modifier.designGlow(shape: Shape, target: String): Modifier {
    val effects = LocalDesignEffects.current ?: return this
    if (!effects.glowBorder || target !in effects.targets) return this
    val (colors, stops) = remember(effects) { sweepColors(effects) }
    val angle = rememberGlowAngle(effects)
    val strength = effects.glowStrength
    val surfaceGlow = effects.surfaceGlow
    return this
        .drawBehind {
            if (strength > 0f) {
                val outline = shape.createOutline(size, layoutDirection, this)
                val brush = TurningSweep(colors, stops, angle.value)
                drawOutline(outline, brush, alpha = 0.16f * strength, style = Stroke(width = 12.dp.toPx()))
                drawOutline(outline, brush, alpha = 0.32f * strength, style = Stroke(width = 5.dp.toPx()))
            }
        }
        .drawWithContent {
            drawContent()
            val outline = shape.createOutline(size, layoutDirection, this)
            val brush = TurningSweep(colors, stops, angle.value)
            if (surfaceGlow > 0f) {
                // Light falling in from the edge, fading towards the middle, and a faint tint.
                clipPath(Path().apply { addOutline(outline) }) {
                    drawOutline(outline, brush, alpha = 0.10f * surfaceGlow, style = Stroke(width = 28.dp.toPx()))
                    drawOutline(outline, brush, alpha = 0.16f * surfaceGlow, style = Stroke(width = 12.dp.toPx()))
                    drawOutline(outline, brush, alpha = 0.05f * surfaceGlow)
                }
            }
            drawOutline(outline, brush, style = Stroke(width = 1.5.dp.toPx()))
        }
}

/** The design's background extras on top of a page background: a fine grid and soft colored
 *  light spots. Static, so it costs nothing while the screen sits still. */
@Composable
fun DesignBackgroundEffects(modifier: Modifier = Modifier) {
    val effects = LocalDesignEffects.current ?: return
    if (!effects.grid && !effects.blobs) return
    Canvas(modifier = modifier.fillMaxSize()) {
        if (effects.blobs) drawBlobs(effects)
        if (effects.grid) drawGrid(effects)
    }
}

private fun DrawScope.drawBlobs(effects: DesignEffects) {
    val colors = effects.blobColors.ifEmpty { listOf(0xFF4DFFB0.toInt(), 0xFFB47BFF.toInt(), 0xFF4DB8FF.toInt()) }
    // Same spots as the reference: top left, top right, below the bottom.
    val spots = listOf(Offset(0.10f, -0.05f) to 0.75f, Offset(0.95f, 0.10f) to 0.85f, Offset(0.50f, 1.10f) to 1.1f)
    spots.forEachIndexed { i, (spot, reach) ->
        val color = Color(colors[i % colors.size])
        val center = Offset(size.width * spot.x, size.height * spot.y)
        val radius = size.width * reach
        drawCircle(
            brush = Brush.radialGradient(
                0f to color.copy(alpha = 0.13f * effects.blobStrength),
                1f to Color.Transparent,
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}

private fun DrawScope.drawGrid(effects: DesignEffects) {
    val step = effects.gridSpacingDp.dp.toPx()
    val color = Color(effects.gridColor)
    val line = 1.dp.toPx()
    var x = 0f
    while (x < size.width) { drawLine(color, Offset(x, 0f), Offset(x, size.height), line); x += step }
    var y = 0f
    while (y < size.height) { drawLine(color, Offset(0f, y), Offset(size.width, y), line); y += step }
}
