package com.bisc.flauncher.ui.home.dock

import android.content.ComponentName
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.data.settings.IconShape

/**
 * Dock: one continuous pill of 4 quick-launch icons with the Hub orb overlapping its top edge, centered
 * — fused into the same surface rather than floating above a cutout. The pill itself follows [appearance]
 * (white/black by theme by default, same per-row customization — color, opacity, border — every other
 * dock-style row already has); the orb is always tinted with [accentColor], independently. Tap the orb to
 * open the expert (full text) search; drag up to open the app list; drag either left or right to open the
 * category search (the sidebar overview panel) — [handedness] no longer picks a side for this, both
 * directions do the same thing for now, kept as a parameter for a future "right drag does something
 * else" assignment.
 */
@Composable
fun SearchBar(
    onHubTap: () -> Unit,
    onHubSwipeUp: () -> Unit,
    onHubCategorySearch: () -> Unit,
    modifier: Modifier = Modifier,
    handedness: Handedness = Handedness.RIGHT,
    iconShape: IconShape = IconShape.ROUNDED_SQUARE,
    appearance: ElementAppearance = ElementAppearance(),
    accentColor: Color = Color(0xFF3B82F6),
    quickLaunchSlots: List<QuickLaunchSlot> = defaultQuickLaunchSlots,
    loadQuickLaunchIcon: (ComponentName) -> ImageBitmap? = { null },
    onQuickLaunchClick: (ComponentName) -> Unit = {}
) {
    val colors = resolveElementColors(appearance)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = DockDimens.horizontalMargin,
                end = DockDimens.horizontalMargin,
                top = DockDimens.topMargin,
                bottom = DockDimens.bottomMargin
            ),
        contentAlignment = Alignment.Center
    ) {
        val d = dockDimens(maxWidth)
        val itemShape = when (iconShape) {
            IconShape.CIRCLE -> CircleShape
            IconShape.ROUNDED_SQUARE -> RoundedCornerShape(d.iconCornerRadius)
        }
        val leftSlots = quickLaunchSlots.take(2)
        val rightSlots = quickLaunchSlots.drop(2).take(2)

        @Composable
        fun QuickLaunchIcon(slot: QuickLaunchSlot) {
            val bitmap = remember(slot.componentName) { slot.componentName?.let(loadQuickLaunchIcon) }
            Box(
                modifier = Modifier
                    .size(d.iconSize)
                    .clip(itemShape)
                    .background(if (bitmap == null) slot.placeholderTint else Color.Transparent)
                    .clickable(onClick = { slot.componentName?.let(onQuickLaunchClick) }),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(bitmap = bitmap, contentDescription = slot.label, modifier = Modifier.size(d.iconSize))
                } else {
                    Icon(imageVector = slot.placeholderIcon, contentDescription = null, tint = Color.White)
                }
            }
        }

        val dragOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
        val pressScale = remember { Animatable(1f) }
        val orbitAngle by rememberInfiniteTransition(label = "hubOrbit").animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
            label = "hubOrbitAngle"
        )

        // The orb is sized larger than the bar is tall, and its center sits exactly on the bar's own
        // center — both axes, not perched above it — so simply centering it already leaves it breaking
        // out symmetrically above and below. The bar and the orb are drawn as ONE path (RoundRect union
        // Circle), so the orb genuinely widens the dock's own silhouette right where it sits, instead of
        // a separate piece merely overlapping it. No glow, no extra ring — just the fused shape and the
        // orb's own glossy fill marking it as the special one.
        Box(modifier = Modifier.width(d.pillWidth).height(d.hubSize)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barTop = (d.hubSize - d.barHeight).toPx() / 2f
                val barHeightPx = d.barHeight.toPx()
                val hubRadius = (d.hubSize.toPx() / 2f) * pressScale.value
                val hubCenter = Offset(size.width / 2f, size.height / 2f) + dragOffset.value
                val cr = CornerRadius(d.cornerRadius.toPx())
                val barPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(0f, barTop, size.width, barTop + barHeightPx),
                            topLeft = cr,
                            topRight = cr,
                            bottomRight = cr,
                            bottomLeft = cr
                        )
                    )
                }
                val orbPath = Path().apply { addOval(Rect(center = hubCenter, radius = hubRadius)) }
                val fused = Path().apply { op(barPath, orbPath, PathOperation.Union) }

                drawPath(fused, brush = colors.background, alpha = colors.backgroundAlpha)
                if (appearance.borderEnabled) {
                    drawPath(fused, color = colors.borderColor, style = Stroke(width = colors.borderWidth.toPx()))
                }
                // The orb's own finish: mostly a pearly white sphere with the accent color only as a tint
                // at the rim and in its glow — not a solid saturated ball — plus a small bright spark that
                // slowly orbits the surface instead of a literal icon, for a "quietly alive" read rather
                // than a specific function being printed on it.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, lerp(Color.White, accentColor, 0.3f), lerp(accentColor, Color.Black, 0.35f)),
                        center = hubCenter + Offset(-hubRadius * 0.32f, -hubRadius * 0.38f),
                        radius = hubRadius * 1.2f
                    ),
                    radius = hubRadius,
                    center = hubCenter
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to Color.Transparent,
                        0.68f to Color.Transparent,
                        1f to lerp(accentColor, Color.Black, 0.2f).copy(alpha = 0.35f),
                        center = hubCenter,
                        radius = hubRadius
                    ),
                    radius = hubRadius,
                    center = hubCenter
                )
                drawOval(
                    color = Color.White.copy(alpha = 0.55f),
                    topLeft = hubCenter + Offset(-hubRadius * 0.5f, -hubRadius * 0.7f),
                    size = Size(hubRadius * 0.5f, hubRadius * 0.3f)
                )
                val sparkAngle = orbitAngle
                val sparkCenter = hubCenter + Offset(cos(sparkAngle) * hubRadius * 0.52f, sin(sparkAngle) * hubRadius * 0.52f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0f)),
                        center = sparkCenter,
                        radius = hubRadius * 0.22f
                    ),
                    radius = hubRadius * 0.22f,
                    center = sparkCenter
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(d.pillWidth)
                    .height(d.barHeight)
                    .padding(horizontal = d.innerHorizontalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leftSlots.forEachIndexed { index, slot ->
                    if (index > 0) Spacer(modifier = Modifier.width(d.itemSpacing))
                    QuickLaunchIcon(slot)
                }
                Spacer(modifier = Modifier.width(d.hubGap))
                Spacer(modifier = Modifier.width(d.hubSize))
                Spacer(modifier = Modifier.width(d.hubGap))
                rightSlots.forEachIndexed { index, slot ->
                    if (index > 0) Spacer(modifier = Modifier.width(d.itemSpacing))
                    QuickLaunchIcon(slot)
                }
            }
            // Invisible hit-target matching the orb's drawn position exactly (same center as the Canvas
            // above). No icon on it by design — the orb reads as one mysterious, multi-purpose control,
            // not a labeled search button — but it keeps its accessibility description.
            val hubDescription = stringResource(R.string.dock_search_hint)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(dragOffset.value.x.roundToInt(), dragOffset.value.y.roundToInt()) }
                    .size(d.hubSize)
                    .semantics { contentDescription = hubDescription }
                    .detectHubGestures(
                        onTap = onHubTap,
                        onSwipeUp = onHubSwipeUp,
                        onCategorySearch = onHubCategorySearch,
                        dragOffset = dragOffset,
                        pressScale = pressScale
                    )
            )
        }
    }
}

