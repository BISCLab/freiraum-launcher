package com.bisc.flauncher.ui.home

import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewCarousel
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
import com.bisc.flauncher.data.model.RowEntry
import com.bisc.flauncher.data.model.Folder
import com.bisc.flauncher.data.model.FolderItem
import com.bisc.flauncher.data.model.GestureAction
import com.bisc.flauncher.data.model.GestureActionType
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.PinnedContact
import com.bisc.flauncher.data.model.PinnedWebsite
import com.bisc.flauncher.data.model.SavedMapTarget
import com.bisc.flauncher.data.model.WidgetKind
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.data.model.clockDateAction
import com.bisc.flauncher.data.model.clockTimeAction
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

/** Renders one [PageRow], full page width, at its own height (widgets) or content-driven height (an element box). */
@Composable
fun PageRowContent(
    row: PageRow,
    entries: List<RowEntry>,
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
    iconShape: IconShape = IconShape.ROUNDED_SQUARE,
    iconSizeScale: Float = 1f,
    onSystemWidgetViewReady: ((android.view.View) -> Unit)? = null
) {
    when (row.type) {
        PageRowType.WIDGET -> {
            val horizontalMargin = if (row.edgeToEdge) 0.dp else DockDimens.horizontalMargin
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PageRowDimens.spanHeight(LocalRowUnitHeight.current, row.spanY))
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
                    WidgetKind.MUSIC -> MusicWidget(appearance = row.appearance(), cornerRadiusPercent = row.cornerRadiusPercent, horizontalMargin = horizontalMargin)
                    WidgetKind.WEATHER -> WeatherWidget(appearance = row.appearance(), cornerRadiusPercent = row.cornerRadiusPercent, horizontalMargin = horizontalMargin)
                    WidgetKind.NOTES -> NotesWidget(appearance = row.appearance(), cornerRadiusPercent = row.cornerRadiusPercent, horizontalMargin = horizontalMargin, spanY = row.spanY)
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
                    PageRowDimens.spanHeight(LocalRowUnitHeight.current, row.spanY)
                }
                SystemWidgetView(
                    appWidgetId = appWidgetId,
                    modifier = Modifier.fillMaxWidth().height(rowHeight),
                    appearance = row.appearance(),
                    cornerRadiusPercent = row.cornerRadiusPercent,
                    horizontalMargin = horizontalMargin,
                    onHostedViewReady = onSystemWidgetViewReady
                )
            }
        }
        PageRowType.ELEMENT_BOX -> {
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
                ElementBoxGrid(
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
                    onOpenPageLink = onOpenPageLink,
                    iconSizeScale = iconSizeScale
                )
            }
        }
        // Neither the dock nor the Panel ever render inline as a row - HomeScreen strips both out
        // before handing the rest to PageRowsColumn and renders them itself, directly, as their own
        // floating layers outside the pager (see HomeScreen's floating dock Box and Panel overlay).
        // These rows only still exist as markers carrying their own per-row appearance/entries;
        // PageRowType must stay exhaustive.
        PageRowType.SEARCH_BAR, PageRowType.PANEL -> Unit
    }
}

@Composable
private fun ElementBoxGrid(
    row: PageRow,
    entries: List<RowEntry>,
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
    onOpenPageLink: (Long, LauncherPageKind) -> Unit,
    iconSizeScale: Float = 1f
) {
    val horizontalMargin = if (row.edgeToEdge) 0.dp else DockDimens.horizontalMargin
    // Height rounded up to whole row units (icons centered inside), so the box sits on the page's
    // row grid like every widget does.
    WidgetCard(
        modifier = Modifier.fillMaxWidth().snapHeightToRowGrid(LocalRowUnitHeight.current),
        appearance = row.appearance(),
        cornerRadiusPercent = row.cornerRadiusPercent,
        horizontalMargin = horizontalMargin,
        contentPadding = 12.dp,
        verticalArrangement = Arrangement.Center
    ) {
        GridEntries(
            entries = entries,
            columns = row.columns.coerceAtLeast(1),
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
            iconSizeScale = iconSizeScale
        )
    }
}

