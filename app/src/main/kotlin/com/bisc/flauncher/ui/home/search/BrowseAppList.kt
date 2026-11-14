package com.bisc.flauncher.ui.home.search

import com.bisc.flauncher.ui.home.notificationDot
import androidx.compose.runtime.collectAsState
import com.bisc.flauncher.ui.theme.designGlow
import android.content.ComponentName
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.bisc.flauncher.ui.home.rememberAppIcon
import kotlinx.coroutines.Job
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.data.model.AppInfo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.theme.noRippleClickable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val INDEX_TOUCH_WIDTH = 72.dp

internal val FADE_TRANSITION_DISTANCE = 130.dp

internal fun Modifier.fadeBackgroundToBottom(startFraction: Float, endFraction: Float): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val start = startFraction.coerceIn(0f, 1f)
        val end = endFraction.coerceIn(start, 1f)
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                start to Color.Transparent,
                end to Color.Black,
                1f to Color.Black
            ),
            blendMode = BlendMode.DstOut
        )
    }

internal fun Modifier.fadeListTop(height: Dp, strength: () -> Float): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val s = strength()
        if (s > 0f) {
            val h = height.toPx().coerceAtMost(size.height / 2f)
            drawRect(
                brush = Brush.verticalGradient(0f to Color.Black.copy(alpha = s), 1f to Color.Transparent, startY = 0f, endY = h),
                size = androidx.compose.ui.geometry.Size(size.width, h),
                blendMode = BlendMode.DstOut
            )
        }
    }

internal val TOP_FADE_HEIGHT = 56.dp

/** How wide one app should at least be in the list; phones keep one column. */
private val BROWSE_COLUMN_MIN_WIDTH = 280.dp

/** Apps side by side on wide screens, as many as fit next to the A-Z rail (at most 4). */
internal fun browseColumns(width: Dp, withRail: Boolean): Int =
    ((width - if (withRail) INDEX_TOUCH_WIDTH else 0.dp) / BROWSE_COLUMN_MIN_WIDTH).toInt().coerceIn(1, 4)

/** Gap above a letter header - also part of the first header's distance from the top. */
private val HEADER_TOP_PADDING = 22.dp

/** The list's own offset below the status bar (HomeScreen's app-list padding). */
private val APP_LIST_TOP_OFFSET = 16.dp

