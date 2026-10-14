package com.bisc.flauncher.ui.home

import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.data.model.AppRowEntry
import com.bisc.flauncher.data.model.DockWidthMode
import com.bisc.flauncher.data.model.Folder
import com.bisc.flauncher.data.model.FolderItem
import com.bisc.flauncher.data.model.GestureAction
import com.bisc.flauncher.data.model.GestureActionType
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.PinnedWebsite
import com.bisc.flauncher.data.model.SavedMapTarget
import com.bisc.flauncher.data.model.WidgetCornerStyle
import com.bisc.flauncher.data.model.WidgetKind
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.data.model.clockDateAction
import com.bisc.flauncher.data.model.clockTimeAction
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.data.settings.IconShape
import com.bisc.flauncher.ui.home.dock.DockDimens
import com.bisc.flauncher.ui.home.dock.PageIconGlyph
import com.bisc.flauncher.ui.home.dock.PageIconGlyphImage
import com.bisc.flauncher.ui.home.dock.UNSET_APP_SLOT_COLOR
import com.bisc.flauncher.ui.home.dock.defaultColor
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.home.dock.unsetAppSlotIcon
import com.bisc.flauncher.ui.home.widgets.ClockWidget
import com.bisc.flauncher.ui.home.widgets.MusicWidget
import com.bisc.flauncher.ui.home.widgets.NotesWidget
import com.bisc.flauncher.ui.home.widgets.SystemWidgetView
import com.bisc.flauncher.ui.home.widgets.WeatherWidget
import com.bisc.flauncher.ui.home.widgets.WidgetCard
import com.bisc.flauncher.ui.pageLabel
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
    pages: List<LauncherPage> = emptyList(),
    loadIcon: (ComponentName) -> ImageBitmap?,
    loadPackIcon: (String, String) -> ImageBitmap? = { _, _ -> null },
    resolveLabel: (ComponentName) -> String = { "" },
    onFolderClick: (Long) -> Unit,
    onShortcutClick: (String) -> Unit = {},
    onOpenPageLink: (Long, LauncherPageKind) -> Unit = { _, _ -> },
    onExecuteAction: (GestureAction) -> Unit = {},
    handedness: Handedness = Handedness.RIGHT,
    iconShape: IconShape = IconShape.ROUNDED_SQUARE,
    onSystemWidgetViewReady: ((android.view.View) -> Unit)? = null,
    accentColor: Color = Color(0xFF3B82F6)
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
                        customInk = row.appearance().contentColor?.let { Color(it) },
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
                val exactHeightPx = row.heightPx
                val rowHeight = if (exactHeightPx != null && exactHeightPx > 0) {
                    with(androidx.compose.ui.platform.LocalDensity.current) { exactHeightPx.toDp() }
                } else {
                    PageRowDimens.rowUnitHeight * row.spanY
                }
                SystemWidgetView(
                    appWidgetId = appWidgetId,
                    modifier = Modifier.fillMaxWidth().height(rowHeight),
                    appearance = row.appearance(),
                    cornerStyle = row.cornerStyle,
                    horizontalMargin = horizontalMargin,
                    onHostedViewReady = onSystemWidgetViewReady
                )
            }
        }
        PageRowType.APPS -> {
            if (row.pillMode) {
                PillRow(
                    row = row,
                    entries = entries,
                    folders = folders,
                    folderItemsByFolder = folderItemsByFolder,
                    savedMapTargets = savedMapTargets,
                    pinnedWebsites = pinnedWebsites,
                    pages = pages,
                    loadIcon = loadIcon,
                    loadPackIcon = loadPackIcon,
                    resolveLabel = resolveLabel,
                    iconShape = iconShape,
                    onFolderClick = onFolderClick,
                    onShortcutClick = onShortcutClick,
                    onOpenPageLink = onOpenPageLink
                )
            } else {
                AppsRowGrid(
                    row = row,
                    entries = entries,
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
                    onOpenPageLink = onOpenPageLink
                )
            }
        }
        // The dock never actually renders inline as a row — HomeScreen strips this row out before handing
        // the rest to PageRowsColumn and renders the floating dock itself, directly, as its own layer
        // outside the pager, calling PillRow with the same entries (see HomeScreen's floating dock Box).
        // This row only still exists as a marker carrying the dock's own per-row appearance/entries;
        // PageRowType must stay exhaustive.
        PageRowType.SEARCH_BAR -> Unit
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
    pages: List<LauncherPage>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    loadPackIcon: (String, String) -> ImageBitmap?,
    resolveLabel: (ComponentName) -> String,
    onFolderClick: (Long) -> Unit,
    onShortcutClick: (String) -> Unit,
    onOpenPageLink: (Long, LauncherPageKind) -> Unit
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
                                GridItemType.PAGE -> {
                                    val pageId = remember(entry.payload) { entry.payload.toLongOrNull() }
                                    val page = pages.find { it.id == pageId }
                                    PageLinkCell(page = page, pages = pages, onClick = { page?.let { onOpenPageLink(it.id, it.kind) } })
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
private fun PageLinkCell(page: LauncherPage?, pages: List<LauncherPage>, onClick: () -> Unit) {
    val glyph = page?.iconGlyph?.let { name -> runCatching { PageIconGlyph.valueOf(name) }.getOrNull() }
        ?: page?.let { PageIconGlyph.forPageKind(it.kind) } ?: PageIconGlyph.WIDGET_PAGE
    val background = page?.iconBackgroundColor?.let { Color(it) } ?: glyph.defaultColor()
    val tint = page?.iconColor?.let { Color(it) } ?: Color.White
    Column(
        modifier = Modifier
            .clickable(enabled = page != null, onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(background),
            contentAlignment = Alignment.Center
        ) {
            PageIconGlyphImage(glyph = glyph, tint = tint, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(8.dp))
        GridLabel(page?.let { pageLabel(it, pages) }.orEmpty())
    }
}

/** Per-icon clip shape for a pill-style row — shared by [PillRow] and the Dock's own flush panel in
 *  HomeScreen.kt, which lays [PillRowEntries] out directly rather than through [PillRow]. */
@Composable
fun pillItemShape(iconShape: IconShape): Shape = when (iconShape) {
    IconShape.CIRCLE -> CircleShape
    IconShape.ROUNDED_SQUARE -> RoundedCornerShape(DockDimens.iconCornerRadius)
}

/**
 * A single non-wrapping line of entries (no per-cell text labels — matches the old dock's uniform
 * squircle-button look), for any row with [PageRow.pillMode] set — the dock being the one instance
 * that floats outside the pager (see HomeScreen's floating dock Box, which calls this directly with
 * the home page's pinned SEARCH_BAR row), but any ordinary [PageRowType.APPS] row can opt into the
 * same layout. Width is either pinned to [PageRow.pillMaxEntries] reference slots or grows with the
 * actual entry count, per [PageRow.pillWidthMode] (see [DockDimens.pillWidth]). When [onAddEmptySlot]
 * is non-null, the row pads itself with tappable empty placeholders up to [PageRow.pillMaxEntries] —
 * the dock's classic "tap an empty slot to fill it" invitation; an ordinary in-page pill row (edited
 * from its own Settings screen instead) passes null and simply shows however many real entries it has.
 */
@Composable
fun PillRow(
    row: PageRow,
    entries: List<AppRowEntry>,
    folders: List<Folder>,
    folderItemsByFolder: Map<Long, List<FolderItem>>,
    savedMapTargets: List<SavedMapTarget>,
    pinnedWebsites: List<PinnedWebsite>,
    pages: List<LauncherPage>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    loadPackIcon: (String, String) -> ImageBitmap? = { _, _ -> null },
    resolveLabel: (ComponentName) -> String = { "" },
    iconShape: IconShape = IconShape.ROUNDED_SQUARE,
    onFolderClick: (Long) -> Unit = {},
    onShortcutClick: (String) -> Unit = {},
    onOpenPageLink: (Long, LauncherPageKind) -> Unit = { _, _ -> },
    onAddEmptySlot: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = resolveElementColors(row.appearance())
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
        val displayedCount = if (onAddEmptySlot != null) maxOf(entries.size, row.pillMaxEntries) else entries.size
        val pillWidth = DockDimens.pillWidth(displayedCount, row.pillWidthMode, maxWidth)
        val itemShape = pillItemShape(iconShape)
        val barShape = remember(row.cornerStyle) {
            if (row.cornerStyle == WidgetCornerStyle.SQUARE) RoundedCornerShape(0.dp) else RoundedCornerShape(percent = 50)
        }
        val barHeight = if (row.showLabels) DockDimens.barHeight + DockDimens.labelExtraHeight else DockDimens.barHeight

        Box(
            modifier = Modifier
                .width(pillWidth)
                .height(barHeight)
                .then(
                    if (colors.backgroundAlpha > 0f) Modifier.clip(barShape).background(colors.background, alpha = colors.backgroundAlpha)
                    else Modifier
                )
                .then(
                    if (row.appearance().borderEnabled) Modifier.border(colors.borderWidth, colors.borderColor, barShape) else Modifier
                )
        ) {
            PillRowEntries(
                entries = entries,
                folders = folders,
                folderItemsByFolder = folderItemsByFolder,
                savedMapTargets = savedMapTargets,
                pinnedWebsites = pinnedWebsites,
                pages = pages,
                loadIcon = loadIcon,
                loadPackIcon = loadPackIcon,
                resolveLabel = resolveLabel,
                itemShape = itemShape,
                onFolderClick = onFolderClick,
                onShortcutClick = onShortcutClick,
                onOpenPageLink = onOpenPageLink,
                onAddEmptySlot = onAddEmptySlot,
                maxEntries = row.pillMaxEntries,
                showLabels = row.showLabels,
                labelColor = row.appearance().contentColor?.let { Color(it) } ?: Color.White,
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = DockDimens.innerHorizontalPadding)
            )
        }
    }
}

