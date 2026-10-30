package com.bisc.flauncher.ui.home.search

import android.content.ComponentName
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.data.model.AppGridBarPosition
import com.bisc.flauncher.data.model.AppInfo
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.Folder
import com.bisc.flauncher.data.model.FolderItem
import com.bisc.flauncher.ui.home.AppIconCell
import com.bisc.flauncher.ui.home.FolderCell
import com.bisc.flauncher.util.matchesAllWordStarts

private sealed interface GridEntry {
    val key: String
    val label: String
    data class App(val app: AppInfo) : GridEntry {
        override val key get() = "app:" + app.componentName.flattenToString()
        override val label get() = app.label
    }
    data class FolderEntry(val folder: Folder) : GridEntry {
        override val key get() = "folder:" + folder.id
        override val label get() = folder.name
    }
}

/** The App grid page: all apps (and the chosen folders) in a fixed-column grid, with an optional
 *  search field above or below that filters it, or opens Text search. */
@Composable
fun AppGridContent(
    apps: List<AppInfo>,
    folders: List<Folder>,
    foldersAlphabetical: Boolean,
    folderItemsByFolder: Map<Long, List<FolderItem>>,
    query: String,
    queryResults: List<AppInfo>,
    columns: Int,
    iconSizeScale: Float,
    showLabels: Boolean,
    labelColor: Color,
    barPosition: AppGridBarPosition,
    barOpensTextSearch: Boolean,
    keyboardAutoOpen: Boolean,
    barAppearance: ElementAppearance,
    cornerRadiusPercent: Int,
    keyboardCapColor: Int?,
    keyboardCapCornerRadiusPercent: Int,
    keyboardCapArrow: Boolean,
    loadIcon: (ComponentName) -> ImageBitmap?,
    loadPackIcon: (String, String) -> ImageBitmap?,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onFolderClick: (Long) -> Unit,
    onOpenTextSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searching = query.isNotBlank()
    val entries = remember(apps, folders, foldersAlphabetical, query, queryResults) {
        if (searching) {
            folders.filter { matchesAllWordStarts(it.name, query) }.map { GridEntry.FolderEntry(it) } +
                queryResults.map { GridEntry.App(it) }
        } else {
            val folderEntries = folders.map { GridEntry.FolderEntry(it) }
            val appEntries = apps.map { GridEntry.App(it) }
            if (foldersAlphabetical) (folderEntries + appEntries).sortedBy { it.label.lowercase() }
            else folderEntries + appEntries
        }
    }

    @Composable
    fun Bar(modifier: Modifier) {
        AppSearchField(
            query = query,
            onQueryChange = onQueryChange,
            onSubmit = {
                when (val first = entries.firstOrNull()) {
                    is GridEntry.App -> onAppClick(first.app)
                    is GridEntry.FolderEntry -> onFolderClick(first.folder.id)
                    null -> Unit
                }
            },
            appearance = barAppearance,
            cornerRadiusPercent = cornerRadiusPercent,
            autoOpen = keyboardAutoOpen,
            onOpenTextSearch = if (barOpensTextSearch) onOpenTextSearch else null,
            modifier = modifier
        )
    }

    Column(modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))) {
        if (barPosition == AppGridBarPosition.TOP) Bar(Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val gridState = rememberLazyGridState()
            val fadeStart = (1f - FADE_TRANSITION_DISTANCE / maxHeight).coerceIn(0f, 1f)
            val canScrollFurther by remember { derivedStateOf { gridState.canScrollForward } }
            val topFadePx = with(LocalDensity.current) { TOP_FADE_HEIGHT.toPx() }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = gridState,
                // Same edge fades as Text search: toward the bottom while there's more to scroll, toward
                // the top once scrolled away from it - so the side with the search field always fades.
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (canScrollFurther) Modifier.fadeBackgroundToBottom(fadeStart, 1f) else Modifier)
                    .fadeListTop(TOP_FADE_HEIGHT) {
                        if (gridState.firstVisibleItemIndex > 0) 1f else (gridState.firstVisibleItemScrollOffset / topFadePx).coerceIn(0f, 1f)
                    },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(entries, key = { it.key }, contentType = { it::class }) { entry ->
                    when (entry) {
                        is GridEntry.App -> AppIconCell(
                            payload = entry.app.componentName.flattenToString(),
                            loadIcon = loadIcon,
                            resolveLabel = { entry.app.label },
                            showLabel = showLabels,
                            labelColor = labelColor,
                            iconSizeScale = iconSizeScale,
                            onClickOverride = { onAppClick(entry.app) }
                        )
                        is GridEntry.FolderEntry -> FolderCell(
                            folder = entry.folder,
                            previewItems = folderItemsByFolder[entry.folder.id].orEmpty(),
                            loadIcon = loadIcon,
                            loadPackIcon = loadPackIcon,
                            onClick = { onFolderClick(entry.folder.id) },
                            showLabel = showLabels,
                            labelColor = labelColor,
                            iconSizeScale = iconSizeScale
                        )
                    }
                }
            }
        }
        if (barPosition == AppGridBarPosition.BOTTOM) Bar(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp))
        if (keyboardCapColor != null) KeyboardCap(keyboardCapColor, keyboardCapCornerRadiusPercent, keyboardCapArrow)
    }
}