/** The actual grid of cells, extracted from [ElementBoxGrid] so the Favorites/Panel (which wraps this
 *  directly in its own [com.bisc.flauncher.ui.home.dock.DockPanelSurface] shell instead of a
 *  [WidgetCard]) can reuse the exact same per-type cell rendering, now extended with an optional
 *  [totalSlots]/[onAddEmptySlot] pair (pad with tappable "+" cells up to a target grid shape,
 *  the dock's "+" slot idea generalized to two dimensions), [GridItemType.CONTACT], and
 *  [rowColumns]/[onEditEntry] for the Panel's own per-row-column, centered shape (see their own
 *  doc comments below). Ordinary in-page element boxes pass neither of the Panel-only params - a
 *  single [columns] value, stretched to fill the row's full width, same as before. */
@Composable
fun GridEntries(
    entries: List<RowEntry>,
    columns: Int,
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
    onOpenPageLink: (Long, LauncherPageKind) -> Unit,
    pinnedContacts: List<PinnedContact> = emptyList(),
    loadContactPhoto: suspend (Long, String?) -> ImageBitmap? = { _, _ -> null },
    onContactClick: (Long) -> Unit = {},
    actionButtons: List<com.bisc.flauncher.data.model.ActionButton> = emptyList(),
    onActionButtonClick: (com.bisc.flauncher.data.model.ActionButton) -> Unit = {},
    showLabels: Boolean = true,
    labelColor: Color = Color.White,
    iconSizeScale: Float = 1f,
    totalSlots: Int? = null,
    onAddEmptySlot: ((Int) -> Unit)? = null,
    // The Panel's own shape: one column count per sub-row (see PageRow.panelRows), each row
    // centered around the middle with fixed-size cells and a constant gap - rather than stretched
    // with Arrangement.SpaceEvenly to fill the full width - so a 3-wide row and a 5-wide row both
    // read as "centered, evenly spaced" instead of the shorter one looking stretched out. When set,
    // this entirely replaces the [columns]/[totalSlots] flat-chunking path below.
    rowColumns: List<Int>? = null,
    // Settings-only: when set, tapping any *filled* cell calls this instead of its normal live
    // action (launch the app, open the folder, …) - the Panel's Elements editor uses it to let you
    // remove an entry by tapping it directly, instead of a separate reorderable list underneath.
    onEditEntry: ((RowEntry) -> Unit)? = null,
    // The Dock's own need: every sub-row centered within the same fixed reference width (sized for
    // this many columns) instead of its own actual content width, so a shorter row (e.g. 4 icons)
    // gets the same edge-to-icon gap as a full row (5 icons) rather than extra Arrangement.Center
    // slack making its edges sit further in. Null (the Panel's case) sizes each row to its content.
    referenceColumnCount: Int? = null,
    // Vertical gap between sub-rows - also Dock-only (0 keeps the Panel's rows flush, as before).
    rowGap: androidx.compose.ui.unit.Dp = 0.dp,
    // Dock/Panel: each entry sits in the slot its orderIndex names, so removing one leaves a gap
    // instead of shifting the rest along. Otherwise entries simply fill the slots in order.
    fixedSlots: Boolean = false
) {
    @Composable
    fun Cell(entry: RowEntry?, slotIndex: Int) {
        if (entry == null) {
            if (onAddEmptySlot != null) EmptyGridSlotCell(onClick = { onAddEmptySlot(slotIndex) }, iconSizeScale = iconSizeScale)
            return
        }
        val editOverride = onEditEntry?.let { cb -> { cb(entry) } }
        when (entry.type) {
            GridItemType.APP -> AppIconCell(payload = entry.payload, loadIcon = loadIcon, resolveLabel = resolveLabel, showLabel = showLabels, labelColor = labelColor, iconSizeScale = iconSizeScale, onClickOverride = editOverride)
            GridItemType.FOLDER -> {
                val folderId = remember(entry.payload) { entry.payload.toLongOrNull() }
                val folder = folders.find { it.id == folderId }
                val previewItems = folderId?.let { folderItemsByFolder[it] }.orEmpty()
                FolderCell(folder = folder, previewItems = previewItems, loadIcon = loadIcon, loadPackIcon = loadPackIcon, onClick = editOverride ?: { if (folderId != null) onFolderClick(folderId) }, showLabel = showLabels, labelColor = labelColor, iconSizeScale = iconSizeScale)
            }
            GridItemType.SHORTCUT -> ShortcutCell(payload = entry.payload, onClick = editOverride ?: { onShortcutClick(entry.payload) }, showLabel = showLabels, labelColor = labelColor, iconSizeScale = iconSizeScale)
            GridItemType.MAP_TARGET -> {
                val targetId = remember(entry.payload) { entry.payload.toLongOrNull() }
                val target = savedMapTargets.find { it.id == targetId }
                MapTargetCell(target = target, showLabel = showLabels, labelColor = labelColor, iconSizeScale = iconSizeScale, onClickOverride = editOverride)
            }
            GridItemType.WEBSITE -> {
                val websiteId = remember(entry.payload) { entry.payload.toLongOrNull() }
                val website = pinnedWebsites.find { it.id == websiteId }
                WebsiteCell(website = website, showLabel = showLabels, labelColor = labelColor, iconSizeScale = iconSizeScale, onClickOverride = editOverride)
            }
            GridItemType.PAGE -> {
                val pageId = remember(entry.payload) { entry.payload.toLongOrNull() }
                val page = pages.find { it.id == pageId }
                PageLinkCell(page = page, pages = pages, onClick = editOverride ?: { if (page != null) onOpenPageLink(page.id, page.kind) }, showLabel = showLabels, labelColor = labelColor, iconSizeScale = iconSizeScale)
            }
            GridItemType.CONTACT -> {
                val contactId = remember(entry.payload) { entry.payload.toLongOrNull() }
                val contact = pinnedContacts.find { it.contactId == contactId }
                ContactCell(
                    contact = contact,
                    loadContactPhoto = loadContactPhoto,
                    onClick = editOverride ?: { if (contactId != null) onContactClick(contactId) },
                    showLabel = showLabels,
                    labelColor = labelColor,
                    iconSizeScale = iconSizeScale
                )
            }
            GridItemType.ACTION_BUTTON -> {
                val buttonId = remember(entry.payload) { entry.payload.toLongOrNull() }
                val button = actionButtons.find { it.id == buttonId }
                ActionButtonCell(
                    button = button,
                    onClick = editOverride ?: { if (button != null) onActionButtonClick(button) },
                    showLabel = showLabels,
                    labelColor = labelColor,
                    iconSizeScale = iconSizeScale
                )
            }
            GridItemType.WIDGET, GridItemType.SYSTEM_WIDGET -> Unit
        }
    }

    if (rowColumns != null) {
        val bySlot = if (fixedSlots) entries.associateBy { it.orderIndex } else null
        fun entryAt(slot: Int): RowEntry? = bySlot?.get(slot) ?: if (bySlot == null) entries.getOrNull(slot) else null
        var consumed = 0
        val cellWidth = 64.dp * iconSizeScale
        val columnGap = 16.dp
        fun widthFor(cols: Int) = cellWidth * cols + columnGap * (cols - 1).coerceAtLeast(0)
        val referenceWidth = referenceColumnCount?.let { widthFor(it.coerceAtLeast(1)) }
        rowColumns.forEachIndexed { rowIndex, cols ->
            if (rowIndex > 0 && rowGap > 0.dp) Spacer(Modifier.height(rowGap))
            val safeCols = cols.coerceAtLeast(1)
            val rowStart = consumed
            consumed += safeCols
            val containerWidth = referenceWidth?.let { maxOf(it, widthFor(safeCols)) } ?: widthFor(safeCols)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(modifier = Modifier.width(containerWidth), horizontalArrangement = Arrangement.Center) {
                    repeat(safeCols) { slot ->
                        if (slot > 0) Spacer(Modifier.width(columnGap))
                        Box(modifier = Modifier.width(cellWidth)) {
                            Cell(entryAt(rowStart + slot), rowStart + slot)
                        }
                    }
                }
            }
        }
    } else {
        val safeColumns = columns.coerceAtLeast(1)
        val padded: List<RowEntry?> = if (totalSlots != null && onAddEmptySlot != null) {
            entries + List((totalSlots - entries.size).coerceAtLeast(0)) { null }
        } else {
            entries
        }
        padded.chunked(safeColumns).forEachIndexed { chunkIndex, chunk ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                repeat(safeColumns) { slot ->
                    Box(modifier = Modifier.weight(1f)) {
                        Cell(chunk.getOrNull(slot), chunkIndex * safeColumns + slot)
                    }
                }
            }
        }
    }
}

