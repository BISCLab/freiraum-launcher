package com.bisc.flauncher.ui.home.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.wrapContentHeight
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.SearchCategory
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.theme.designGlow
import kotlin.math.roundToInt

/** The rail is as slim as the App list's A-Z rail. */
val SEARCH_RAIL_WIDTH = 28.dp
/** Between the rail and the result cards. */
val SEARCH_RAIL_GAP = 10.dp
private val RAIL_SLOT = 40.dp
private val BUBBLE_HEIGHT = 52.dp
/** How far the bubble sits above the finger, so the thumb never covers it. */
private val BUBBLE_LIFT = 72.dp
/** How much the symbol under the finger grows; its neighbours grow less. */
private const val MAGNIFY = 0.45f

/**
 * The categories of the Text search as a vertical rail beside the results, like the App list's
 * A-Z rail. A tap jumps to that category; dragging along the rail runs through the categories like
 * a magnifier: the symbol under the finger and its neighbours grow in place on a ground in the
 * bubble's look, a ring in the category's color follows the finger, and a bubble above the thumb
 * names the category. Categories
 * without results right now are faint.
 * [focus] is the one tapped last; the others are greyed out until the results are scrolled.
 */
@Composable
fun SearchCategoryRail(
    categories: List<SearchCategory>,
    categoryColors: Map<SearchCategory, Int?>,
    present: Set<SearchCategory>,
    focus: SearchCategory?,
    left: Boolean,
    appearance: ElementAppearance,
    cornerRadiusPercent: Int,
    onTap: (SearchCategory) -> Unit,
    onScrub: (SearchCategory) -> Unit,
    modifier: Modifier = Modifier,
    /** Between the rail and the field above, and the keyboard below. */
    edgeGap: androidx.compose.ui.unit.Dp = 0.dp,
    /** Where the keyboard will end, shared with the strip over it; null = no keyboard to mind. */
    room: KeyboardRoom? = null,
    /** The bubble and the ground under magnified symbols. */
    bubbleAppearance: ElementAppearance = ElementAppearance()
) {
    if (categories.isEmpty()) return
    // The gesture below lives as long as the categories do; it must always call the current
    // actions, which know what is picked right now.
    val currentOnTap by androidx.compose.runtime.rememberUpdatedState(onTap)
    val currentOnScrub by androidx.compose.runtime.rememberUpdatedState(onScrub)
    val colors = resolveElementColors(appearance)
    val bubbleColors = resolveElementColors(bubbleAppearance)
    val radius = com.bisc.flauncher.ui.theme.partCornerRadius(cornerRadiusPercent, SEARCH_RAIL_WIDTH)
    val shape = RoundedCornerShape(radius)
    // On a dark rail the category colors are lifted towards white, so darker ones (blue, violet)
    // still stand out; on a light rail they stay as they are.
    val darkRail = colors.contentColor.luminance() > 0.5f
    fun colorOf(category: SearchCategory): Color {
        val base = categoryColors[category]?.let { Color(it) } ?: category.defaultColor
        return if (darkRail) androidx.compose.ui.graphics.lerp(base, Color.White, 0.35f) else base
    }
    var scrubbing by remember { mutableStateOf<SearchCategory?>(null) }
    var scrubY by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    // Fades the magnifier in and out instead of switching it.
    val lens by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (scrubbing != null) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(if (scrubbing != null) 140 else 220),
        label = "railLens"
    )
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    // Each symbol's middle, from the top of the rail box, for the magnifier.
    val centers = remember(categories) { FloatArray(categories.size) }

    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxHeight().width(SEARCH_RAIL_WIDTH), contentAlignment = Alignment.TopCenter) {
        val areaPx = constraints.maxHeight.toFloat()
        val gapPx = with(density) { edgeGap.toPx() }
        val keyboardMode = room?.keyboardMode == true
        val restPx = room?.restPx?.takeIf { it > 0f } ?: areaPx
        val following = room?.following == true
        val bottomPx = room?.bottomPx ?: restPx
        val naturalPx = with(density) { (RAIL_SLOT * categories.size).toPx() }
        // With the keyboard (and while it closes): from the field down to it, top level with the
        // first card. Without: its own height in the middle.
        val stretched = keyboardMode || following
        val targetHeight = (if (stretched) (bottomPx - gapPx * 2) else minOf(naturalPx, restPx - gapPx * 2)).coerceAtLeast(0f)
        val targetTop = if (stretched) gapPx else gapPx + (restPx - gapPx * 2 - targetHeight) / 2
        val top = remember { androidx.compose.animation.core.Animatable(targetTop) }
        val height = remember { androidx.compose.animation.core.Animatable(targetHeight) }
        androidx.compose.runtime.LaunchedEffect(targetTop, targetHeight, following) {
            if (following) {
                // Rides down with the closing keyboard frame by frame, no motion of its own.
                top.snapTo(targetTop)
                height.snapTo(targetHeight)
            } else {
                val spec = androidx.compose.animation.core.tween<Float>(300, easing = com.bisc.flauncher.ui.EmphasizedEasing)
                launch { top.animateTo(targetTop, spec) }
                launch { height.animateTo(targetHeight, spec) }
            }
        }
        val followTop = androidx.compose.runtime.rememberUpdatedState(if (following) targetTop else null)
        val followHeight = if (following) targetHeight else null
        fun shownTop() = followTop.value ?: top.value
        Column(
            modifier = Modifier
                .offset { IntOffset(0, shownTop().roundToInt()) }
                .width(SEARCH_RAIL_WIDTH)
                // Anchored at the top even while it is briefly taller than the area around it.
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .requiredHeight(with(density) { (followHeight ?: height.value).toDp() })
                .designGlow(shape, "rail")
                // No clipping: the magnified symbols may reach out of the rail.
                .background(colors.background, shape, colors.backgroundAlpha)
                .then(if (colors.showBorder) Modifier.border(colors.borderWidth, colors.borderColor, shape) else Modifier)
                .pointerInput(categories) {
                    fun at(y: Float) = categories[((y / size.height) * categories.size).toInt().coerceIn(0, categories.lastIndex)]
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        var moved = false
                        var last = at(down.position.y)
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            if (!moved && kotlin.math.abs(change.position.y - down.position.y) > viewConfiguration.touchSlop) moved = true
                            if (moved) {
                                val now = at(change.position.y)
                                scrubY = shownTop() + change.position.y.coerceIn(0f, size.height.toFloat())
                                if (now != last || scrubbing == null) {
                                    if (now != last) haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    last = now
                                    scrubbing = now
                                    currentOnScrub(now)
                                }
                            }
                        }
                        if (!moved) currentOnTap(last)
                        scrubbing = null
                    }
                },
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val slotPx = with(LocalDensity.current) { RAIL_SLOT.toPx() }
            categories.forEachIndexed { index, category ->
                val color = colorOf(category)
                val chosen = category == (scrubbing ?: focus)
                val faded = (focus != null && !chosen) || category !in present
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { centers[index] = shownTop() + it.positionInParent().y + it.size.height / 2f }
                        .graphicsLayer {
                            // Grows in place, centred on the rail; 1 right under the finger, falling
                            // off over about two symbols each way.
                            val eased = magnified(scrubY, centers[index], slotPx, lens)
                            val scale = 1f + MAGNIFY * eased
                            scaleX = scale
                            scaleY = scale
                            // Faint ones stay readable; the magnifier brings them to full.
                            alpha = if (faded) 0.55f + 0.45f * eased else 1f
                        }
                        .size(26.dp)
                        // A solid ground in the bubble's look, so a magnified symbol never floats.
                        .drawBehind {
                            val eased = magnified(scrubY, centers[index], slotPx, lens)
                            if (eased > 0.01f) drawCircle(bubbleColors.background, alpha = eased * bubbleColors.backgroundAlpha.coerceAtLeast(0.85f))
                        }
                        .clip(CircleShape)
                        .background(color.copy(alpha = if (chosen) 0.24f else 0f))
                        .then(if (chosen && scrubbing != null) Modifier.border(1.5.dp, color.copy(alpha = 0.7f), CircleShape) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        category.shownIcon(),
                        contentDescription = stringResource(category.filterLabelRes),
                        tint = color,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
        // While dragging, the category under the finger is named in a bubble above the thumb.
        scrubbing?.let { category ->
            val density = LocalDensity.current
            val bubblePx = with(density) { BUBBLE_HEIGHT.toPx() }
            val liftPx = with(density) { BUBBLE_LIFT.toPx() }
            val gapPx = with(density) { (SEARCH_RAIL_WIDTH + 20.dp).toPx() }
            val color = colorOf(category)
            Row(
                modifier = Modifier
                    .align(if (left) Alignment.TopStart else Alignment.TopEnd)
                    .offset { IntOffset(if (left) gapPx.roundToInt() else -gapPx.roundToInt(), (scrubY - liftPx - bubblePx / 2).coerceAtLeast(0f).roundToInt()) }
                    .wrapContentWidth(unbounded = true, align = if (left) Alignment.Start else Alignment.End)
                    .height(BUBBLE_HEIGHT)
                    .shadow(8.dp, CircleShape)
                    .designGlow(CircleShape, "buttons")
                    .clip(CircleShape)
                    .background(bubbleColors.background, CircleShape, bubbleColors.backgroundAlpha)
                    .then(if (bubbleColors.showBorder) Modifier.border(bubbleColors.borderWidth, bubbleColors.borderColor, CircleShape) else Modifier)
                    .padding(start = 8.dp, end = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) { Icon(category.shownIcon(), contentDescription = null, tint = color, modifier = Modifier.size(22.dp)) }
                Text(
                    stringResource(category.filterLabelRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = bubbleColors.contentColor,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
    }
}

/** How far the magnifier reaches a symbol whose middle is at [center]: 1 under the finger at [fingerY]. */
private fun magnified(fingerY: Float, center: Float, slotPx: Float, lens: Float): Float {
    val near = (1f - kotlin.math.abs(fingerY - center) / (slotPx * 2.2f)).coerceIn(0f, 1f) * lens
    return near * near * (3f - 2f * near)
}
