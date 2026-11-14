package com.bisc.flauncher.ui.home

import androidx.compose.ui.unit.dp
import android.content.ComponentName
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.bisc.flauncher.data.model.RowEntry
import com.bisc.flauncher.data.model.Folder
import com.bisc.flauncher.data.model.FolderItem
import com.bisc.flauncher.data.model.GestureAction
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PinnedWebsite
import com.bisc.flauncher.data.model.SavedMapTarget
import com.bisc.flauncher.data.settings.IconShape

/**
 * One page's content: [rows] (already sorted by orderIndex) stacked top to bottom, no scrolling - a
 * page is exactly one screen. Whatever doesn't fit is moved to a fresh page at the data layer instead
 * (see [com.bisc.flauncher.data.repository.PageContentRepository.rebalancePageOverflow]), so rows here
 * are expected to always fit; this is just a plain stacking layout, no page-snap or clipping tricks.
 */
@Composable
fun PageRowsColumn(
    rows: List<PageRow>,
    entriesByRow: Map<Long, List<RowEntry>>,
    folders: List<Folder>,
    folderItemsByFolder: Map<Long, List<FolderItem>>,
    savedMapTargets: List<SavedMapTarget> = emptyList(),
    pinnedWebsites: List<PinnedWebsite> = emptyList(),
    pages: List<LauncherPage> = emptyList(),
    loadIcon: (ComponentName) -> ImageBitmap?,
    loadPackIcon: (String, String) -> ImageBitmap? = { _, _ -> null },
    resolveLabel: (ComponentName) -> String,
    onFolderClick: (Long) -> Unit,
    onShortcutClick: (String) -> Unit = {},
    onOpenPageLink: (Long, LauncherPageKind) -> Unit = { _, _ -> },
    onExecuteAction: (GestureAction) -> Unit = {},
    iconShape: IconShape = IconShape.ROUNDED_SQUARE,
    iconSizeScale: Float = 1f,
    /** 2 on a wide screen held sideways: the rows fill the left column first, then the right one,
     *  [unitsPerColumn] row units each. */
    columns: Int = 1,
    unitsPerColumn: Int = 0,
    modifier: Modifier = Modifier
) {
    if (columns >= 2 && unitsPerColumn > 0) {
        val unit = LocalRowUnitHeight.current
        val density = androidx.compose.ui.platform.LocalDensity.current
        val split = androidx.compose.runtime.remember(rows, entriesByRow, unit, unitsPerColumn) {
            var used = 0
            var right = false
            rows.partition { row ->
                val units = when {
                    row.type == com.bisc.flauncher.data.model.PageRowType.ELEMENT_BOX ->
                        PageRowDimens.unitsFor(PageRowDimens.elementBoxHeight(entriesByRow[row.id].orEmpty().size, row.columns.coerceAtLeast(1)), unit)
                    (row.heightPx ?: 0) > 0 -> PageRowDimens.unitsFor(with(density) { row.heightPx!!.toDp() } - androidx.compose.ui.unit.Dp(1f), unit)
                    else -> row.spanY.coerceAtLeast(1)
                }
                // Keeps the order: once a row goes right, all after it do too.
                if (!right && used + units <= unitsPerColumn) { used += units; true } else { right = true; false }
            }
        }
        androidx.compose.foundation.layout.Row(
            modifier = modifier.fillMaxWidth().fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            listOf(split.first, split.second).forEach { part ->
                PageRowsColumn(
                    rows = part, entriesByRow = entriesByRow, folders = folders, folderItemsByFolder = folderItemsByFolder,
                    savedMapTargets = savedMapTargets, pinnedWebsites = pinnedWebsites, pages = pages, loadIcon = loadIcon,
                    loadPackIcon = loadPackIcon, resolveLabel = resolveLabel, onFolderClick = onFolderClick,
                    onShortcutClick = onShortcutClick, onOpenPageLink = onOpenPageLink, onExecuteAction = onExecuteAction,
                    iconShape = iconShape, iconSizeScale = iconSizeScale, modifier = Modifier.weight(1f)
                )
            }
        }
        return
    }
    Column(
        modifier = modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(PageRowDimens.rowSpacing)
    ) {
        rows.forEach { row ->
            key(row.id) {
                PageRowContent(
                    row = row,
                    entries = entriesByRow[row.id].orEmpty(),
                    folders = folders,
                    folderItemsByFolder = folderItemsByFolder,
                    savedMapTargets = savedMapTargets,
                    pinnedWebsites = pinnedWebsites,
                    pages = pages,
                    loadIcon = loadIcon,
                    loadPackIcon = loadPackIcon,
                    resolveLabel = resolveLabel,
                    onFolderClick = onFolderClick,
                    onShortcutClick = onShortcutClick,
                    onOpenPageLink = onOpenPageLink,
                    onExecuteAction = onExecuteAction,
                    iconShape = iconShape,
                    iconSizeScale = iconSizeScale
                )
            }
        }
    }
}