/** App list grouped by initial letter with sticky headers, A-Z rail and scrub magnifier. */
@Composable
fun BrowseAppList(
    allApps: List<AppInfo>,
    grouping: BrowseGrouping,
    showAzIndex: Boolean,
    headerLetterAppearance: ElementAppearance,
    listState: LazyListState,
    loadIcon: (ComponentName) -> ImageBitmap?,
    onAppClick: (AppInfo) -> Unit,
    coroutineScope: CoroutineScope,
    reservedZoneHeight: Dp,
    handedness: Handedness,
    accentColor: Int,
    edgeInset: Dp = AppOverviewDimens.horizontalMargin,
    iconSize: Dp = APP_ICON_SIZE,
    cornerRadiusPercent: Int = com.bisc.flauncher.ui.theme.DEFAULT_CORNER_RADIUS_PERCENT,
    profiles: ProfileListHost? = null,
    profileQuery: String = "",
    columns: Int = 1
) {
    val scrub = remember { BrowseScrubState() }
    val profileSections = profiles?.sections?.collectAsState()?.value.orEmpty()
    val haptic = LocalHapticFeedback.current

    val highlight = remember { mutableStateOf(IntRange.EMPTY) }
    val othersAlpha = animateFloatAsState(
        targetValue = if (scrub.active) SCRUB_OTHERS_ALPHA else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "scrubOthersAlpha"
    )
    val lastTarget = remember { intArrayOf(-1) }
    val scrollJob = remember { arrayOfNulls<Job>(1) }

    fun sectionRange(start: Int): IntRange {
        val next = (start + 1 until grouping.items.size).firstOrNull { grouping.items[it].let { item -> item is BrowseItem.Header || (item is BrowseItem.Line && item.letter != null) } }
        return start until (next ?: grouping.items.size)
    }

    val surfaceIsDark = LocalContentColor.current.luminance() > 0.5f
    val railAppearance = LocalRailAppearance.current
    val railColors = if (railAppearance == null) automaticRailColors(surfaceIsDark) else customRailColors(railAppearance, surfaceIsDark)
    CompositionLocalProvider(LocalRailColors provides railColors) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // The A–Z rail stops above the reserved zone (so it never runs into the search button); the
        // list itself runs down to the same distance from the bottom edge as the first letter header
        // ("A") sits from the top, and fades out there. The reserved zone stays as bottom scroll room,
        // so the last apps can still be scrolled up above the search button.
        val listHeight = (maxHeight - reservedZoneHeight).coerceAtLeast(0.dp)
        val bottomGap = maxOf(
            WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
            WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
        ) + APP_LIST_TOP_OFFSET + HEADER_TOP_PADDING
        val visibleHeight = (maxHeight - bottomGap).coerceAtLeast(1.dp)

        val fadeEnd = 1f
        val fadeStart = ((visibleHeight - FADE_TRANSITION_DISTANCE) / visibleHeight).coerceIn(0f, fadeEnd)

        val canScrollFurther by remember {
            derivedStateOf { listState.canScrollForward }
        }
        val topFadePx = with(LocalDensity.current) { TOP_FADE_HEIGHT.toPx() }
        // With several columns the last one keeps clear of the A-Z rail.
        val railRoom = if (columns > 1 && showAzIndex) INDEX_TOUCH_WIDTH - edgeInset else 0.dp
        val listModifier = Modifier
            .fillMaxSize()
            .padding(
                start = if (handedness == Handedness.LEFT) railRoom else 0.dp,
                end = if (handedness == Handedness.LEFT) 0.dp else railRoom
            )
            .padding(bottom = bottomGap)
            .then(if (canScrollFurther) Modifier.fadeBackgroundToBottom(fadeStart, fadeEnd) else Modifier)
            .fadeListTop(TOP_FADE_HEIGHT) {
                if (listState.firstVisibleItemIndex > 0) 1f else (listState.firstVisibleItemScrollOffset / topFadePx).coerceIn(0f, 1f)
            }

        LazyColumn(
            state = listState,
            modifier = listModifier,
            contentPadding = PaddingValues(bottom = (reservedZoneHeight - bottomGap).coerceAtLeast(0.dp))
        ) {
            itemsIndexed(
                items = grouping.items,
                key = { _, item -> browseItemKey(item) },
                contentType = { _, item -> item::class }
            ) { index, item ->
                Box(modifier = Modifier.graphicsLayer { alpha = if (index in highlight.value) 1f else othersAlpha.value }) {
                    when (item) {
                        is BrowseItem.Header -> BrowseSectionHeader(item.letter, headerLetterAppearance, handedness, edgeInset)
                        is BrowseItem.Row -> AppBrowseRow(item.app, loadIcon, handedness, edgeInset, iconSize) { onAppClick(item.app) }
                        is BrowseItem.Line -> AppBrowseLine(item.apps, columns, loadIcon, handedness, edgeInset, iconSize, onAppClick, item.letter, headerLetterAppearance, index == 0)
                    }
                }
            }
            if (profiles != null) profileSectionItems(profileSections, profileQuery, profiles, loadIcon, onAppClick, handedness, edgeInset, iconSize, columns)
        }

        if (showAzIndex && allApps.isNotEmpty()) {
            AlphabetIndex(
                letters = ('A'..'Z').toList(),
                hasOtherGroup = grouping.hasOther,
                handedness = handedness,
                cornerRadiusPercent = cornerRadiusPercent,
                modifier = Modifier
                    .align(if (handedness == Handedness.LEFT) Alignment.TopStart else Alignment.TopEnd)
                    .width(INDEX_TOUCH_WIDTH)
                    .height(listHeight),
                edgeInset = edgeInset,
                onSelect = { entry ->
                    val target = when (entry) {
                        is IndexEntry.Letter -> grouping.nearestIndexOf(entry.char)
                        IndexEntry.Other -> grouping.indexOf('#')
                    }
                    if (target != null && (target != lastTarget[0] || !scrub.active)) {
                        lastTarget[0] = target
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scrollJob[0]?.cancel()
                        scrollJob[0] = coroutineScope.launch {
                            listState.scrollToItem(target)
                            highlight.value = sectionRange(target)
                        }
                    }
                },
                onScrub = { active, label, fraction, pull ->
                    scrub.update(active, label, fraction, pull)
                    if (!active) lastTarget[0] = -1
                }
            )
        }

        ScrubOverlay(scrub = scrub, listHeight = listHeight, handedness = handedness, accentColor = accentColor, edgeInset = edgeInset)
    }
    }
}

/** What a finger on the A–Z rail is doing right now. */
@Stable
class BrowseScrubState {
    var active by mutableStateOf(false)
        private set
    var label by mutableStateOf("")
        private set
    var fraction by mutableFloatStateOf(0f)
        private set
    /** How far the finger has pulled off the rail into the list, in px. */
    var pull by mutableFloatStateOf(0f)
        private set

