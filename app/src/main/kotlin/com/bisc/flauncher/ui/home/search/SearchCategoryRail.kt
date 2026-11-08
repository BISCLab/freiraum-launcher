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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.WindowInsets
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
private const val MAGNIFY = 0.85f

/**
 * The categories of the Text search as a vertical rail beside the results, like the App list's
 * A-Z rail. A tap jumps to that category; dragging along the rail runs through the categories like
 * a magnifier: the symbol under the finger and its neighbours grow and light up, a ring in the
 * category's color follows the finger, and a bubble above the thumb names the category. Categories
 * without results right now are faint.
 * [focus] is the one tapped last; the others are greyed out until the results are scrolled.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
    /** The keyboard comes up on its own when the search opens. */
    expectKeyboard: Boolean = false,
    /** How much height the open keyboard took from the rail's area last time (px); 0 = not known yet. */
    keyboardOverlapPx: Int = 0,
    /** Called with that height once the keyboard is fully up, when it differs. */
    onKeyboardOverlap: (Int) -> Unit = {}
) {
    if (categories.isEmpty()) return
    // The gesture below lives as long as the categories do; it must always call the current
    // actions, which know what is picked right now.
    val currentOnTap by androidx.compose.runtime.rememberUpdatedState(onTap)
    val currentOnScrub by androidx.compose.runtime.rememberUpdatedState(onScrub)
    val colors = resolveElementColors(appearance)
    val radius = SEARCH_RAIL_WIDTH / 2 * (cornerRadiusPercent.coerceIn(0, 100) / 100f)
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
    // The keyboard's final height is known from the start of its animation; until it starts, the
    // height it took last time stands in, so the rail is in place right away instead of chasing
    // the keyboard frame by frame.
    val imeNow = WindowInsets.ime.getBottom(density)
    val imeTarget = WindowInsets.imeAnimationTarget.getBottom(density)
    var keyboardSeen by remember { mutableStateOf(false) }
    if (imeTarget > 0) keyboardSeen = true
    androidx.compose.runtime.LaunchedEffect(Unit) {
        // A keyboard that was expected but never came does not hold the rail up for long.
        kotlinx.coroutines.delay(900)
        keyboardSeen = true
    }
    val keyboardMode = imeTarget > 0 || (expectKeyboard && !keyboardSeen)

    BoxWithConstraints(modifier = modifier.fillMaxHeight().width(SEARCH_RAIL_WIDTH), contentAlignment = Alignment.TopCenter) {
        val areaPx = constraints.maxHeight.toFloat()
        val gapPx = with(density) { edgeGap.toPx() }
        // The area's height without the keyboard, kept while the keyboard is up.
        var restPx by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
        if (imeNow == 0 && imeTarget == 0) restPx = areaPx
        else if (restPx == 0f) restPx = areaPx + keyboardOverlapPx
        // Once the keyboard is fully up, how much it really takes (navigation bar, the strip on top
        // of it and all) is remembered for the next time.
        val measuredOverlap = (restPx - areaPx).toInt()
        androidx.compose.runtime.LaunchedEffect(imeNow == imeTarget && imeTarget > 0, measuredOverlap) {
            if (imeNow == imeTarget && imeTarget > 0 && measuredOverlap > 0 && kotlin.math.abs(measuredOverlap - keyboardOverlapPx) > 2) {
                onKeyboardOverlap(measuredOverlap)
            }
        }
        val overlap = when {
            imeNow == imeTarget && imeTarget > 0 -> restPx - areaPx
            keyboardOverlapPx > 0 -> keyboardOverlapPx.toFloat()
            else -> imeTarget - with(density) { WindowInsets.navigationBars.getBottom(density).toFloat() }
        }.coerceAtLeast(0f)
        val naturalPx = with(density) { (RAIL_SLOT * categories.size).toPx() }
        // With the keyboard: from the field down to it, top level with the first card. Without: its
        // own height in the middle.
        val targetHeight = if (keyboardMode) (restPx - overlap - gapPx * 2) else minOf(naturalPx, restPx - gapPx * 2)
        val targetTop = if (keyboardMode) gapPx else gapPx + (restPx - gapPx * 2 - targetHeight) / 2
        val top = remember { androidx.compose.animation.core.Animatable(targetTop) }
        val height = remember { androidx.compose.animation.core.Animatable(targetHeight.coerceAtLeast(0f)) }
        androidx.compose.runtime.LaunchedEffect(targetTop, targetHeight) {
            val spec = androidx.compose.animation.core.tween<Float>(300, easing = com.bisc.flauncher.ui.EmphasizedEasing)
            launch { top.animateTo(targetTop, spec) }
            launch { height.animateTo(targetHeight.coerceAtLeast(0f), spec) }
        }
        val railTopPx = top.value
        Column(
            modifier = Modifier
                .offset { IntOffset(0, top.value.roundToInt()) }
                .width(SEARCH_RAIL_WIDTH)
                // Anchored at the top even while it is briefly taller than the area around it.
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .requiredHeight(with(density) { height.value.toDp() })
                .designGlow(shape, "rail")
                // No clipping: the magnified symbols may reach out of the rail.
                .background(colors.background, shape, colors.backgroundAlpha)
                .then(if (appearance.borderEnabled) Modifier.border(colors.borderWidth, colors.borderColor, shape) else Modifier)
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
                                scrubY = top.value + change.position.y.coerceIn(0f, size.height.toFloat())
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
            val pullPx = with(LocalDensity.current) { 10.dp.toPx() } * (if (left) 1f else -1f)
            categories.forEachIndexed { index, category ->
                val color = colorOf(category)
                val chosen = category == (scrubbing ?: focus)
                val faded = (focus != null && !chosen) || category !in present
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { centers[index] = top.value + it.positionInParent().y + it.size.height / 2f }
                        .graphicsLayer {
                            // 1 right under the finger, falling off over about two symbols each way.
                            val near = (1f - kotlin.math.abs(scrubY - centers[index]) / (slotPx * 2.2f)).coerceIn(0f, 1f) * lens
                            val eased = near * near * (3f - 2f * near)
                            val scale = 1f + MAGNIFY * eased
                            scaleX = scale
                            scaleY = scale
                            // Leans a little towards the results, out of the thumb's shadow.
                            translationX = pullPx * eased
                            // Faint ones stay readable; the magnifier brings them to full.
                            alpha = if (faded) 0.55f + 0.45f * eased else 1f
                        }
                        .size(26.dp)
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
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
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
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
    }
}
