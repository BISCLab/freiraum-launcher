package com.bisc.flauncher.ui.home.search

import android.content.ComponentName
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.bisc.flauncher.data.model.AppListLayout
import com.bisc.flauncher.ui.home.rememberAppIcon
import kotlinx.coroutines.Job
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import com.bisc.flauncher.ui.home.dock.DockDimens
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

private val TOP_FADE_HEIGHT = 56.dp

/** App list grouped by initial letter with sticky headers, A-Z rail and scrub magnifier. */
@Composable
fun BrowseAppList(
    allApps: List<AppInfo>,
    grouping: BrowseGrouping,
    showAzIndex: Boolean,
    indexShowRailBar: Boolean,
    headerLetterAppearance: ElementAppearance,
    alphabetLetterAppearance: ElementAppearance,
    listState: LazyListState,
    loadIcon: (ComponentName) -> ImageBitmap?,
    onAppClick: (AppInfo) -> Unit,
    coroutineScope: CoroutineScope,
    reservedZoneHeight: Dp,
    handedness: Handedness,
    accentColor: Int,
    edgeInset: Dp = DockDimens.horizontalMargin,
    layout: AppListLayout = AppListLayout.LIST,
    iconSize: Dp = TILE_ICON_SIZE,
    cornerRadiusPercent: Int = 25
) {
    val scrub = remember { BrowseScrubState() }
    val gridState = rememberLazyGridState()
    val haptic = LocalHapticFeedback.current
    val tiles = layout == AppListLayout.TILES

    val highlight = remember { mutableStateOf(IntRange.EMPTY) }
    val othersAlpha = animateFloatAsState(
        targetValue = if (scrub.active) SCRUB_OTHERS_ALPHA else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "scrubOthersAlpha"
    )
    val lastTarget = remember { intArrayOf(-1) }
    val scrollJob = remember { arrayOfNulls<Job>(1) }

    fun sectionRange(start: Int): IntRange {
        val next = (start + 1 until grouping.items.size).firstOrNull { grouping.items[it] is BrowseItem.Header }
        return start until (next ?: grouping.items.size)
    }

    val surfaceIsDark = LocalContentColor.current.luminance() > 0.5f
    val railAppearance = LocalRailAppearance.current
    val railColors = if (railAppearance == null) automaticRailColors(surfaceIsDark) else customRailColors(railAppearance, surfaceIsDark)
    CompositionLocalProvider(LocalRailColors provides railColors) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val listHeight = (maxHeight - reservedZoneHeight).coerceAtLeast(0.dp)

        val fadeEnd = (listHeight / maxHeight).coerceIn(0f, 1f)
        val fadeStart = ((listHeight - FADE_TRANSITION_DISTANCE) / maxHeight).coerceIn(0f, fadeEnd)

        val canScrollFurther by remember(tiles) {
            derivedStateOf {
                reservedZoneHeight > 0.dp && (if (tiles) gridState.canScrollForward else listState.canScrollForward)
            }
        }
        val topFadePx = with(LocalDensity.current) { TOP_FADE_HEIGHT.toPx() }
        val listModifier = Modifier
            .fillMaxSize()
            .then(if (canScrollFurther) Modifier.fadeBackgroundToBottom(fadeStart, fadeEnd) else Modifier)
            .fadeListTop(TOP_FADE_HEIGHT) {
                val index = if (tiles) gridState.firstVisibleItemIndex else listState.firstVisibleItemIndex
                val offset = if (tiles) gridState.firstVisibleItemScrollOffset else listState.firstVisibleItemScrollOffset
                if (index > 0) 1f else (offset / topFadePx).coerceIn(0f, 1f)
            }

        if (tiles) {
            // The rail floats over the grid's own edge.
            val railSpace = edgeInset + RAIL_BAR_WIDTH + 8.dp
            val railOnLeft = handedness == Handedness.LEFT
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(TILE_MIN_WIDTH),
                modifier = listModifier,
                contentPadding = PaddingValues(
                    start = edgeInset + if (showAzIndex && railOnLeft) railSpace - edgeInset else 0.dp,
                    end = edgeInset + if (showAzIndex && !railOnLeft) railSpace - edgeInset else 0.dp,
                    bottom = reservedZoneHeight
                )
            ) {
                itemsIndexed(
                    items = grouping.items,
                    key = { _, item -> browseItemKey(item) },
                    span = { _, item ->
                        if (item is BrowseItem.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1)
                    },
                    contentType = { _, item -> item is BrowseItem.Header }
                ) { index, item ->
                    Box(modifier = Modifier.graphicsLayer { alpha = if (index in highlight.value) 1f else othersAlpha.value }) {
                        when (item) {
                            is BrowseItem.Header -> BrowseSectionHeader(item.letter, headerLetterAppearance, handedness, 0.dp)
                            is BrowseItem.Row -> AppTile(item.app, loadIcon, iconSize) { onAppClick(item.app) }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = listModifier,
                contentPadding = PaddingValues(bottom = reservedZoneHeight)
            ) {
                itemsIndexed(
                    items = grouping.items,
                    key = { _, item -> browseItemKey(item) },
                    contentType = { _, item -> item is BrowseItem.Header }
                ) { index, item ->
                    Box(modifier = Modifier.graphicsLayer { alpha = if (index in highlight.value) 1f else othersAlpha.value }) {
                        when (item) {
                            is BrowseItem.Header -> BrowseSectionHeader(item.letter, headerLetterAppearance, handedness, edgeInset)
                            is BrowseItem.Row -> AppBrowseRow(item.app, loadIcon, handedness, edgeInset, iconSize) { onAppClick(item.app) }
                        }
                    }
                }
            }
        }

        if (showAzIndex && allApps.isNotEmpty()) {
            AlphabetIndex(
                letters = ('A'..'Z').toList(),
                hasOtherGroup = grouping.hasOther,
                showRailBar = indexShowRailBar,
                letterAppearance = alphabetLetterAppearance,
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
                            if (tiles) gridState.scrollToItem(target) else listState.scrollToItem(target)
                            highlight.value = sectionRange(target)
                        }
                    }
                },
                onScrub = { active, label, fraction ->
                    scrub.update(active, label, fraction)
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

    fun update(active: Boolean, label: String, fraction: Float) {
        if (this.active != active) this.active = active
        if (this.label != label) this.label = label
        if (this.fraction != fraction) this.fraction = fraction
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
        ScrubMagnifier(label = scrub.label, verticalFraction = scrub.fraction, handedness = handedness, accentColor = accentColor, edgeInset = edgeInset)
    }
}

private fun browseItemKey(item: BrowseItem): String = when (item) {
    is BrowseItem.Header -> "header_${item.letter}"
    is BrowseItem.Row -> "app_" + item.app.componentName.flattenToString()
}

private const val SCRUB_OTHERS_ALPHA = 0.06f

private val TILE_MIN_WIDTH = 78.dp

private val TILE_ICON_SIZE = 56.dp

@Composable
private fun AppTile(app: AppInfo, loadIcon: (ComponentName) -> ImageBitmap?, iconSize: Dp, onClick: () -> Unit) {
    val bitmap = rememberAppIcon(app.componentName, loadIcon)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(iconSize)) {
            if (bitmap != null) Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
        Text(
            text = app.label,
            modifier = Modifier.padding(top = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

private val HEADER_CIRCLE_SIZE = 32.dp

@Composable
private fun BrowseSectionHeader(letter: Char, appearance: ElementAppearance, handedness: Handedness, edgeInset: Dp) {
    val isLeft = handedness == Handedness.LEFT
    val colors = resolveElementColors(appearance)
    val listContent = LocalContentColor.current
    val ownFill = appearance.fillMode == ElementFillMode.COLOR
    val letterTextColor = if (ownFill) colors.contentColor else listContent
    val letterBorderColor = if (ownFill || appearance.borderColor != null) colors.borderColor else listContent.copy(alpha = 0.3f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = edgeInset,
                end = edgeInset,
                top = 6.dp,
                bottom = 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val circle = @Composable {
            Box(
                modifier = Modifier
                    .size(HEADER_CIRCLE_SIZE)
                    .clip(CircleShape)
                    .background(colors.background, alpha = colors.backgroundAlpha)
                    .then(
                        if (appearance.borderEnabled) Modifier.border(colors.borderWidth, letterBorderColor, CircleShape) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(letter.toString(), style = MaterialTheme.typography.titleMedium, color = letterTextColor)
            }
        }
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
private fun ScrubMagnifier(label: String, verticalFraction: Float, handedness: Handedness, accentColor: Int, edgeInset: Dp) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val bubbleSize = 76.dp
        val bubbleSizePx = with(density) { bubbleSize.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val yPx = (verticalFraction * maxHeightPx - bubbleSizePx / 2f)
            .coerceIn(0f, (maxHeightPx - bubbleSizePx).coerceAtLeast(0f))
        val y = with(density) { yPx.toDp() }
        val isLeft = handedness == Handedness.LEFT

        val railBarDistance = edgeInset + RAIL_BAR_WIDTH
        val bubbleColor = Color(accentColor)
        Box(
            modifier = Modifier
                .align(if (isLeft) Alignment.TopStart else Alignment.TopEnd)
                .offset(
                    x = if (isLeft) (railBarDistance + MAGNIFIER_GAP) else -(railBarDistance + MAGNIFIER_GAP),
                    y = y
                )
                .size(bubbleSize)
                .clip(CircleShape)
                .background(bubbleColor.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            Text(label, style = MaterialTheme.typography.headlineMedium, color = contentColorFor(bubbleColor))
        }
    }
}

@Composable
private fun AppBrowseRow(app: AppInfo, loadIcon: (ComponentName) -> ImageBitmap?, handedness: Handedness, edgeInset: Dp, iconSize: Dp, onClick: () -> Unit) {
    val bitmap = rememberAppIcon(app.componentName, loadIcon)
    val isLeft = handedness == Handedness.LEFT
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable(onClick = onClick)
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
            if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(iconSize))
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
