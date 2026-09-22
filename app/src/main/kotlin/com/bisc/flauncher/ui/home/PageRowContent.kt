package com.bisc.flauncher.ui.home

import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.data.model.AppRowEntry
import com.bisc.flauncher.data.model.Folder
import com.bisc.flauncher.data.model.FolderItem
import com.bisc.flauncher.data.model.GestureAction
import com.bisc.flauncher.data.model.GestureActionType
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.PinnedWebsite
import com.bisc.flauncher.data.model.SavedMapTarget
import com.bisc.flauncher.data.model.WidgetKind
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.data.model.clockDateAction
import com.bisc.flauncher.data.model.clockTimeAction
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.data.settings.IconShape
import com.bisc.flauncher.ui.home.dock.DockDimens
import com.bisc.flauncher.ui.home.dock.QuickLaunchSlot
import com.bisc.flauncher.ui.home.dock.SearchBar
import com.bisc.flauncher.ui.home.dock.defaultQuickLaunchSlots
import com.bisc.flauncher.ui.home.widgets.ClockWidget
import com.bisc.flauncher.ui.home.widgets.MusicWidget
import com.bisc.flauncher.ui.home.widgets.NotesWidget
import com.bisc.flauncher.ui.home.widgets.SystemWidgetView
import com.bisc.flauncher.ui.home.widgets.WeatherWidget
import com.bisc.flauncher.ui.home.widgets.WidgetCard
import com.bisc.flauncher.util.launchApp
import com.bisc.flauncher.util.launchMapTarget
import com.bisc.flauncher.util.openWebsite

/** Renders one [PageRow], full page width, at its own height (widgets) or content-driven height (an apps row). */
@Composable
fun PageRowContent(
    row: PageRow,
    entries: List<AppRowEntry>,
    folders: List<Folder>,
    folderItemsByFolder: Map<Long, List<FolderItem>>,
    savedMapTargets: List<SavedMapTarget> = emptyList(),
    pinnedWebsites: List<PinnedWebsite> = emptyList(),
    loadIcon: (ComponentName) -> ImageBitmap?,
    loadPackIcon: (String, String) -> ImageBitmap? = { _, _ -> null },
    resolveLabel: (ComponentName) -> String = { "" },
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
    onSystemWidgetViewReady: ((android.view.View) -> Unit)? = null
) {
    when (row.type) {
        PageRowType.WIDGET -> {
            val horizontalMargin = if (row.edgeToEdge) 0.dp else DockDimens.horizontalMargin
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PageRowDimens.rowUnitHeight * row.spanY)
            ) {
                when (row.payload) {
                    WidgetKind.CLOCK -> ClockWidget(
                        style = row.clockStyle,
                        timeFormat = row.clockTimeFormat,
                        dateFormat = row.clockDateFormat,
                        showNextAlarm = row.clockShowNextAlarm,
                        alignment = row.clockAlignment,
                        onDateClick = row.clockDateAction().takeIf { it.type != GestureActionType.NONE }?.let { action -> { onExecuteAction(action) } },
                        onTimeClick = row.clockTimeAction().takeIf { it.type != GestureActionType.NONE }?.let { action -> { onExecuteAction(action) } }
                    )
                    WidgetKind.MUSIC -> MusicWidget(appearance = row.appearance(), cornerStyle = row.cornerStyle, horizontalMargin = horizontalMargin)
                    WidgetKind.WEATHER -> WeatherWidget(appearance = row.appearance(), cornerStyle = row.cornerStyle, horizontalMargin = horizontalMargin)
                    WidgetKind.NOTES -> NotesWidget(appearance = row.appearance(), cornerStyle = row.cornerStyle, horizontalMargin = horizontalMargin, spanY = row.spanY)
                    WidgetKind.SPACER -> Unit
                }
            }
        }
        PageRowType.SYSTEM_WIDGET -> {
            val appWidgetId = remember(row.payload) { row.payload.toIntOrNull() }
            if (appWidgetId != null) {
                val horizontalMargin = if (row.edgeToEdge) 0.dp else DockDimens.horizontalMargin
                SystemWidgetView(
                    appWidgetId = appWidgetId,
                    modifier = Modifier.fillMaxWidth().height(PageRowDimens.rowUnitHeight * row.spanY),
                    appearance = row.appearance(),
                    cornerStyle = row.cornerStyle,
                    horizontalMargin = horizontalMargin,
                    onHostedViewReady = onSystemWidgetViewReady
                )
            }
        }
        PageRowType.APPS -> {
            AppsRowGrid(
                row = row,
                entries = entries,
                folders = folders,
                folderItemsByFolder = folderItemsByFolder,
                savedMapTargets = savedMapTargets,
                pinnedWebsites = pinnedWebsites,
                loadIcon = loadIcon,
                loadPackIcon = loadPackIcon,
                resolveLabel = resolveLabel,
                onFolderClick = onFolderClick,
                onShortcutClick = onShortcutClick
            )
        }
        PageRowType.SEARCH_BAR -> {
            SearchBar(
                onSingleTap = onSearchBarTap,
                onDoubleTap = onSearchBarDoubleTap,
                handedness = handedness,
                iconShape = iconShape,
                appearance = row.appearance(),
                quickLaunchSlots = quickLaunchSlots,
                loadQuickLaunchIcon = loadQuickLaunchIcon,
                onQuickLaunchClick = onQuickLaunchClick
            )
        }
    }
}