/** Clip shape for the cell types whose background box is drawn directly in Compose rather than
 *  baked into a cached bitmap - see [LocalIconShape]'s doc comment for why those need this instead
 *  of just picking up the shape for free the way app icons do. */
private fun elementCellShape(iconShape: IconShape): Shape = when (iconShape) {
    IconShape.CIRCLE -> CircleShape
    IconShape.ROUNDED_SQUARE -> RoundedCornerShape(16.dp)
}

@Composable
private fun EmptyGridSlotCell(onClick: () -> Unit, iconSizeScale: Float = 1f) {
    Column(modifier = Modifier.clickable(onClick = onClick).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp * iconSizeScale)
                .clip(elementCellShape(LocalIconShape.current))
                .background(UNSET_APP_SLOT_COLOR),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = unsetAppSlotIcon, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
internal fun AppIconCell(
    payload: String,
    loadIcon: (ComponentName) -> ImageBitmap?,
    resolveLabel: (ComponentName) -> String,
    showLabel: Boolean = true,
    labelColor: Color = Color.White,
    iconSizeScale: Float = 1f,
    onClickOverride: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val componentName = remember(payload) { ComponentName.unflattenFromString(payload) }
    val bitmap = rememberAppIcon(componentName, loadIcon)
    val label = componentName?.let(resolveLabel).orEmpty()
    // This app's own icon-size override if it has one, otherwise the flat scale passed in.
    val effectiveIconSizeScale = LocalIconSizeResolver.current?.invoke(payload) ?: iconSizeScale

    Column(
        modifier = Modifier
            .clickable(enabled = onClickOverride != null || componentName != null) {
                if (onClickOverride != null) onClickOverride() else componentName?.let { launchApp(context, it) }
            }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = label, modifier = Modifier.size(56.dp * effectiveIconSizeScale))
        } else {
            Spacer(
                modifier = Modifier
                    .size(56.dp * effectiveIconSizeScale)
                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            )
        }
        if (showLabel) {
            Spacer(Modifier.height(8.dp))
            GridLabel(label, color = labelColor)
        }
    }
}