private const val QUICK_DOUBLE_TAP_WINDOW_MS = 150L
private val HUB_GESTURE_THRESHOLD = 28.dp
private val HUB_MAX_DRAG = 16.dp

/**
 * The Hub's three gestures live on one pointerInput so they can never be mistaken for each other: a
 * drag has to clearly commit to one axis (whichever of up/left/right first crosses the threshold by the
 * widest margin) before anything fires, and a release without ever crossing it is a tap. Left and right
 * both resolve to [onCategorySearch] — see the type doc on [SearchBar] for why. [dragOffset] is written
 * to directly during the drag (clamped to [HUB_MAX_DRAG]) so the orb visibly tracks the finger, then
 * eased back with a real spring once the gesture ends; [pressScale] gives the same tactile dip on down.
 * Keyed only on stable values (rememberUpdatedState for the callbacks) so this doesn't restart — and
 * lose a mid-gesture drag — every time the caller recomposes with fresh lambdas.
 */
@Composable
private fun Modifier.detectHubGestures(
    onTap: () -> Unit,
    onSwipeUp: () -> Unit,
    onCategorySearch: () -> Unit,
    dragOffset: Animatable<Offset, androidx.compose.animation.core.AnimationVector2D>,
    pressScale: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>
): Modifier {
    val currentOnTap = rememberUpdatedState(onTap)
    val currentOnSwipeUp = rememberUpdatedState(onSwipeUp)
    val currentOnCategorySearch = rememberUpdatedState(onCategorySearch)
    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { HUB_GESTURE_THRESHOLD.toPx() } }
    val maxDragPx = remember(density) { with(density) { HUB_MAX_DRAG.toPx() } }
    val coroutineScope = rememberCoroutineScope()
    return pointerInput(thresholdPx) {
        awaitEachGesture {
            val down = awaitFirstDown()
            coroutineScope.launch { pressScale.animateTo(0.92f, spring(stiffness = Spring.StiffnessHigh)) }
            var totalDx = 0f
            var totalDy = 0f
            var resolved = false
            while (!resolved) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                totalDx += change.positionChange().x
                totalDy += change.positionChange().y
                val raw = Offset(totalDx, totalDy)
                val dist = raw.getDistance()
                val capped = if (dist > maxDragPx) raw * (maxDragPx / dist) else raw
                coroutineScope.launch { dragOffset.snapTo(capped) }
                val up = -totalDy
                when {
                    up > thresholdPx && up > abs(totalDx) -> {
                        change.consume()
                        currentOnSwipeUp.value()
                        resolved = true
                    }
                    abs(totalDx) > thresholdPx && abs(totalDx) > abs(totalDy) -> {
                        change.consume()
                        currentOnCategorySearch.value()
                        resolved = true
                    }
                }
            }
            coroutineScope.launch { pressScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) }
            coroutineScope.launch { dragOffset.animateTo(Offset.Zero, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) }
            if (resolved) {
                waitForUpOrCancellation()
            } else if (totalDx * totalDx + totalDy * totalDy < thresholdPx * thresholdPx) {
                currentOnTap.value()
            }
        }
    }
}

