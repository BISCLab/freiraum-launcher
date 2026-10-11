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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.data.settings.IconShape

/**
 * Dock: a pill of fixed-size quick-launch icons (see [DockDimens] — sizes are constant across screens,
 * only the pill's width and icon spacing flex to fill the available width, the same responsive approach
 * iOS's own dock uses) with the Hub orb nested into it, centered. The pill's own silhouette rises into a
 * shallow, smooth hill on both its top and bottom edge around the orb — drawn as a RoundRect unioned with
 * a wide, short ellipse (not a raw circle, and not hand-tuned bezier control points — both turned out
 * fragile; a wide ellipse's own curvature gives a reliably soft, continuous bump with no manual tuning).
 * The pill follows [appearance] for its own fill/border and [fullyRounded] for how rounded its own ends
 * are (fully round "stadium" ends by default, or a smaller fixed radius for visible-but-soft corners),
 * same per-row customization every other dock-style row already has. The orb's own art is drawn by
 * [DockOrbArt] — currently always [DockOrbDesign.DEFAULT]; the type stays pluggable (other designs exist
 * in [DockOrbDesigns], just not exposed as a picker right now — see that file's doc comment) for later.
 * Tap the orb to open the expert (full text) search; drag up to open the app list; drag either left or
 * right to open the category search (the sidebar overview panel) — [handedness] no longer picks a side
 * for this, both directions do the same thing for now, kept as a parameter for a future "right drag does
 * something else" assignment.
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
    fullyRounded: Boolean = true,
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

        Box(modifier = Modifier.width(d.pillWidth).height(d.barHeight + 64.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barHeightPx = d.barHeight.toPx()
                val barTop = (size.height - barHeightPx) / 2f
                val cornerPx = if (fullyRounded) barHeightPx / 2f else 20.dp.toPx()
                val hubCenter = Offset(size.width / 2f + dragOffset.value.x, size.height / 2f)
                val cr = CornerRadius(cornerPx)
                val barPath = Path().apply {
                    addRoundRect(RoundRect(Rect(0f, barTop, size.width, barTop + barHeightPx), cr, cr, cr, cr))
                }
                // A slightly wide ellipse rather than a plain circle: just enough wider than it is tall
                // that the union reads as a soft, localized dome around the orb instead of a ball with a
                // sharp seam — but still a contained bump, not wide enough to flatten into the pill's own
                // silhouette (an easy mistake: too wide and the "hill" swallows the whole bar instead of
                // sitting on it).
                val hillRadiusY = (barHeightPx / 2f + 15.dp.toPx()) * pressScale.value
                val hillRadiusX = hillRadiusY * 1.18f
                val hillPath = Path().apply {
                    addOval(
                        Rect(
                            left = hubCenter.x - hillRadiusX,
                            top = hubCenter.y - hillRadiusY,
                            right = hubCenter.x + hillRadiusX,
                            bottom = hubCenter.y + hillRadiusY
                        )
                    )
                }
                val pillPath = Path().apply { op(barPath, hillPath, PathOperation.Union) }

                drawPath(pillPath, brush = colors.background, alpha = colors.backgroundAlpha)
                if (appearance.borderEnabled) {
                    drawPath(pillPath, color = colors.borderColor, style = Stroke(width = colors.borderWidth.toPx()))
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(d.pillWidth)
                    .padding(horizontal = d.innerHorizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(d.itemSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leftSlots.forEach { slot -> QuickLaunchIcon(slot) }
                Spacer(modifier = Modifier.width(d.hubSize))
                rightSlots.forEach { slot -> QuickLaunchIcon(slot) }
            }
            // The orb's own art, independent of the pill's appearance — see [DockOrbArt]. Drag feedback
            // (tracking the finger, a press-dip) is applied as a graphics layer on top of it.
            DockOrbArt(
                design = DockOrbDesign.DEFAULT,
                size = d.hubSize,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(dragOffset.value.x.roundToInt(), dragOffset.value.y.roundToInt()) }
                    .graphicsLayer { scaleX = pressScale.value; scaleY = pressScale.value }
            )
            // Invisible hit-target matching the orb's drawn position exactly. No icon on it by design — the
            // orb reads as one mysterious, multi-purpose control, not a labeled search button — but it
            // keeps its accessibility description.
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