@Composable
private fun ShortcutCell(payload: String, onClick: () -> Unit, showLabel: Boolean = true, labelColor: Color = Color.White, iconSizeScale: Float = 1f) {
    val label = remember(payload) { payload.split("|").getOrNull(2)?.takeIf { it.isNotBlank() } ?: payload.substringBefore('|') }
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.AutoMirrored.Filled.Shortcut, contentDescription = null, modifier = Modifier.size(56.dp * iconSizeScale))
        if (showLabel) {
            Spacer(Modifier.height(8.dp))
            GridLabel(label, color = labelColor)
        }
    }
}

@Composable
private fun MapTargetCell(target: SavedMapTarget?, showLabel: Boolean = true, labelColor: Color = Color.White, iconSizeScale: Float = 1f, onClickOverride: (() -> Unit)? = null) {
    val context = LocalContext.current
    val background = target?.backgroundColor?.let { Color(it) } ?: UNSET_APP_SLOT_COLOR
    val tint = target?.iconColor?.let { Color(it) } ?: Color.White
    val icon = PictogramCatalog.iconFor(target?.iconKey) ?: Icons.Filled.Place
    Column(
        modifier = Modifier
            .clickable(enabled = onClickOverride != null || target != null) {
                if (onClickOverride != null) onClickOverride() else target?.let { launchMapTarget(context, it) }
            }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(56.dp * iconSizeScale).clip(elementCellShape(LocalIconShape.current)).background(background),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp * iconSizeScale))
        }
        if (showLabel) {
            Spacer(Modifier.height(8.dp))
            GridLabel(target?.label.orEmpty(), color = labelColor)
        }
    }
}

@Composable
private fun WebsiteCell(website: PinnedWebsite?, showLabel: Boolean = true, labelColor: Color = Color.White, iconSizeScale: Float = 1f, onClickOverride: (() -> Unit)? = null) {
    val context = LocalContext.current
    val background = website?.backgroundColor?.let { Color(it) } ?: UNSET_APP_SLOT_COLOR
    val tint = website?.iconColor?.let { Color(it) } ?: Color.White
    val icon = PictogramCatalog.iconFor(website?.iconKey) ?: Icons.Filled.Language
    Column(
        modifier = Modifier
            .clickable(enabled = onClickOverride != null || website != null) {
                if (onClickOverride != null) onClickOverride() else website?.let { openWebsite(context, it) }
            }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(56.dp * iconSizeScale).clip(elementCellShape(LocalIconShape.current)).background(background),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp * iconSizeScale))
        }
        if (showLabel) {
            Spacer(Modifier.height(8.dp))
            GridLabel(website?.label.orEmpty(), color = labelColor)
        }
    }
}

