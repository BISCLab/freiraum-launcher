package com.bisc.flauncher.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.TransformOrigin

/** How much the launcher animates: zoom and depth everywhere, a lighter version, or nothing.
 *  Android's own "Remove animations" switch always means [OFF]. */
enum class MotionLevel { FULL, SUBTLE, OFF }

val LocalMotion = staticCompositionLocalOf { MotionLevel.FULL }

/** Each animation on its own, on top of [MotionLevel]. */
data class MotionPrefs(
    val appZoom: Boolean = true,
    val overlayZoom: Boolean = true,
    val folderZoom: Boolean = true,
    val pageDepth: Boolean = true,
    val backGesture: Boolean = true,
    val settingsSlide: Boolean = true,
    val gestureRipples: Boolean = true
)

val LocalMotionPrefs = staticCompositionLocalOf { MotionPrefs() }

/** This level for an animation that's switched on, none for one that's off. */
fun MotionLevel.unless(enabled: Boolean): MotionLevel = if (enabled) this else MotionLevel.OFF

/** Android's own "Remove animations" (Accessibility) is on. */
fun systemAnimationsOff(context: android.content.Context): Boolean =
    android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/** Smooth start, soft landing - the curve all zoom transitions share. */
val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** Where the finger last touched the screen, so things open from and close into that spot
 *  (the tapped icon) instead of the middle of the screen. Set by MainActivity. */
object TouchOrigin {
    var x = -1f
    var y = -1f
    /** Whether an app launch zooms out of the touched spot. */
    var launchZoom = true

    /** [x]/[y] as a transform origin within a screen of [width] x [height] pixels; without a touch
     *  yet, the bottom center, where swipe-up gestures start. */
    fun transformOrigin(width: Int, height: Int): TransformOrigin =
        if (x < 0f || width <= 0 || height <= 0) TransformOrigin(0.5f, 1f)
        else TransformOrigin((x / width).coerceIn(0f, 1f), (y / height).coerceIn(0f, 1f))
}

/** A page or overlay growing out of [origin]. */
fun zoomIn(level: MotionLevel, origin: TransformOrigin): EnterTransition = when (level) {
    MotionLevel.FULL -> scaleIn(tween(320, easing = EmphasizedEasing), initialScale = 0.82f, transformOrigin = origin) + fadeIn(tween(200))
    MotionLevel.SUBTLE -> scaleIn(tween(240, easing = EmphasizedEasing), initialScale = 0.95f, transformOrigin = origin) + fadeIn(tween(160))
    MotionLevel.OFF -> EnterTransition.None
}

/** The reverse of [zoomIn]: shrinking back towards [origin]. */
fun zoomOut(level: MotionLevel, origin: TransformOrigin): ExitTransition = when (level) {
    MotionLevel.FULL -> scaleOut(tween(240, easing = EmphasizedEasing), targetScale = 0.86f, transformOrigin = origin) + fadeOut(tween(180))
    MotionLevel.SUBTLE -> scaleOut(tween(180, easing = EmphasizedEasing), targetScale = 0.96f, transformOrigin = origin) + fadeOut(tween(140))
    MotionLevel.OFF -> ExitTransition.None
}

/** How far a page shrinks while it's swiped away, at most. */
fun pageDepth(level: MotionLevel): Float = when (level) {
    MotionLevel.FULL -> 0.08f
    MotionLevel.SUBTLE -> 0.03f
    MotionLevel.OFF -> 0f
}

/** The spot an overlay opened from, captured the moment [open] turns true and kept for its closing,
 *  so it shrinks back to where it came from. */
@Composable
fun rememberOpenOrigin(open: Boolean): TransformOrigin {
    val view = LocalView.current
    val origin = remember { arrayOf(TransformOrigin(0.5f, 1f)) }
    val wasOpen = remember { booleanArrayOf(false) }
    if (open && !wasOpen[0]) origin[0] = TouchOrigin.transformOrigin(view.width, view.height)
    wasOpen[0] = open
    return origin[0]
}
