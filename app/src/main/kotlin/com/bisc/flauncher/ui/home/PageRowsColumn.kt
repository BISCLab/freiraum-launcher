package com.bisc.flauncher.ui.home

import android.content.ComponentName
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.bisc.flauncher.data.model.AppRowEntry
import com.bisc.flauncher.data.model.Folder
import com.bisc.flauncher.data.model.FolderItem
import com.bisc.flauncher.data.model.GestureAction
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PinnedWebsite
import com.bisc.flauncher.data.model.SavedMapTarget
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.data.settings.IconShape

/**
 * One page's content: [rows] (already sorted by orderIndex) stacked top to bottom, no scrolling — a
 * page is exactly one screen. Whatever doesn't fit is moved to a fresh page at the data layer instead
 * (see [com.bisc.flauncher.data.repository.PageContentRepository.rebalancePageOverflow]), so rows here
 * are expected to always fit; this is just a plain stacking layout, no page-snap or clipping tricks.
 */
@Composable
fun PageRowsColumn(
    rows: List<PageRow>,
    entriesByRow: Map<Long, List<AppRowEntry>>,
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
    handedness: Handedness = Handedness.RIGHT,
    iconShape: IconShape = IconShape.ROUNDED_SQUARE,
    iconSizeScale: Float = 1f,
    accentColor: Color = Color(0xFF3B82F6),
    modifier: Modifier = Modifier
) {
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
                    handedness = handedness,
                    iconShape = iconShape,
                    iconSizeScale = iconSizeScale,
                    accentColor = accentColor
                )
            }
        }
    }
}
