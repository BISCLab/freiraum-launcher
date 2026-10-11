package com.bisc.flauncher.ui.home.dock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp

/**
 * Selectable visual designs for the dock's central orb. Each is a direct port of its Figma vector (exact
 * path data and gradient stops, extracted from the user's "Ohne Namen" Figma file) drawn natively on a
 * [Canvas] rather than rasterized, so it scales losslessly with the dock and can later be recolored/
 * animated. New designs are added by extending this enum and [DockOrbArt]'s `when` — e.g. the planned
 * "users add their own orb designs later" feature would turn this into a sealed type with a CUSTOM case
 * carrying user-supplied art, without changing any caller of [DockOrbArt].
 */
enum class DockOrbDesign { DEFAULT, LEAF, DEW }

/** Parses one SVG path `d` string once; cheap enough to call per-draw since paths here are tiny. */
private fun svgPath(data: String): Path = PathParser().parsePathString(data).toPath()

/** Draws [design] filling the given square [size], scaled from its native Figma viewBox. */
@Composable
fun DockOrbArt(design: DockOrbDesign, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        // All three designs are authored against a 72x72 viewBox (the Default orb's original 82x82 Figma
        // export padded a 72-wide circle by 5 on each side — every one of its coordinates below is that
        // original value minus 5, so it lines up with Leaf/Dew's native 0..72 box with no extra margin).
        val s = this.size.width / 72f
        scale(scale = s, pivot = Offset.Zero) {
            when (design) {
                DockOrbDesign.DEFAULT -> drawDefaultOrb()
                DockOrbDesign.LEAF -> drawLeafOrb()
                DockOrbDesign.DEW -> drawDewOrb()
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDefaultOrb() {
    val center = Offset(36f, 36f)
    val ringBrush = Brush.linearGradient(
        0f to Color(0xFF1AF0FF), 0.34f to Color(0xFF3385FF), 0.66f to Color(0xFFA14AFF), 1f to Color(0xFFFF4DBD),
        start = Offset(4f, 36f), end = Offset(68f, 36f)
    )
    // Soft glow behind the aurora ring, approximating the SVG's feGaussianBlur drop-shadow.
    drawCircle(brush = ringBrush, radius = 29.5f, center = center, alpha = 0.35f, style = Stroke(width = 10f))
    // Base sphere.
    drawCircle(color = Color(0xFF0B0D16), radius = 36f, center = center)
    // Aurora ring.
    drawCircle(brush = ringBrush, radius = 29.5f, center = center, style = Stroke(width = 5f))
    // Inner glossy orbit.
    drawCircle(
        brush = Brush.linearGradient(
            0f to Color(0xFF12213D), 0.57f to Color(0xFF050913), 1f to Color(0xFF210E38),
            start = Offset(36f, 10f), end = Offset(36f, 62f)
        ),
        radius = 26f,
        center = center
    )
    drawCircle(color = Color(0xFF7AA3FF).copy(alpha = 0.34f), radius = 25.65f, center = center, style = Stroke(width = 0.7f))
    drawPath(
        svgPath("M16 25.5922C20 16.5922 30 13.5922 39 15.5922"),
        color = Color(0xFF96E0FF).copy(alpha = 0.52f),
        style = Stroke(width = 0.9f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    )
    drawPath(
        svgPath("M32 52C40 56 50 55 55 48"),
        color = Color(0xFFD187FF).copy(alpha = 0.45f),
        style = Stroke(width = 0.7f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLeafOrb() {
    val round = androidx.compose.ui.graphics.StrokeCap.Round
    drawPath(
        svgPath("M36 72C55.8823 72 72 55.8823 72 36C72 16.1177 55.8823 0 36 0C16.1177 0 0 16.1177 0 36C0 55.8823 16.1177 72 36 72Z"),
        brush = Brush.linearGradient(
            0f to Color(0xFF3A7466), 0.5f to Color(0xFF23554B), 1f to Color(0xFF1D443E),
            start = Offset(12f, 0f), end = Offset(60f, 72f)
        )
    )
    drawPath(
        svgPath("M36 68.7C54.0597 68.7 68.7 54.0597 68.7 36C68.7 17.9403 54.0597 3.3 36 3.3C17.9403 3.3 3.3 17.9403 3.3 36C3.3 54.0597 17.9403 68.7 36 68.7Z"),
        brush = Brush.linearGradient(
            0f to Color(0xFFD2F4D7), 0.45f to Color(0xFF6CC2A3), 1f to Color(0xFF739F87),
            start = Offset(11f, 9f), end = Offset(61f, 63f)
        ),
        alpha = 0.8f,
        style = Stroke(width = 1.15f)
    )
    drawPath(
        svgPath("M10 25C11.4142 20.9675 13.7327 17.3122 16.7778 14.3141C19.8229 11.3161 23.5138 9.05473 27.5678 7.7034C31.6218 6.35206 35.9313 5.94657 40.1662 6.51798C44.4012 7.08939 48.4491 8.62255 52 11"),
        color = Color(0xFFE9FFF0), alpha = 0.28f, style = Stroke(width = 0.7f, cap = round)
    )
    drawPath(
        svgPath("M11 59C13 55 15 53 19 50"),
        color = Color(0xFF7AC9A5), style = Stroke(width = 1.7f, cap = round)
    )
    val leafBody = svgPath("M17 53C9 36 21 16 56 14C58 39 38 60 17 53Z")
    drawPath(
        leafBody,
        brush = Brush.linearGradient(
            0f to Color(0xFF138C68), 0.48f to Color(0xFF59D8A1), 1f to Color(0xFFDAF6BE),
            start = Offset(17f, 53f), end = Offset(56f, 14f)
        )
    )
    drawPath(leafBody, color = Color(0xFFC3FAD4), alpha = 0.38f, style = Stroke(width = 0.6f))
    drawPath(
        svgPath("M17 53C9 36 21 16 56 14C41 24 24 43 17 53Z"),
        brush = Brush.linearGradient(0f to Color(0xFF167B63), 1f to Color(0xFFA9ECC5), start = Offset(15f, 45f), end = Offset(46f, 17f)),
        alpha = 0.66f
    )
    drawPath(
        svgPath("M16 54C29 41 40 26 55 15"),
        color = Color(0xFFE0FEE4), alpha = 0.75f, style = Stroke(width = 1.05f, cap = round)
    )
    drawPath(
        svgPath("M25 29C24 32 24 38 27 42C33 44 38 41 42 36M33 21C31 25 32 29 36 32C41 34 46 31 49 27"),
        color = Color(0xFFD6F8DD), alpha = 0.35f, style = Stroke(width = 0.55f, cap = round)
    )
    drawPath(
        svgPath("M16 40C18 27 31 18 45 17"),
        color = Color(0xFFF0FFF4), alpha = 0.35f, style = Stroke(width = 0.85f, cap = round)
    )
    drawPath(
        svgPath("M40.3 41.8C42.951 41.8 45.1 40.3673 45.1 38.6C45.1 36.8327 42.951 35.4 40.3 35.4C37.649 35.4 35.5 36.8327 35.5 38.6C35.5 40.3673 37.649 41.8 40.3 41.8Z"),
        color = Color(0xFF126B53), alpha = 0.23f
    )
    drawPath(
        svgPath("M40 40.7C42.3196 40.7 44.2 38.8196 44.2 36.5C44.2 34.1804 42.3196 32.3 40 32.3C37.6804 32.3 35.8 34.1804 35.8 36.5C35.8 38.8196 37.6804 40.7 40 40.7Z"),
        brush = Brush.radialGradient(
            0f to Color.White.copy(alpha = 0.85f), 0.4f to Color(0xFFDAFFF4).copy(alpha = 0.42f), 1f to Color(0xFF5DBEA5).copy(alpha = 0.7f),
            center = Offset(38.32f, 34.232f), radius = 6.72f
        )
    )
    drawPath(
        svgPath("M40 40.7C42.3196 40.7 44.2 38.8196 44.2 36.5C44.2 34.1804 42.3196 32.3 40 32.3C37.6804 32.3 35.8 34.1804 35.8 36.5C35.8 38.8196 37.6804 40.7 40 40.7Z"),
        color = Color(0xFFDEFFF2), alpha = 0.55f, style = Stroke(width = 0.65f)
    )
    drawPath(
        svgPath("M38.7 35.88C39.418 35.88 40 35.5308 40 35.1C40 34.6692 39.418 34.32 38.7 34.32C37.982 34.32 37.4 34.6692 37.4 35.1C37.4 35.5308 37.982 35.88 38.7 35.88Z"),
        color = Color.White, alpha = 0.8f
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDewOrb() {
    val round = androidx.compose.ui.graphics.StrokeCap.Round
    drawPath(
        svgPath("M36 72C55.8823 72 72 55.8823 72 36C72 16.1177 55.8823 0 36 0C16.1177 0 0 16.1177 0 36C0 55.8823 16.1177 72 36 72Z"),
        brush = Brush.linearGradient(
            0f to Color(0xFF6796A8), 0.48f to Color(0xFF345E7B), 1f to Color(0xFF4B527E),
            start = Offset(10f, 0f), end = Offset(59f, 72f)
        )
    )
    drawPath(
        svgPath("M36 68.7C54.0597 68.7 68.7 54.0597 68.7 36C68.7 17.9403 54.0597 3.3 36 3.3C17.9403 3.3 3.3 17.9403 3.3 36C3.3 54.0597 17.9403 68.7 36 68.7Z"),
        brush = Brush.linearGradient(
            0f to Color(0xFFE4FFFF), 0.45f to Color(0xFF84D4E1), 1f to Color(0xFFB0BEEA),
            start = Offset(10f, 8f), end = Offset(61f, 64f)
        ),
        alpha = 0.83f,
        style = Stroke(width = 1.15f)
    )
    drawPath(
        svgPath("M9 26C10.2051 22.264 12.1823 18.8231 14.8033 15.9007C17.4242 12.9783 20.6304 10.6397 24.2138 9.03664C27.7971 7.43358 31.6774 6.6019 35.603 6.59559C39.5285 6.58928 43.4115 7.40847 47 9"),
        color = Color(0xFFE5FFFF), alpha = 0.3f, style = Stroke(width = 0.75f, cap = round)
    )
    drawPath(
        svgPath("M36 64.1C47.0457 64.1 56 62.1748 56 59.8C56 57.4252 47.0457 55.5 36 55.5C24.9543 55.5 16 57.4252 16 59.8C16 62.1748 24.9543 64.1 36 64.1Z"),
        brush = Brush.linearGradient(
            0f to Color(0xFFAAECF6).copy(alpha = 0f), 0.4f to Color(0xFFD2FAFF).copy(alpha = 0.65f), 1f to Color(0xFFB4C8FF).copy(alpha = 0f),
            start = Offset(16f, 60f), end = Offset(57f, 60f)
        ),
        style = Stroke(width = 0.8f)
    )
    drawPath(
        svgPath("M37 60.5C43.9036 60.5 49.5 59.1121 49.5 57.4C49.5 55.6879 43.9036 54.3 37 54.3C30.0964 54.3 24.5 55.6879 24.5 57.4C24.5 59.1121 30.0964 60.5 37 60.5Z"),
        color = Color(0xFF284778), alpha = 0.22f
    )
    val dewBody = svgPath("M39 11C35 22 21 29 20 40C18 54 31 61 43 56C56 51 56 39 49 28C46 23 43 17 39 11Z")
    drawPath(
        dewBody,
        brush = Brush.linearGradient(
            0f to Color(0xFFE5FFFF), 0.35f to Color(0xFF94E9F2), 0.68f to Color(0xFF5DC5E6), 1f to Color(0xFFA1B4F6),
            start = Offset(29f, 14f), end = Offset(44f, 57f)
        )
    )
    drawPath(dewBody, color = Color(0xFFE1FFFF), alpha = 0.7f, style = Stroke(width = 0.7f))
    drawPath(
        svgPath("M39 11C35 22 21 29 20 40C18 50 25 57 34 58C26 51 25 40 30 32C33 26 37 19 39 11Z"),
        brush = Brush.linearGradient(
            0f to Color(0xFFF4FFFF).copy(alpha = 0.84f), 0.55f to Color(0xFFD1FAFF).copy(alpha = 0.38f), 1f to Color(0xFF568FDB).copy(alpha = 0.54f),
            start = Offset(24f, 27f), end = Offset(34f, 58f)
        )
    )
    drawPath(
        svgPath("M21 41C28 34 42 34 52 42C51 53 42 58 34 58C27 57 21 51 21 41Z"),
        brush = Brush.linearGradient(
            0f to Color(0xFFB7F8FF).copy(alpha = 0.45f), 0.45f to Color(0xFF5B99E0).copy(alpha = 0.3f), 1f to Color(0xFFD7DAFF).copy(alpha = 0.7f),
            start = Offset(36f, 35f), end = Offset(37f, 58f)
        ),
        alpha = 0.7f
    )
    drawPath(
        svgPath("M39 11C42 28 49 37 48 46C47 54 41 57 34 58C48 60 61 45 49 28C46 23 43 17 39 11Z"),
        color = Color(0xFF4269AC), alpha = 0.2f
    )
    drawPath(
        svgPath("M35 22C29 29 24 34 24 42"),
        color = Color.White, alpha = 0.8f, style = Stroke(width = 2.45f, cap = round)
    )
    drawPath(
        svgPath("M25 48.1C25.6627 48.1 26.2 47.2493 26.2 46.2C26.2 45.1507 25.6627 44.3 25 44.3C24.3373 44.3 23.8 45.1507 23.8 46.2C23.8 47.2493 24.3373 48.1 25 48.1Z"),
        color = Color(0xFFE9FFFF), alpha = 0.7f
    )
    drawPath(
        svgPath("M26 51C32 57 42 57 48 49"),
        color = Color(0xFFEDF8FF), alpha = 0.82f, style = Stroke(width = 1f, cap = round)
    )
    drawPath(
        svgPath("M27 41C33 38 41 38 46 42"),
        color = Color(0xFFDDFBFF), alpha = 0.46f, style = Stroke(width = 0.65f, cap = round)
    )
}