/** The actual row of icon cells shared by [PillRow] (the floating/inline pill bar) and the Dock's own
 *  flush bottom panel in HomeScreen.kt — extracted so the panel can lay these out full-width inside
 *  its own [com.bisc.flauncher.ui.home.dock.DockPanelSurface] shell instead of [PillRow]'s pill-shaped
 *  bar Box. */
@Composable
fun PillRowEntries(
    entries: List<AppRowEntry>,
    folders: List<Folder>,
    folderItemsByFolder: Map<Long, List<FolderItem>>,
    savedMapTargets: List<SavedMapTarget>,
    pinnedWebsites: List<PinnedWebsite>,
    pages: List<LauncherPage>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    loadPackIcon: (String, String) -> ImageBitmap? = { _, _ -> null },
    resolveLabel: (ComponentName) -> String = { "" },
    itemShape: Shape,
    onFolderClick: (Long) -> Unit = {},
    onShortcutClick: (String) -> Unit = {},
    onOpenPageLink: (Long, LauncherPageKind) -> Unit = { _, _ -> },
    onAddEmptySlot: ((Int) -> Unit)?,
    maxEntries: Int,
    showLabels: Boolean = false,
    labelColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        entries.forEach { entry ->
            key(entry.id) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(DockDimens.iconSize).clip(itemShape), contentAlignment = Alignment.Center) {
                    when (entry.type) {
                        GridItemType.APP -> {
                            val componentName = remember(entry.payload) { ComponentName.unflattenFromString(entry.payload) }
                            val bitmap = rememberAppIcon(componentName, loadIcon)
                            val label = componentName?.let(resolveLabel).orEmpty()
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(if (bitmap == null) UNSET_APP_SLOT_COLOR else Color.Transparent)
                                    .clickable(enabled = componentName != null) { componentName?.let { launchApp(context, it) } },
                                contentAlignment = Alignment.Center
                            ) {
                                if (bitmap != null) Image(bitmap = bitmap, contentDescription = label, modifier = Modifier.size(DockDimens.iconSize))
                                else Icon(imageVector = unsetAppSlotIcon, contentDescription = null, tint = Color.White)
                            }
                        }
                        GridItemType.FOLDER -> {
                            val folderId = remember(entry.payload) { entry.payload.toLongOrNull() }
                            val folder = folders.find { it.id == folderId }
                            val previewItems = folderId?.let { folderItemsByFolder[it] }.orEmpty()
                            FolderIcon(
                                folder = folder,
                                previewItems = previewItems,
                                loadIcon = loadIcon,
                                loadPackIcon = loadPackIcon,
                                modifier = Modifier.fillMaxSize().clickable { folderId?.let(onFolderClick) }
                            )
                        }
                        GridItemType.SHORTCUT -> {
                            Box(
                                modifier = Modifier.fillMaxSize().background(UNSET_APP_SLOT_COLOR).clickable { onShortcutClick(entry.payload) },
                                contentAlignment = Alignment.Center
                            ) { Icon(imageVector = Icons.AutoMirrored.Filled.Shortcut, contentDescription = null, tint = Color.White) }
                        }
                        GridItemType.MAP_TARGET -> {
                            val targetId = remember(entry.payload) { entry.payload.toLongOrNull() }
                            val target = savedMapTargets.find { it.id == targetId }
                            Box(
                                modifier = Modifier.fillMaxSize().background(UNSET_APP_SLOT_COLOR)
                                    .clickable(enabled = target != null) { target?.let { launchMapTarget(context, it) } },
                                contentAlignment = Alignment.Center
                            ) { Icon(imageVector = Icons.Filled.Place, contentDescription = null, tint = Color.White) }
                        }
                        GridItemType.WEBSITE -> {
                            val websiteId = remember(entry.payload) { entry.payload.toLongOrNull() }
                            val website = pinnedWebsites.find { it.id == websiteId }
                            Box(
                                modifier = Modifier.fillMaxSize().background(UNSET_APP_SLOT_COLOR)
                                    .clickable(enabled = website != null) { website?.let { openWebsite(context, it) } },
                                contentAlignment = Alignment.Center
                            ) { Icon(imageVector = Icons.Filled.Language, contentDescription = null, tint = Color.White) }
                        }
                        GridItemType.PAGE -> {
                            val pageId = remember(entry.payload) { entry.payload.toLongOrNull() }
                            val page = pages.find { it.id == pageId }
                            val glyph = page?.iconGlyph?.let { name -> runCatching { PageIconGlyph.valueOf(name) }.getOrNull() }
                                ?: page?.let { PageIconGlyph.forPageKind(it.kind) } ?: PageIconGlyph.WIDGET_PAGE
                            val background = page?.iconBackgroundColor?.let { Color(it) } ?: glyph.defaultColor()
                            val tint = page?.iconColor?.let { Color(it) } ?: Color.White
                            Box(
                                modifier = Modifier.fillMaxSize().background(background)
                                    .clickable(enabled = page != null) { page?.let { onOpenPageLink(it.id, it.kind) } },
                                contentAlignment = Alignment.Center
                            ) { PageIconGlyphImage(glyph = glyph, tint = tint, modifier = Modifier.size(DockDimens.iconSize * 0.55f)) }
                        }
                        GridItemType.WIDGET, GridItemType.SYSTEM_WIDGET -> Unit
                    }
                }
                if (showLabels) {
                    GridLabel(
                        text = entryLabel(entry, folders, savedMapTargets, pinnedWebsites, pages, resolveLabel),
                        color = labelColor,
                        modifier = Modifier.width(DockDimens.iconSize)
                    )
                }
                }
            }
        }
        if (onAddEmptySlot != null) {
            repeat((maxEntries - entries.size).coerceAtLeast(0)) { offset ->
                val slotIndex = entries.size + offset
                key("empty_$slotIndex") {
                    Box(
                        modifier = Modifier
                            .size(DockDimens.iconSize)
                            .clip(itemShape)
                            .background(UNSET_APP_SLOT_COLOR)
                            .clickable { onAddEmptySlot(slotIndex) },
                        contentAlignment = Alignment.Center
                    ) { Icon(imageVector = unsetAppSlotIcon, contentDescription = null, tint = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun GridLabel(text: String, color: Color = Color.White, modifier: Modifier = Modifier) {
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(
            color = color,
            shadow = Shadow(color = Color.Black, blurRadius = 6f, offset = Offset(0f, 1f))
        )
    )
}

/** Resolves a display name for any [AppRowEntry] type — shared by [PillRowEntries]' optional
 *  captions (PageRow.showLabels) and AppsRowGrid's own always-on per-cell labels. */
@Composable
private fun entryLabel(
    entry: AppRowEntry,
    folders: List<Folder>,
    savedMapTargets: List<SavedMapTarget>,
    pinnedWebsites: List<PinnedWebsite>,
    pages: List<LauncherPage>,
    resolveLabel: (ComponentName) -> String
): String = when (entry.type) {
    GridItemType.APP -> ComponentName.unflattenFromString(entry.payload)?.let(resolveLabel).orEmpty()
    GridItemType.FOLDER -> folders.find { it.id == entry.payload.toLongOrNull() }?.name.orEmpty()
    GridItemType.SHORTCUT -> entry.payload.split("|").getOrNull(2)?.takeIf { it.isNotBlank() } ?: entry.payload.substringBefore('|')
    GridItemType.MAP_TARGET -> savedMapTargets.find { it.id == entry.payload.toLongOrNull() }?.label.orEmpty()
    GridItemType.WEBSITE -> pinnedWebsites.find { it.id == entry.payload.toLongOrNull() }?.label.orEmpty()
    GridItemType.PAGE -> pages.find { it.id == entry.payload.toLongOrNull() }?.let { pageLabel(it, pages) }.orEmpty()
    GridItemType.WIDGET, GridItemType.SYSTEM_WIDGET -> ""
}