/**
 * Same single-vs-double-tap distinction Compose's own `combinedClickable` does, but with a much
 * shorter window to decide "no second tap is coming" (150ms here vs. the system's ~300ms
 * doubleTapTimeoutMillis that combinedClickable uses internally). That system default is what made a
 * plain single tap feel noticeably delayed before doing anything, since it always had to wait it out
 * first even when no double tap was going to follow.
 */
@Composable
internal fun Modifier.quickTapGestures(onTap: () -> Unit, onDoubleTap: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val currentOnTap = rememberUpdatedState(onTap)
    val currentOnDoubleTap = rememberUpdatedState(onDoubleTap)
    return this
        .indication(interactionSource, LocalIndication.current)
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown()
                val press = PressInteraction.Press(down.position)
                interactionSource.tryEmit(press)
                val up = waitForUpOrCancellation()
                interactionSource.tryEmit(if (up != null) PressInteraction.Release(press) else PressInteraction.Cancel(press))
                if (up == null) return@awaitEachGesture

                val secondDown = withTimeoutOrNull(QUICK_DOUBLE_TAP_WINDOW_MS) { awaitFirstDown() }
                if (secondDown == null) {
                    currentOnTap.value()
                    return@awaitEachGesture
                }
                val secondPress = PressInteraction.Press(secondDown.position)
                interactionSource.tryEmit(secondPress)
                val secondUp = waitForUpOrCancellation()
                interactionSource.tryEmit(if (secondUp != null) PressInteraction.Release(secondPress) else PressInteraction.Cancel(secondPress))
                if (secondUp != null) currentOnDoubleTap.value()
            }
        }
}