@Composable
internal fun FolderCell(
    folder: Folder?,
    previewItems: List<FolderItem>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    loadPackIcon: (String, String) -> ImageBitmap?,
    onClick: () -> Unit,
    showLabel: Boolean = true,
    labelColor: Color = Color.White,
    iconSizeScale: Float = 1f
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FolderIcon(folder = folder, previewItems = previewItems, loadIcon = loadIcon, loadPackIcon = loadPackIcon, modifier = Modifier.size(56.dp * iconSizeScale))
        if (showLabel) {
            Spacer(Modifier.height(8.dp))
            GridLabel(folder?.name.orEmpty(), color = labelColor)
        }
    }
}

@Composable
private fun PageLinkCell(page: LauncherPage?, pages: List<LauncherPage>, onClick: () -> Unit, showLabel: Boolean = true, labelColor: Color = Color.White, iconSizeScale: Float = 1f) {
    val glyph = page?.iconGlyph?.let { name -> runCatching { PageIconGlyph.valueOf(name) }.getOrNull() }
        ?: page?.let { PageIconGlyph.forPageKind(it.kind) } ?: PageIconGlyph.WIDGET_PAGE
    val background = page?.iconBackgroundColor?.let { Color(it) } ?: com.bisc.flauncher.ui.home.dock.pageDefaultColor(page, glyph)
    val tint = page?.iconColor?.let { Color(it) } ?: Color.White
    Column(
        modifier = Modifier
            .clickable(enabled = page != null, onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(56.dp * iconSizeScale).clip(elementCellShape(LocalIconShape.current)).background(background),
            contentAlignment = Alignment.Center
        ) {
            PageIconGlyphImage(glyph = glyph, tint = tint, modifier = Modifier.size(30.dp * iconSizeScale))
        }
        if (showLabel) {
            Spacer(Modifier.height(8.dp))
            GridLabel(page?.let { pageLabel(it, pages) }.orEmpty(), color = labelColor)
        }
    }
}

@Composable
private fun ContactCell(
    contact: PinnedContact?,
    loadContactPhoto: suspend (Long, String?) -> ImageBitmap?,
    onClick: () -> Unit,
    showLabel: Boolean = true,
    labelColor: Color = Color.White,
    iconSizeScale: Float = 1f
) {
    Column(
        modifier = Modifier
            .clickable(enabled = contact != null, onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(56.dp * iconSizeScale), contentAlignment = Alignment.Center) {
            if (contact != null) {
                com.bisc.flauncher.ui.home.dock.ContactPhotoIcon(
                    contactId = contact.contactId,
                    photoUri = contact.photoUri,
                    loadPhoto = loadContactPhoto,
                    size = 56.dp * iconSizeScale
                )
            } else {
                Icon(imageVector = Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(56.dp * iconSizeScale))
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(8.dp))
            GridLabel(contact?.displayName.orEmpty(), color = labelColor)
        }
    }
}

@Composable
private fun ActionButtonCell(
    button: com.bisc.flauncher.data.model.ActionButton?,
    onClick: () -> Unit,
    showLabel: Boolean = true,
    labelColor: Color = Color.White,
    iconSizeScale: Float = 1f
) {
    val background = button?.backgroundColor?.let { Color(it) } ?: UNSET_APP_SLOT_COLOR
    val tint = button?.iconColor?.let { Color(it) } ?: Color.White
    val icon = PictogramCatalog.iconFor(button?.iconKey) ?: when (button?.actionType) {
        com.bisc.flauncher.data.model.ActionButtonActionType.PANEL -> Icons.Filled.Dashboard
        com.bisc.flauncher.data.model.ActionButtonActionType.DOCK -> Icons.Filled.Apps
        com.bisc.flauncher.data.model.ActionButtonActionType.ANDROID_SETTING -> Icons.Filled.Settings
        com.bisc.flauncher.data.model.ActionButtonActionType.LAUNCHER_SETTINGS -> Icons.Filled.Tune
        else -> Icons.Filled.ViewCarousel
    }
    Column(
        modifier = Modifier
            .clickable(enabled = button != null, onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(56.dp * iconSizeScale).clip(elementCellShape(LocalIconShape.current)).background(background),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp * iconSizeScale))
        }
        if (showLabel) {
            Spacer(Modifier.height(8.dp))
            GridLabel(button?.label.orEmpty(), color = labelColor)
        }
    }
}