    fun update(active: Boolean, label: String, fraction: Float, pull: Float = 0f) {
        if (this.active != active) this.active = active
        // Let go: the bubble fades out where it was, with its letter, instead of sliding empty to the top.
        if (!active) return
        if (this.label != label) this.label = label
        if (this.fraction != fraction) this.fraction = fraction
        if (this.pull != pull) this.pull = pull
    }
}

@Composable
private fun ScrubOverlay(scrub: BrowseScrubState, listHeight: Dp, handedness: Handedness, accentColor: Int, edgeInset: Dp) {
    AnimatedVisibility(
        visible = scrub.active,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxWidth().height(listHeight)
    ) {
        ScrubMagnifier(label = scrub.label, verticalFraction = scrub.fraction, pull = scrub.pull, handedness = handedness, accentColor = accentColor, edgeInset = edgeInset)
    }
}

private fun browseItemKey(item: BrowseItem): String = when (item) {
    is BrowseItem.Header -> "header_${item.letter}"
    is BrowseItem.Row -> "app_" + item.app.componentName.flattenToString()
    is BrowseItem.Line -> "line_" + item.apps.first().componentName.flattenToString()
}

/** Apps side by side in equal columns; a short last line keeps its cells the same width. On the
 *  rail's far side a gutter holds the letter of the section's first line. */
