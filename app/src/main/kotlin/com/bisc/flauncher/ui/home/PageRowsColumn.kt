package com.bisc.flauncher.ui.home

import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.geometry.Offset
import android.content.ComponentName
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import com.bisc.flauncher.data.model.AppRowEntry
import com.bisc.flauncher.data.model.Folder
import com.bisc.flauncher.data.model.FolderItem
import com.bisc.flauncher.data.model.GestureAction
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.PinnedWebsite
import com.bisc.flauncher.data.model.SavedMapTarget
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.data.settings.IconShape
import com.bisc.flauncher.ui.home.dock.QuickLaunchSlot
import com.bisc.flauncher.ui.home.dock.defaultQuickLaunchSlots

/** One page's content: [rows] (already sorted by orderIndex) stacked top to bottom in a plain scrollable [Column]. */
@Composable
fun PageRowsColumn(
    rows: List<PageRow>,
    entriesByRow: Map<Long, List<AppRowEntry>>,
    folders: List<Folder>,
    folderItemsByFolder: Map<Long, List<FolderItem>>,
    savedMapTargets: List<SavedMapTarget> = emptyList(),
    pinnedWebsites: List<PinnedWebsite> = emptyList(),
    loadIcon: (ComponentName) -> ImageBitmap?,
    loadPackIcon: (String, String) -> ImageBitmap? = { _, _ -> null },
    resolveLabel: (ComponentName) -> String,
    onFolderClick: (Long) -> Unit,
    onShortcutClick: (String) -> Unit = {},
    onExecuteAction: (GestureAction) -> Unit = {},
    handedness: Handedness = Handedness.RIGHT,
    iconShape: IconShape = IconShape.ROUNDED_SQUARE,
    onSearchBarTap: () -> Unit = {},
    onSearchBarDoubleTap: () -> Unit = {},
    quickLaunchSlots: List<QuickLaunchSlot> = defaultQuickLaunchSlots,
    loadQuickLaunchIcon: (ComponentName) -> ImageBitmap? = { null },
    onQuickLaunchClick: (ComponentName) -> Unit = {},
    // How much space to keep clear at the bottom of every virtual page for a floating dock overlay,
    // enforced only against SYSTEM_WIDGET rows (see [PagedRows]'s clip logic).
    widgetBottomReserve: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth().fillMaxHeight()) {
        val viewportHeight = maxHeight
        val scrollState = rememberScrollState()
        val density = LocalDensity.current
        val fling = remember(scrollState, density, viewportHeight) {
            PageSnapFling(scrollState) { with(density) { viewportHeight.roundToPx() } }
        }
        Column(
            modifier = Modifier.fillMaxSize().nestedScroll(fling.connection).verticalScroll(scrollState, flingBehavior = fling)
        ) {
            val clippableToPage = remember(rows) { rows.map { it.type == PageRowType.SYSTEM_WIDGET } }
            PagedRows(
                viewportHeight = viewportHeight,
                spacing = PageRowDimens.rowSpacing,
                clippableToPage = clippableToPage,
                bottomReserve = widgetBottomReserve
            ) {
                rows.forEach { row ->
                    key(row.id) {
                        Box {
                            PageRowContent(
                    row = row,
                    entries = entriesByRow[row.id].orEmpty(),
                    folders = folders,
                    folderItemsByFolder = folderItemsByFolder,
                    savedMapTargets = savedMapTargets,
                    pinnedWebsites = pinnedWebsites,
                    loadIcon = loadIcon,
                    loadPackIcon = loadPackIcon,
                    resolveLabel = resolveLabel,
                    onFolderClick = onFolderClick,
                    onShortcutClick = onShortcutClick,
                    onExecuteAction = onExecuteAction,
                    handedness = handedness,
                    iconShape = iconShape,
                    onSearchBarTap = onSearchBarTap,
                    onSearchBarDoubleTap = onSearchBarDoubleTap,
                    quickLaunchSlots = quickLaunchSlots,
                    loadQuickLaunchIcon = loadQuickLaunchIcon,
                    onQuickLaunchClick = onQuickLaunchClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PagedRows(viewportHeight: Dp, spacing: Dp, clippableToPage: List<Boolean>, bottomReserve: Dp, content: @Composable () -> Unit) {
    Layout(content = content, modifier = Modifier.fillMaxWidth()) { measurables, constraints ->
        val pageHeight = viewportHeight.roundToPx()
        val gap = spacing.roundToPx()
        val reserve = bottomReserve.roundToPx()
        val childConstraints = constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
        val placeables = measurables.map { it.measure(childConstraints) }.toMutableList()

        var page = 0
        var usedOnPage = 0
        val tops = IntArray(placeables.size)
        for (index in placeables.indices) {
            var placeable = placeables[index]
            if (usedOnPage > 0 && usedOnPage + gap + placeable.height > pageHeight) {
                page++
                usedOnPage = 0
            }
            val top = if (usedOnPage == 0) 0 else usedOnPage + gap
            // A system widget that would spill past this page's bottom (e.g. at its tallest setting)
            // is cut off right there instead of overflowing past the dock's reserved space below it —
            // this is the hard safety net that stops it from ever colliding with a floating dock.
            val isWidget = clippableToPage.getOrElse(index) { false }
            val remaining = (pageHeight - top - (if (isWidget) reserve else 0)).coerceAtLeast(0)
            if (isWidget && placeable.height > remaining) {
                placeable = measurables[index].measure(constraints.copy(minHeight = 0, maxHeight = remaining))
                placeables[index] = placeable
            }
            tops[index] = page * pageHeight + top
            usedOnPage = top + placeable.height
            if (usedOnPage > pageHeight) {
                val extraPages = (usedOnPage - 1) / pageHeight
                page += extraPages
                usedOnPage -= extraPages * pageHeight
            }
        }
        val totalHeight = (page + 1) * pageHeight
        layout(constraints.maxWidth, totalHeight) {
            placeables.forEachIndexed { index, placeable -> placeable.placeRelative(0, tops[index]) }
        }
    }
}

/**
 * Snaps a vertical release to page boundaries. The direction of the gesture decides: a release moved past a small
 * distance, or with a light flick, advances one page; only an almost stationary release returns to the nearest page.
 * [connection] records where the drag started and must sit above the scrollable.
 */
private class PageSnapFling(
    private val state: ScrollState,
    private val pageHeightPx: () -> Int
) : FlingBehavior {
    private var dragStart = -1

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput && dragStart < 0) dragStart = state.value
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            dragStart = -1
            return Velocity.Zero
        }
    }

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val pageHeight = pageHeightPx()
        val origin = if (dragStart >= 0) dragStart else state.value
        dragStart = -1
        if (pageHeight <= 0 || state.maxValue <= 0) return initialVelocity
        val start = state.value
        val page = start.toFloat() / pageHeight
        val moved = start - origin
        val distanceThreshold = pageHeight * DRAG_THRESHOLD_FRACTION
        val forward = when {
            initialVelocity < -FLING_THRESHOLD -> true
            initialVelocity > FLING_THRESHOLD -> false
            moved > distanceThreshold -> true
            moved < -distanceThreshold -> false
            else -> null
        }
        val targetPage = when (forward) {
            true -> ceil(page)
            false -> floor(page)
            null -> page.roundToInt().toFloat()
        }
        val target = (targetPage * pageHeight).roundToInt().coerceIn(0, state.maxValue)
        var last = start.toFloat()
        animate(start.toFloat(), target.toFloat(), animationSpec = tween(250)) { value, _ ->
            scrollBy(value - last)
            last = value
        }
        return 0f
    }

    private companion object {
        const val FLING_THRESHOLD = 200f
        const val DRAG_THRESHOLD_FRACTION = 0.05f
    }
}