@Composable
private fun AppsRowGrid(
    row: PageRow,
    entries: List<AppRowEntry>,
    folders: List<Folder>,
    folderItemsByFolder: Map<Long, List<FolderItem>>,
    savedMapTargets: List<SavedMapTarget>,
    pinnedWebsites: List<PinnedWebsite>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    loadPackIcon: (String, String) -> ImageBitmap?,
    resolveLabel: (ComponentName) -> String,
    onFolderClick: (Long) -> Unit,
    onShortcutClick: (String) -> Unit
) {
    val columns = row.columns.coerceAtLeast(1)
    val horizontalMargin = if (row.edgeToEdge) 0.dp else DockDimens.horizontalMargin
    WidgetCard(
        modifier = Modifier.fillMaxWidth(),
        appearance = row.appearance(),
        cornerStyle = row.cornerStyle,
        horizontalMargin = horizontalMargin,
        contentPadding = 12.dp
    ) {
        entries.chunked(columns).forEach { chunk ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                repeat(columns) { slot ->
                    Box(modifier = Modifier.weight(1f)) {
                        val entry = chunk.getOrNull(slot)
                        if (entry != null) {
                            when (entry.type) {
                                GridItemType.APP -> AppIconCell(payload = entry.payload, loadIcon = loadIcon, resolveLabel = resolveLabel)
                                GridItemType.FOLDER -> {
                                    val folderId = remember(entry.payload) { entry.payload.toLongOrNull() }
                                    val folder = folders.find { it.id == folderId }
                                    val previewItems = folderId?.let { folderItemsByFolder[it] }.orEmpty()
                                    FolderCell(folder = folder, previewItems = previewItems, loadIcon = loadIcon, loadPackIcon = loadPackIcon, onClick = { folderId?.let(onFolderClick) })
                                }
                                GridItemType.SHORTCUT -> ShortcutCell(payload = entry.payload, onClick = { onShortcutClick(entry.payload) })
                                GridItemType.MAP_TARGET -> {
                                    val targetId = remember(entry.payload) { entry.payload.toLongOrNull() }
                                    val target = savedMapTargets.find { it.id == targetId }
                                    MapTargetCell(target = target)
                                }
                                GridItemType.WEBSITE -> {
                                    val websiteId = remember(entry.payload) { entry.payload.toLongOrNull() }
                                    val website = pinnedWebsites.find { it.id == websiteId }
                                    WebsiteCell(website = website)
                                }
                                GridItemType.WIDGET, GridItemType.SYSTEM_WIDGET -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIconCell(payload: String, loadIcon: (ComponentName) -> ImageBitmap?, resolveLabel: (ComponentName) -> String) {
    val context = LocalContext.current
    val componentName = remember(payload) { ComponentName.unflattenFromString(payload) }
    val bitmap = rememberAppIcon(componentName, loadIcon)
    val label = componentName?.let(resolveLabel).orEmpty()

    Column(
        modifier = Modifier
            .clickable(enabled = componentName != null) {
                componentName?.let { launchApp(context, it) }
            }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = label, modifier = Modifier.size(56.dp))
        } else {
            Spacer(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            )
        }
        Spacer(Modifier.height(8.dp))
        GridLabel(label)
    }
}

@Composable
private fun ShortcutCell(payload: String, onClick: () -> Unit) {
    val label = remember(payload) { payload.split("|").getOrNull(2)?.takeIf { it.isNotBlank() } ?: payload.substringBefore('|') }
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.AutoMirrored.Filled.Shortcut, contentDescription = null, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(8.dp))
        GridLabel(label)
    }
}

@Composable
private fun MapTargetCell(target: SavedMapTarget?) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .clickable(enabled = target != null) { target?.let { launchMapTarget(context, it) } }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(8.dp))
        GridLabel(target?.label.orEmpty())
    }
}

@Composable
private fun WebsiteCell(website: PinnedWebsite?) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .clickable(enabled = website != null) { website?.let { openWebsite(context, it) } }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(8.dp))
        GridLabel(website?.label.orEmpty())
    }
}

@Composable
private fun FolderCell(
    folder: Folder?,
    previewItems: List<FolderItem>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    loadPackIcon: (String, String) -> ImageBitmap?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FolderIcon(folder = folder, previewItems = previewItems, loadIcon = loadIcon, loadPackIcon = loadPackIcon, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(8.dp))
        GridLabel(folder?.name.orEmpty())
    }
}

@Composable
private fun GridLabel(text: String) {
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.labelSmall.copy(
            color = Color.White,
            shadow = Shadow(color = Color.Black, blurRadius = 6f, offset = Offset(0f, 1f))
        )
    )
}