@Composable
internal fun AppBrowseLine(
    apps: List<AppInfo>,
    columns: Int,
    loadIcon: (ComponentName) -> ImageBitmap?,
    handedness: Handedness,
    edgeInset: Dp,
    iconSize: Dp,
    onAppClick: (AppInfo) -> Unit,
    letter: Char? = null,
    letterAppearance: ElementAppearance? = null,
    first: Boolean = false
) {
    val isLeft = handedness == Handedness.LEFT
    val gutter = @Composable {
        Box(modifier = Modifier.width(edgeInset + HEADER_CIRCLE_SIZE).height(iconSize + 20.dp), contentAlignment = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd) {
            if (letter != null && letterAppearance != null) LetterCircle(letter, letterAppearance)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = if (letter != null && !first) 12.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (letterAppearance != null && !isLeft) gutter()
        val cells: List<AppInfo?> = if (isLeft) List(columns - apps.size) { null } + apps else apps + List(columns - apps.size) { null }
        cells.forEach { app ->
            Box(modifier = Modifier.weight(1f)) {
                if (app != null) AppBrowseRow(app, loadIcon, handedness, edgeInset, iconSize) { onAppClick(app) }
            }
        }
        if (letterAppearance != null && isLeft) gutter()
    }
}

private const val SCRUB_OTHERS_ALPHA = 0.06f

private val APP_ICON_SIZE = 56.dp

private val HEADER_CIRCLE_SIZE = 32.dp

@Composable
private fun LetterCircle(letter: Char, appearance: ElementAppearance) {
    val colors = resolveElementColors(appearance)
    val listContent = LocalContentColor.current
    val ownFill = appearance.fillMode == ElementFillMode.COLOR
    val letterTextColor = if (ownFill) colors.contentColor else listContent
    val letterBorderColor = if (ownFill || appearance.borderColor != null) colors.borderColor else listContent.copy(alpha = 0.3f)
    Box(
        modifier = Modifier
            .size(HEADER_CIRCLE_SIZE)
            .then(Modifier.designGlow(CircleShape, "buttons"))
            .clip(CircleShape)
            .background(colors.background, alpha = colors.backgroundAlpha)
            .then(if (appearance.borderEnabled) Modifier.border(colors.borderWidth, letterBorderColor, CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(letter.toString(), style = MaterialTheme.typography.titleMedium, color = letterTextColor)
    }
}

@Composable
private fun BrowseSectionHeader(letter: Char, appearance: ElementAppearance, handedness: Handedness, edgeInset: Dp) {
    val isLeft = handedness == Handedness.LEFT
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = edgeInset,
                end = edgeInset,
                // Bigger than the bottom gap on purpose - this is the break from the *previous*
                // section's last app, not from this header's own apps right below it (that gap is
                // the row's own top padding plus this header's bottom padding, unchanged).
                top = HEADER_TOP_PADDING,
                bottom = 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val circle = @Composable { LetterCircle(letter, appearance) }
        if (isLeft) {
            Box(modifier = Modifier.weight(1f))
            circle()
        } else {
            circle()
        }
    }
}

private val MAGNIFIER_GAP = 6.dp

private fun contentColorFor(color: Color): Color = if (color.luminance() > 0.5f) Color.Black else Color.White

@Composable
private fun ScrubMagnifier(label: String, verticalFraction: Float, pull: Float, handedness: Handedness, accentColor: Int, edgeInset: Dp) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val motion = com.bisc.flauncher.ui.LocalMotion.current
        val bubbleSize = 76.dp
        val bubbleSizePx = with(density) { bubbleSize.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val targetY = (verticalFraction * maxHeightPx - bubbleSizePx / 2f)
            .coerceIn(0f, (maxHeightPx - bubbleSizePx).coerceAtLeast(0f))
        val isLeft = handedness == Handedness.LEFT

        // Like a spring: pulled into the list it follows the finger, but every further bit takes more
        // effort (it approaches a limit and never reaches it). Let go, it snaps back with a little bounce.
        val maxStretchPx = with(density) { MAGNIFIER_MAX_STRETCH.toPx() }
        val stretchTarget = if (motion == com.bisc.flauncher.ui.MotionLevel.OFF) 0f
            else maxStretchPx * (1f - kotlin.math.exp(-pull / maxStretchPx))
        val springy = androidx.compose.animation.core.spring<Float>(dampingRatio = 0.5f, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
        val stretch by androidx.compose.animation.core.animateFloatAsState(stretchTarget, springy, label = "magnifierStretch")
        // The letter glides after the finger instead of jumping row by row.
        val yPx by androidx.compose.animation.core.animateFloatAsState(
            targetY,
            if (motion == com.bisc.flauncher.ui.MotionLevel.OFF) androidx.compose.animation.core.snap()
            else androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = androidx.compose.animation.core.Spring.StiffnessHigh),
            label = "magnifierY"
        )
        val tension = (stretch / maxStretchPx).coerceIn(0f, 1f)

        val railBarDistance = edgeInset + RAIL_BAR_WIDTH
        val restOffsetPx = with(density) { (railBarDistance + MAGNIFIER_GAP).toPx() }
        val bubbleColor = Color(accentColor)

        Box(
            modifier = Modifier
                .align(if (isLeft) Alignment.TopStart else Alignment.TopEnd)
                .offset {
                    androidx.compose.ui.unit.IntOffset(
                        x = (if (isLeft) restOffsetPx + stretch else -(restOffsetPx + stretch)).toInt(),
                        y = yPx.toInt()
                    )
                }
                .size(bubbleSize)
                .graphicsLayer {
                    // A little bigger under tension, but always a circle.
                    val grow = 1f + 0.12f * tension
                    scaleX = grow
                    scaleY = grow
                }
                .then(Modifier.designGlow(CircleShape, "buttons"))
                .clip(CircleShape)
                .background(bubbleColor.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            Text(label, style = MaterialTheme.typography.headlineMedium, color = contentColorFor(bubbleColor))
        }
    }
}

/** How far the letter bubble can be pulled into the list at most (it only ever approaches this). */
private val MAGNIFIER_MAX_STRETCH = 120.dp

@Composable
internal fun AppBrowseRow(app: AppInfo, loadIcon: (ComponentName) -> ImageBitmap?, handedness: Handedness, edgeInset: Dp, iconSize: Dp, onClick: () -> Unit) {
    val bitmap = rememberAppIcon(app.componentName, loadIcon)
    val isLeft = handedness == Handedness.LEFT
    val menu = com.bisc.flauncher.ui.home.rememberAppMenu(app.componentName, app.label)
    // The app's own size if it has one, otherwise the size for all apps.
    @Suppress("NAME_SHADOWING")
    val iconSize = iconSize * (com.bisc.flauncher.ui.home.LocalIconSizeResolver.current?.invoke(app.componentName.flattenToString()) ?: 1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .appClickable(onClick = onClick, onLongClick = menu?.open)
            .padding(
                start = edgeInset,
                end = edgeInset,
                top = 10.dp,
                bottom = 10.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(16.dp, if (isLeft) Alignment.End else Alignment.Start),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = @Composable {
            // The menu opens at the icon.
            Box {
                if (bitmap != null) Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(iconSize).notificationDot(app.componentName.packageName))
                menu?.Menu?.invoke()
            }
        }
        val label = @Composable { Text(app.label) }
        if (isLeft) {
            label(); icon()
        } else {
            icon(); label()
        }
    }
}

/** A tap opens the app, a long press its menu (where there is one); no ripple, like the lists. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.appClickable(onClick: () -> Unit, onLongClick: (() -> Unit)?): Modifier = combinedClickable(
    interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
    indication = null,
    onClick = onClick,
    onLongClick = onLongClick
)