/** Per-icon clip shape for a pill-style row - shared by [PillRow] and the Dock's own flush panel in
 *  HomeScreen.kt, which lays [PillRowEntries] out directly rather than through [PillRow]. */
@Composable
fun pillItemShape(iconShape: IconShape): Shape = when (iconShape) {
    IconShape.CIRCLE -> CircleShape
    IconShape.ROUNDED_SQUARE -> RoundedCornerShape(DockDimens.iconCornerRadius)
}

/**
 * A single non-wrapping line of entries (no per-cell text labels, uniform squircle buttons), for
 * any row with [PageRow.pillMode] set - the dock being the one instance
 * that floats outside the pager (see HomeScreen's floating dock Box, which calls this directly with
 * the home page's pinned SEARCH_BAR row), but any ordinary [PageRowType.ELEMENT_BOX] row can opt into the
 * same layout. It spans the full row width, with the fine standard margin or edge to edge per
 * [PageRow.edgeToEdge]. When [onAddEmptySlot]
 * is non-null, the row pads itself with tappable empty placeholders up to [PageRow.pillMaxEntries] - 
 * the dock's classic "tap an empty slot to fill it" invitation; an ordinary in-page pill row (edited
 * from its own Settings screen instead) passes null and simply shows however many real entries it has.
 */
@Composable
fun PillRow(
    row: PageRow,
    entries: List<RowEntry>,
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
    // Same width rule as every other in-page element: the full row width, either with the fine
    // standard margin or edge to edge (PageRow.edgeToEdge).
    val sideMargin = if (row.edgeToEdge) 0.dp else DockDimens.horizontalMargin
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = sideMargin,
                end = sideMargin,
                top = DockDimens.topMargin,
                bottom = DockDimens.bottomMargin
            ),
        contentAlignment = Alignment.Center
    ) {
        val itemShape = pillItemShape(iconShape)
        val barShape = remember(row.cornerRadiusPercent) {
            RoundedCornerShape(percent = row.cornerRadiusPercent.coerceIn(0, 100))
        }
        val barHeight = if (row.showLabels) DockDimens.barHeight + DockDimens.labelExtraHeight else DockDimens.barHeight

        Box(
            modifier = Modifier
                .fillMaxWidth()
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
 *  flush bottom panel in HomeScreen.kt - extracted so the panel can lay these out full-width inside
 *  its own [com.bisc.flauncher.ui.home.dock.DockPanelSurface] shell instead of [PillRow]'s pill-shaped
 *  bar Box. */
@Composable
fun PillRowEntries(
    entries: List<RowEntry>,
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
    pinnedContacts: List<PinnedContact> = emptyList(),
    loadContactPhoto: suspend (Long, String?) -> ImageBitmap? = { _, _ -> null },
    onContactClick: (Long) -> Unit = {},
    actionButtons: List<com.bisc.flauncher.data.model.ActionButton> = emptyList(),
    onActionButtonClick: (com.bisc.flauncher.data.model.ActionButton) -> Unit = {},
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
                            val background = page?.iconBackgroundColor?.let { Color(it) } ?: com.bisc.flauncher.ui.home.dock.pageDefaultColor(page, glyph)
                            val tint = page?.iconColor?.let { Color(it) } ?: Color.White
                            Box(
                                modifier = Modifier.fillMaxSize().background(background)
                                    .clickable(enabled = page != null) { page?.let { onOpenPageLink(it.id, it.kind) } },
                                contentAlignment = Alignment.Center
                            ) { PageIconGlyphImage(glyph = glyph, tint = tint, modifier = Modifier.size(DockDimens.iconSize * 0.55f)) }
                        }
                        GridItemType.CONTACT -> {
                            val contactId = remember(entry.payload) { entry.payload.toLongOrNull() }
                            val contact = pinnedContacts.find { it.contactId == contactId }
                            Box(
                                modifier = Modifier.fillMaxSize().clickable(enabled = contact != null) { contactId?.let(onContactClick) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (contact != null) {
                                    com.bisc.flauncher.ui.home.dock.ContactPhotoIcon(
                                        contactId = contact.contactId,
                                        photoUri = contact.photoUri,
                                        loadPhoto = loadContactPhoto,
                                        size = DockDimens.iconSize
                                    )
                                } else {
                                    Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = Color.White)
                                }
                            }
                        }
                        GridItemType.ACTION_BUTTON -> {
                            val buttonId = remember(entry.payload) { entry.payload.toLongOrNull() }
                            val button = actionButtons.find { it.id == buttonId }
                            val background = button?.backgroundColor?.let { Color(it) } ?: UNSET_APP_SLOT_COLOR
                            val tint = button?.iconColor?.let { Color(it) } ?: Color.White
                            val icon = PictogramCatalog.iconFor(button?.iconKey) ?: when (button?.actionType) {
                                com.bisc.flauncher.data.model.ActionButtonActionType.PANEL -> Icons.Filled.Dashboard
                                com.bisc.flauncher.data.model.ActionButtonActionType.DOCK -> Icons.Filled.Apps
                                com.bisc.flauncher.data.model.ActionButtonActionType.ANDROID_SETTING -> Icons.Filled.Settings
                                com.bisc.flauncher.data.model.ActionButtonActionType.LAUNCHER_SETTINGS -> Icons.Filled.Tune
                                else -> Icons.Filled.ViewCarousel
                            }
                            Box(
                                modifier = Modifier.fillMaxSize().background(background)
                                    .clickable(enabled = button != null) { button?.let(onActionButtonClick) },
                                contentAlignment = Alignment.Center
                            ) { Icon(imageVector = icon, contentDescription = null, tint = tint) }
                        }
                        GridItemType.WIDGET, GridItemType.SYSTEM_WIDGET -> Unit
                    }
                }
                if (showLabels) {
                    GridLabel(
                        text = entryLabel(entry, folders, savedMapTargets, pinnedWebsites, pages, pinnedContacts, resolveLabel, actionButtons),
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
        style = MaterialTheme.typography.labelSmall.copy(color = color)
    )
}

/** Resolves a display name for any [RowEntry] type - shared by [PillRowEntries]' optional
 *  captions (PageRow.showLabels) and ElementBoxGrid's own always-on per-cell labels. */
@Composable
internal fun entryLabel(
    entry: RowEntry,
    folders: List<Folder>,
    savedMapTargets: List<SavedMapTarget>,
    pinnedWebsites: List<PinnedWebsite>,
    pages: List<LauncherPage>,
    pinnedContacts: List<PinnedContact>,
    resolveLabel: (ComponentName) -> String,
    actionButtons: List<com.bisc.flauncher.data.model.ActionButton> = emptyList()
): String = when (entry.type) {
    GridItemType.APP -> ComponentName.unflattenFromString(entry.payload)?.let(resolveLabel).orEmpty()
    GridItemType.FOLDER -> folders.find { it.id == entry.payload.toLongOrNull() }?.name.orEmpty()
    GridItemType.SHORTCUT -> entry.payload.split("|").getOrNull(2)?.takeIf { it.isNotBlank() } ?: entry.payload.substringBefore('|')
    GridItemType.MAP_TARGET -> savedMapTargets.find { it.id == entry.payload.toLongOrNull() }?.label.orEmpty()
    GridItemType.WEBSITE -> pinnedWebsites.find { it.id == entry.payload.toLongOrNull() }?.label.orEmpty()
    GridItemType.PAGE -> pages.find { it.id == entry.payload.toLongOrNull() }?.let { pageLabel(it, pages) }.orEmpty()
    GridItemType.CONTACT -> pinnedContacts.find { it.contactId == entry.payload.toLongOrNull() }?.displayName.orEmpty()
    GridItemType.ACTION_BUTTON -> actionButtons.find { it.id == entry.payload.toLongOrNull() }?.label.orEmpty()
    GridItemType.WIDGET, GridItemType.SYSTEM_WIDGET -> ""
}
