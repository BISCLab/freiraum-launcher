package com.bisc.flauncher.ui.home.dock

import com.bisc.flauncher.ui.home.NoteReadContent
import com.bisc.flauncher.ui.home.rememberAppIcon
import android.Manifest
import android.content.ComponentName
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Shape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.ui.home.ConfirmRemoveDialog
import com.bisc.flauncher.ui.home.NoteEditIconButton
import com.bisc.flauncher.data.model.AppInfo
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.Folder
import com.bisc.flauncher.data.model.FolderItem
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.data.model.Note
import com.bisc.flauncher.data.model.NoteType
import com.bisc.flauncher.data.model.checklistItemsToNoteContent
import com.bisc.flauncher.data.model.noteContentToChecklistItems
import com.bisc.flauncher.data.model.PinnedContact
import com.bisc.flauncher.data.model.PinnedShortcut
import com.bisc.flauncher.data.model.PinnedWebsite
import com.bisc.flauncher.data.model.SavedMapTarget
import com.bisc.flauncher.data.model.SidebarCategoryViewMode
import com.bisc.flauncher.data.model.SidebarPinnedApp
import com.bisc.flauncher.ui.pageLabel
import com.bisc.flauncher.ui.home.FolderIcon
import com.bisc.flauncher.ui.home.NoteContentEditor
import com.bisc.flauncher.ui.home.NoteTypeToggle
import com.bisc.flauncher.ui.home.search.fadeBackgroundToBottom
import com.bisc.flauncher.ui.home.search.fadeListTop
import com.bisc.flauncher.ui.theme.noRippleClickable
import kotlinx.coroutines.delay

// Matches the 20.dp horizontal inset every list row applies to its own content, so the gap to the
// top edge reads the same as the gap to the left edge instead of looking cramped above/below.
private val OVERVIEW_LIST_CONTENT_PADDING = PaddingValues(vertical = 20.dp)
private val OVERVIEW_ROW_HEIGHT_MIN = 48.dp
private const val OVERVIEW_GRID_COLUMNS = 5
private val OVERVIEW_LIST_ICON_SIZE = 40.dp

/** Grid mode uses exactly the dock's own icon size; list mode gets a smaller, but still roomier, size. */
private fun overviewIconSize(viewMode: SidebarCategoryViewMode, dock: ScaledDock) =
    if (viewMode == SidebarCategoryViewMode.GRID) dock.iconSize else OVERVIEW_LIST_ICON_SIZE

internal fun overviewRowHeight(availableHeight: Dp, appCount: Int): Dp {
    if (appCount <= 0) return OVERVIEW_ROW_HEIGHT_MIN
    val verticalPadding = OVERVIEW_LIST_CONTENT_PADDING.calculateTopPadding() + OVERVIEW_LIST_CONTENT_PADDING.calculateBottomPadding()
    return ((availableHeight - verticalPadding) / appCount).coerceAtLeast(OVERVIEW_ROW_HEIGHT_MIN)
}

/**
 * Discreet Launcher's actual favorites popup is dead simple: just a grid, nothing else — no title, no
 * settings shortcut, no inline "add"/"create" row anywhere. Managing favorites, creating folders and
 * launcher settings in general all happen through Settings -> Quick overview (folder creation already
 * lived there, `showCreateFolderDialog` in SettingsSidebarScreen.kt) rather than inline in this popup.
 *
 * Shows pinned apps and *every* folder (folders aren't individually "pinnable" the way apps are — they're
 * a global concept, same as the old multi-category Sidebar's own Folders tab just listed all of them) in
 * one merged grid, apps first. [showGroupDividers] draws a thin line between the two groups when both are
 * non-empty; off by default. The older multi-category rail panel this replaced (`SidebarOverviewPanel`,
 * `Sidebar.kt`/`SidebarLayout.kt`) had zero live call sites left and was removed outright — recoverable
 * from git history if a "browse everything" surface like it is wanted again later.
 *
 * [topContentInset] reserves space for the status bar/camera cutout, passed in rather than computed with
 * [androidx.compose.foundation.layout.statusBarsPadding] here — that resolves to zero the instant the
 * status bar is hidden (the home screen's default), which left content starting flush under the camera
 * cutout. The caller already tracks a *cached* max(status bar, cutout) inset that survives hiding.
 *
 * No border on the top/left/right edges ever — [appearance] is always rendered with borderEnabled
 * forced off regardless of what's stored, since a border drawn by [DockPanelSurface] would trace all four
 * sides of [shape] and there's no way to ask it for just one. [showBottomBorder] draws a separate,
 * deliberately bottom-only line instead, off by default.
 */
@Composable
fun FavoritesPopup(
    installedApps: List<AppInfo>,
    pinnedApps: List<SidebarPinnedApp>,
    folders: List<Folder>,
    folderItemsByFolder: Map<Long, List<FolderItem>>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    loadPackIcon: (String, String) -> ImageBitmap?,
    onAppClick: (AppInfo) -> Unit,
    onFolderClick: (Folder) -> Unit,
    onRemoveApp: (SidebarPinnedApp) -> Unit,
    onOpenSettings: () -> Unit,
    appearance: ElementAppearance,
    shape: Shape,
    dock: ScaledDock,
    topContentInset: Dp = 0.dp,
    showBottomBorder: Boolean = false,
    showGroupDividers: Boolean = false,
    modifier: Modifier = Modifier
) {
    val borderColor = resolveElementColors(appearance).borderColor
    DockPanelSurface(
        appearance = appearance.copy(borderEnabled = false),
        shape = shape,
        modifier = modifier.then(
            if (showBottomBorder) {
                Modifier.drawBehind {
                    drawLine(borderColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                }
            } else {
                Modifier
            }
        )
    ) {
        val appsByComponent = remember(installedApps) { installedApps.associateBy { it.componentName } }
        val iconSize = dock.iconSize
        val appEntries = remember(pinnedApps, appsByComponent) {
            pinnedApps.map { pinned ->
                val componentName = ComponentName.unflattenFromString(pinned.componentName)
                val app = componentName?.let { appsByComponent[it] }
                CategoryEntry(
                    key = "app_${pinned.id}",
                    label = app?.label ?: pinned.componentName,
                    onClick = { app?.let(onAppClick) },
                    onRemove = { onRemoveApp(pinned) },
                    icon = {
                        val bitmap = rememberAppIcon(componentName, loadIcon)
                        if (bitmap != null) Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(iconSize))
                    }
                )
            }
        }
        val folderEntries = remember(folders, folderItemsByFolder) {
            folders.map { folder ->
                CategoryEntry(
                    key = "folder_${folder.id}",
                    label = folder.name,
                    onClick = { onFolderClick(folder) },
                    icon = {
                        FolderIcon(
                            folder = folder,
                            previewItems = folderItemsByFolder[folder.id].orEmpty(),
                            loadIcon = loadIcon,
                            loadPackIcon = loadPackIcon,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                )
            }
        }
        FavoritesMergedGrid(
            groups = listOf(appEntries, folderEntries),
            cellSize = iconSize,
            showDividers = showGroupDividers,
            onOpenSettings = onOpenSettings,
            modifier = Modifier.fillMaxSize().padding(top = topContentInset)
        )
    }
}

/**
 * Apps and folders laid out as one continuous grid rather than [CategoryGrid]'s per-category LazyColumn
 * (nesting two of those here would mean two independently-scrolling, unboundedheight lists competing for
 * space — not what "one grid" means). [LazyVerticalGrid] handles this cleanly: each group's entries are
 * regular grid items, and a full-width divider between non-empty groups is just one more item spanning
 * every column via [GridItemSpan]. Empty groups contribute nothing, including no stray divider. A
 * full-width "Settings" row is appended after everything else — this popup has no header or inline
 * add-row of its own (see [FavoritesPopup]'s doc comment), so this is the only way back into Settings
 * from the home screen; it scrolls with the rest of the grid rather than floating separately.
 *
 * The grid already fills whatever height its caller gives it (see the panel-height estimate in
 * HomeScreen.kt) and is natively scrollable; what it was missing was any visual cue that there's more
 * to scroll to — [fadeListTop]/[fadeBackgroundToBottom] (the same fade treatment [BrowseAppList] uses)
 * fixes that instead of an abrupt hard clip at the bottom edge.
 */
@Composable
private fun FavoritesMergedGrid(
    groups: List<List<CategoryEntry>>,
    cellSize: Dp,
    showDividers: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingRemoval by remember { mutableStateOf<CategoryEntry?>(null) }
    val nonEmptyGroups = remember(groups) { groups.filter { it.isNotEmpty() } }
    val gridState = rememberLazyGridState()
    val canScrollDown by remember { derivedStateOf { gridState.canScrollForward } }
    val topFadeHeight = 48.dp
    val topFadePx = with(LocalDensity.current) { topFadeHeight.toPx() }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(OVERVIEW_GRID_COLUMNS),
        modifier = modifier
            .then(if (canScrollDown) Modifier.fadeBackgroundToBottom(0.86f, 1f) else Modifier)
            .fadeListTop(topFadeHeight) {
                if (gridState.firstVisibleItemIndex > 0) 1f else (gridState.firstVisibleItemScrollOffset / topFadePx).coerceIn(0f, 1f)
            },
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        nonEmptyGroups.forEachIndexed { groupIndex, group ->
            gridItems(group, key = { it.key }) { entry ->
                CategoryGridCell(entry, cellSize, onRequestRemove = { pendingRemoval = entry })
            }
            if (showDividers && groupIndex < nonEmptyGroups.lastIndex) {
                item(key = "divider_$groupIndex", span = { GridItemSpan(maxLineSpan) }) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = LocalContentColor.current.copy(alpha = 0.15f)
                    )
                }
            }
        }
        item(key = "open_settings", span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .noRippleClickable(onClick = onOpenSettings),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.shortcut_settings))
            }
        }
    }
    pendingRemoval?.let { entry ->
        ConfirmRemoveDialog(
            label = entry.label,
            onConfirm = { entry.onRemove?.invoke(); pendingRemoval = null },
            onDismiss = { pendingRemoval = null }
        )
    }
}

/** The category content shown when a sidebar category (Apps, Folders, Notes, …) is drilled into. */
@Composable
fun OverviewContent(
    entry: SidebarEntryType,
    pages: List<LauncherPage>,
    installedApps: List<AppInfo>,
    sidebarPinnedApps: List<SidebarPinnedApp>,
    folders: List<Folder>,
    folderItemsByFolder: Map<Long, List<FolderItem>>,
    notes: List<Note>,
    savedMapTargets: List<SavedMapTarget>,
    pinnedWebsites: List<PinnedWebsite>,
    pinnedShortcuts: List<PinnedShortcut>,
    hasShortcutPermission: Boolean,
    rowHeight: Dp,
    viewMode: SidebarCategoryViewMode,
    showAddRow: Boolean,
    loadIcon: (ComponentName) -> ImageBitmap?,
    loadPackIcon: (String, String) -> ImageBitmap? = { _, _ -> null },
    onAppClick: (AppInfo) -> Unit,
    onPageClick: (LauncherPage) -> Unit,
    onFolderClick: (Folder) -> Unit,
    onAddFolderClick: () -> Unit,
    onSaveNote: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onCreateNote: ((Long) -> Unit) -> Unit,
    onReorderNotes: (List<Note>) -> Unit,
    onMapTargetClick: (SavedMapTarget) -> Unit,
    onRemoveMapTarget: (SavedMapTarget) -> Unit,
    onAddMapTargetClick: () -> Unit,
    onWebsiteClick: (PinnedWebsite) -> Unit,
    onRemoveWebsite: (PinnedWebsite) -> Unit,
    onAddWebsiteClick: () -> Unit,
    onShortcutClick: (PinnedShortcut) -> Unit,
    onRemoveShortcut: (PinnedShortcut) -> Unit,
    onAddShortcutClick: () -> Unit,
    onAddSidebarAppsClick: () -> Unit,
    onRemoveSidebarApp: (SidebarPinnedApp) -> Unit,
    pinnedContacts: List<PinnedContact>,
    hasContactsPermission: Boolean,
    onContactClick: (PinnedContact) -> Unit,
    onRemoveContact: (PinnedContact) -> Unit,
    onAddContactClick: () -> Unit,
    onContactsPermissionResult: (Boolean) -> Unit,
    loadContactPhoto: suspend (Long, String?) -> ImageBitmap?,
    dock: ScaledDock
) {
    when (entry) {
        SidebarEntryType.APPS -> SidebarAppsOverviewContent(
            installedApps, sidebarPinnedApps, loadIcon, onAppClick, onAddSidebarAppsClick, onRemoveSidebarApp, viewMode, showAddRow, rowHeight, dock
        )
        SidebarEntryType.PAGES -> PagesOverviewContent(pages, onPageClick, rowHeight)
        SidebarEntryType.FOLDERS -> FoldersOverviewContent(
            folders, folderItemsByFolder, loadIcon, loadPackIcon, onFolderClick, onAddFolderClick, viewMode, showAddRow, rowHeight, dock
        )
        SidebarEntryType.NOTES -> NotesOverviewContent(notes, onSaveNote, onDeleteNote, onCreateNote, onReorderNotes, rowHeight)
        SidebarEntryType.WEBSITES -> WebsitesOverviewContent(
            pinnedWebsites, onWebsiteClick, onRemoveWebsite, onAddWebsiteClick, viewMode, showAddRow, rowHeight, dock
        )
        SidebarEntryType.MAPS -> MapsOverviewContent(
            savedMapTargets, onMapTargetClick, onRemoveMapTarget, onAddMapTargetClick, viewMode, showAddRow, rowHeight, dock
        )
        SidebarEntryType.SHORTCUTS -> ShortcutsOverviewContent(
            pinnedShortcuts, hasShortcutPermission, onShortcutClick, onRemoveShortcut, onAddShortcutClick, viewMode, showAddRow, rowHeight, dock
        )
        SidebarEntryType.CONTACTS -> ContactsOverviewContent(
            pinnedContacts, hasContactsPermission, onContactClick, onRemoveContact, onAddContactClick, onContactsPermissionResult,
            loadContactPhoto, viewMode, showAddRow, rowHeight, dock
        )
    }
}

@Composable
private fun WholeRowsLazyColumn(
    rowHeight: Dp,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = OVERVIEW_LIST_CONTENT_PADDING,
    content: LazyListScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val verticalPadding = contentPadding.calculateTopPadding() + contentPadding.calculateBottomPadding()
        val wholeRows = ((maxHeight - verticalPadding) / rowHeight).toInt().coerceAtLeast(0)
        val cappedHeight = (verticalPadding + rowHeight * wholeRows).coerceAtMost(maxHeight)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(cappedHeight),
            contentPadding = contentPadding,
            content = content
        )
    }
}

@Composable
private fun SidebarAppsOverviewContent(
    installedApps: List<AppInfo>,
    pinnedApps: List<SidebarPinnedApp>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    onAppClick: (AppInfo) -> Unit,
    onAddAppsClick: () -> Unit,
    onRemove: (SidebarPinnedApp) -> Unit,
    viewMode: SidebarCategoryViewMode,
    showAddRow: Boolean,
    rowHeight: Dp,
    dock: ScaledDock
) {
    val appsByComponent = remember(installedApps) { installedApps.associateBy { it.componentName } }
    val onAppClickLatest by rememberUpdatedState(onAppClick)
    val onRemoveLatest by rememberUpdatedState(onRemove)
    val loadIconLatest by rememberUpdatedState(loadIcon)
    val iconSize = overviewIconSize(viewMode, dock)
    val entries = remember(pinnedApps, appsByComponent, iconSize) {
        pinnedApps.map { pinned ->
            val componentName = ComponentName.unflattenFromString(pinned.componentName)
            val app = componentName?.let { appsByComponent[it] }
            CategoryEntry(
                key = pinned.id,
                label = app?.label ?: pinned.componentName,
                onClick = { app?.let(onAppClickLatest) },
                onRemove = { onRemoveLatest(pinned) },
                icon = {
                    val bitmap = rememberAppIcon(componentName, loadIconLatest)
                    if (bitmap != null) {
                        Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(iconSize))
                    }
                }
            )
        }
    }
    CategoryOverviewList(
        entries = entries,
        rowHeight = rowHeight,
        viewMode = viewMode,
        cellSize = iconSize,
        emptyLabelRes = R.string.sidebar_apps_empty,
        addLabelRes = R.string.sidebar_apps_add.takeIf { showAddRow },
        onAddClick = onAddAppsClick.takeIf { showAddRow }
    )
}

@Composable
private fun PagesOverviewContent(pages: List<LauncherPage>, onPageClick: (LauncherPage) -> Unit, rowHeight: Dp) {
    WholeRowsLazyColumn(rowHeight = rowHeight) {
        items(pages, key = { it.id }) { page ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .noRippleClickable { onPageClick(page) }
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = pageLabel(page, pages), color = LocalContentColor.current.copy(alpha = 0.87f))
            }
        }
    }
}

@Composable
private fun FoldersOverviewContent(
    folders: List<Folder>,
    folderItemsByFolder: Map<Long, List<FolderItem>>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    loadPackIcon: (String, String) -> ImageBitmap?,
    onFolderClick: (Folder) -> Unit,
    onAddFolderClick: () -> Unit,
    viewMode: SidebarCategoryViewMode,
    showAddRow: Boolean,
    rowHeight: Dp,
    dock: ScaledDock
) {
    val onFolderClickLatest by rememberUpdatedState(onFolderClick)
    val loadIconLatest by rememberUpdatedState(loadIcon)
    val loadPackIconLatest by rememberUpdatedState(loadPackIcon)
    val iconSize = overviewIconSize(viewMode, dock)
    val entries = remember(folders, folderItemsByFolder, iconSize) {
        folders.map { folder ->
            CategoryEntry(
                key = folder.id,
                label = folder.name,
                onClick = { onFolderClickLatest(folder) },
                icon = {
                    FolderIcon(
                        folder = folder,
                        previewItems = folderItemsByFolder[folder.id].orEmpty(),
                        loadIcon = loadIconLatest,
                        loadPackIcon = loadPackIconLatest,
                        modifier = Modifier.size(iconSize)
                    )
                }
            )
        }
    }
    CategoryOverviewList(
        entries = entries,
        rowHeight = rowHeight,
        viewMode = viewMode,
        cellSize = iconSize,
        emptyLabelRes = R.string.folder_overview_empty,
        addLabelRes = R.string.folder_add.takeIf { showAddRow },
        onAddClick = onAddFolderClick.takeIf { showAddRow }
    )
}

@Composable
private fun NotesOverviewContent(
    notes: List<Note>,
    onSaveNote: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onCreateNote: ((Long) -> Unit) -> Unit,
    onReorderNotes: (List<Note>) -> Unit,
    rowHeight: Dp
) {
    var expandedNoteId by remember { mutableStateOf<Long?>(null) }
    var editingNoteId by remember { mutableStateOf<Long?>(null) }
    var menuForNoteId by remember { mutableStateOf<Long?>(null) }
    var movingNoteId by remember { mutableStateOf<Long?>(null) }

    fun moveBy(delta: Int) {
        val from = notes.indexOfFirst { it.id == movingNoteId }
        val to = (from + delta).coerceIn(0, notes.lastIndex)
        if (from < 0 || from == to) return
        val reordered = notes.toMutableList().apply { add(to, removeAt(from)) }
        onReorderNotes(reordered)
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        contentPadding = OVERVIEW_LIST_CONTENT_PADDING
    ) {
        if (notes.isEmpty()) {
            item(key = "empty") {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.sidebar_notes_empty), color = LocalContentColor.current.copy(alpha = 0.5f))
                }
            }
        }
        items(notes, key = { "note_${it.id}" }) { note ->
            NoteOverviewItem(
                note = note,
                expanded = expandedNoteId == note.id,
                editing = editingNoteId == note.id,
                selected = note.id == movingNoteId,
                menuExpanded = note.id == menuForNoteId,
                onToggleExpand = {
                    editingNoteId = null
                    expandedNoteId = if (expandedNoteId == note.id) null else note.id
                },
                onEdit = { editingNoteId = note.id },
                onDoneEditing = { editingNoteId = null },
                onSave = onSaveNote,
                onDelete = {
                    onDeleteNote(note)
                    if (expandedNoteId == note.id) expandedNoteId = null
                    if (editingNoteId == note.id) editingNoteId = null
                },
                onLongClick = { menuForNoteId = note.id },
                onDismissMenu = { menuForNoteId = null },
                onRequestMove = { menuForNoteId = null; movingNoteId = note.id },
                rowHeight = rowHeight
            )
        }
        if (movingNoteId != null) {
            item(key = "move_controls") {
                Row(
                    modifier = Modifier.fillMaxWidth().height(rowHeight).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { moveBy(-1) }) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.folder_move_up))
                    }
                    Text(
                        stringResource(R.string.folder_move_hint),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    TextButton(onClick = { movingNoteId = null }) { Text(stringResource(R.string.folder_move_done)) }
                    IconButton(onClick = { moveBy(1) }) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.folder_move_down))
                    }
                }
            }
        } else {
            item(key = "add_note") {
                AddRow(
                    labelRes = R.string.sidebar_notes_add,
                    onClick = { onCreateNote { id -> expandedNoteId = id; editingNoteId = id } },
                    rowHeight = rowHeight
                )
            }
        }
    }
}

@Composable
private fun NoteOverviewItem(
    note: Note,
    expanded: Boolean,
    editing: Boolean,
    selected: Boolean,
    menuExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onDoneEditing: () -> Unit,
    onSave: (Note) -> Unit,
    onDelete: () -> Unit,
    onLongClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onRequestMove: () -> Unit,
    rowHeight: Dp
) {
    var confirmingDelete by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Box {
            DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.folder_item_move)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null) },
                    onClick = onRequestMove
                )
                DropdownMenuItem(
                    text = { Text(stringResource(if (note.hiddenInWidget) R.string.widget_notes_show_in_widget else R.string.widget_notes_hide)) },
                    leadingIcon = { Icon(if (note.hiddenInWidget) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null) },
                    onClick = { onDismissMenu(); onSave(note.copy(hiddenInWidget = !note.hiddenInWidget)) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.folder_item_remove)) },
                    leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
                    onClick = { onDismissMenu(); confirmingDelete = true }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .then(if (selected) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) else Modifier)
                .combinedClickable(onClick = onToggleExpand, onLongClick = onLongClick)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (note.type == NoteType.CHECKLISTE) Icons.Filled.Checklist else Icons.Filled.EditNote,
                contentDescription = null,
                tint = LocalContentColor.current.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp)
            )
            Text(
                note.title.ifBlank { stringResource(R.string.widget_notes_untitled) },
                color = LocalContentColor.current.copy(alpha = 0.87f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp).weight(1f)
            )
            if (note.type == NoteType.CHECKLISTE && note.items.isNotEmpty()) {
                Text(
                    stringResource(R.string.widget_notes_checklist_progress, note.items.count { it.checked }, note.items.size),
                    color = LocalContentColor.current.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (expanded && !editing) NoteEditIconButton(onEdit)
        }
        if (expanded && editing) {
            NoteInlineEditor(note = note, onSave = onSave)
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDoneEditing) { Text(stringResource(R.string.widget_notes_done)) }
            }
        } else if (expanded) {
            NoteReadContent(
                note = note,
                onToggleItem = { index ->
                    onSave(note.copy(items = note.items.toMutableList().also { it[index] = it[index].copy(checked = !it[index].checked) }))
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                showTitle = false
            )
        }
    }
    if (confirmingDelete) {
        ConfirmRemoveDialog(
            label = note.title.ifBlank { stringResource(R.string.widget_notes_untitled) },
            onConfirm = { confirmingDelete = false; onDelete() },
            onDismiss = { confirmingDelete = false }
        )
    }
}

@Composable
private fun NoteInlineEditor(note: Note, onSave: (Note) -> Unit) {
    var dirty by remember(note.id) { mutableStateOf(false) }
    var title by remember(note.id) { mutableStateOf(note.title) }
    var type by remember(note.id) { mutableStateOf(note.type) }
    var content by remember(note.id) { mutableStateOf(note.content) }
    var items by remember(note.id) { mutableStateOf(note.items) }

    LaunchedEffect(title, type, content, items) {
        if (!dirty) return@LaunchedEffect
        delay(400)
        onSave(note.copy(title = title, type = type, content = content, items = items, updatedAt = System.currentTimeMillis()))
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it; dirty = true },
            label = { Text(stringResource(R.string.widget_notes_title_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        NoteTypeToggle(
            type = type,
            onTypeSelected = { selected ->
                if (selected != type) {
                    if (selected == NoteType.CHECKLISTE) items = noteContentToChecklistItems(content)
                    else content = checklistItemsToNoteContent(items)
                    type = selected
                    dirty = true
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        )
        NoteContentEditor(
            type = type,
            content = content,
            items = items,
            onContentChange = { content = it; dirty = true },
            onItemsChange = { items = it; dirty = true },
            contentMinHeight = 72.dp
        )
    }
}

@Composable
private fun WebsitesOverviewContent(
    websites: List<PinnedWebsite>,
    onWebsiteClick: (PinnedWebsite) -> Unit,
    onRemoveWebsite: (PinnedWebsite) -> Unit,
    onAddWebsiteClick: () -> Unit,
    viewMode: SidebarCategoryViewMode,
    showAddRow: Boolean,
    rowHeight: Dp,
    dock: ScaledDock
) {
    val onClickLatest by rememberUpdatedState(onWebsiteClick)
    val onRemoveLatest by rememberUpdatedState(onRemoveWebsite)
    val iconSize = overviewIconSize(viewMode, dock)
    val entries = remember(websites, iconSize) {
        websites.map { website ->
            CategoryEntry(
                key = "site_${website.id}",
                label = website.label,
                onClick = { onClickLatest(website) },
                onRemove = { onRemoveLatest(website) },
                icon = { Icon(Icons.Filled.Language, contentDescription = null, tint = LocalContentColor.current.copy(alpha = 0.7f), modifier = Modifier.size(iconSize)) }
            )
        }
    }
    CategoryOverviewList(
        entries = entries,
        rowHeight = rowHeight,
        viewMode = viewMode,
        cellSize = iconSize,
        emptyLabelRes = R.string.sidebar_websites_empty,
        addLabelRes = R.string.sidebar_website_add.takeIf { showAddRow },
        onAddClick = onAddWebsiteClick.takeIf { showAddRow }
    )
}

@Composable
private fun MapsOverviewContent(
    targets: List<SavedMapTarget>,
    onTargetClick: (SavedMapTarget) -> Unit,
    onRemoveTarget: (SavedMapTarget) -> Unit,
    onAddClick: () -> Unit,
    viewMode: SidebarCategoryViewMode,
    showAddRow: Boolean,
    rowHeight: Dp,
    dock: ScaledDock
) {
    val onClickLatest by rememberUpdatedState(onTargetClick)
    val onRemoveLatest by rememberUpdatedState(onRemoveTarget)
    val iconSize = overviewIconSize(viewMode, dock)
    val entries = remember(targets, iconSize) {
        targets.map { target ->
            CategoryEntry(
                key = target.id,
                label = target.label,
                onClick = { onClickLatest(target) },
                onRemove = { onRemoveLatest(target) },
                icon = { Icon(Icons.Filled.Place, contentDescription = null, tint = LocalContentColor.current.copy(alpha = 0.7f), modifier = Modifier.size(iconSize)) }
            )
        }
    }
    CategoryOverviewList(
        entries = entries,
        rowHeight = rowHeight,
        viewMode = viewMode,
        cellSize = iconSize,
        emptyLabelRes = R.string.sidebar_maps_empty,
        addLabelRes = R.string.sidebar_maps_add.takeIf { showAddRow },
        onAddClick = onAddClick.takeIf { showAddRow }
    )
}

@Composable
private fun ShortcutsOverviewContent(
    shortcuts: List<PinnedShortcut>,
    hasPermission: Boolean,
    onShortcutClick: (PinnedShortcut) -> Unit,
    onRemoveShortcut: (PinnedShortcut) -> Unit,
    onAddClick: () -> Unit,
    viewMode: SidebarCategoryViewMode,
    showAddRow: Boolean,
    rowHeight: Dp,
    dock: ScaledDock
) {
    if (!hasPermission) {
        WholeRowsLazyColumn(rowHeight = rowHeight) {
            item(key = "no_permission") {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.sidebar_shortcuts_needs_default),
                        color = LocalContentColor.current.copy(alpha = 0.5f)
                    )
                }
            }
        }
        return
    }
    val onClickLatest by rememberUpdatedState(onShortcutClick)
    val onRemoveLatest by rememberUpdatedState(onRemoveShortcut)
    val iconSize = overviewIconSize(viewMode, dock)
    val entries = remember(shortcuts, iconSize) {
        shortcuts.map { shortcut ->
            CategoryEntry(
                key = shortcut.id,
                label = shortcut.label,
                onClick = { onClickLatest(shortcut) },
                onRemove = { onRemoveLatest(shortcut) },
                icon = { Icon(Icons.AutoMirrored.Filled.Shortcut, contentDescription = null, tint = LocalContentColor.current.copy(alpha = 0.7f), modifier = Modifier.size(iconSize)) }
            )
        }
    }
    CategoryOverviewList(
        entries = entries,
        rowHeight = rowHeight,
        viewMode = viewMode,
        cellSize = iconSize,
        emptyLabelRes = R.string.sidebar_shortcuts_empty,
        addLabelRes = R.string.sidebar_shortcuts_add.takeIf { showAddRow },
        onAddClick = onAddClick.takeIf { showAddRow }
    )
}

@Composable
private fun ContactsOverviewContent(
    contacts: List<PinnedContact>,
    hasPermission: Boolean,
    onContactClick: (PinnedContact) -> Unit,
    onRemoveContact: (PinnedContact) -> Unit,
    onAddContactClick: () -> Unit,
    onPermissionResult: (Boolean) -> Unit,
    loadContactPhoto: suspend (Long, String?) -> ImageBitmap?,
    viewMode: SidebarCategoryViewMode,
    showAddRow: Boolean,
    rowHeight: Dp,
    dock: ScaledDock
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> onPermissionResult(granted) }

    if (!hasPermission) {
        WholeRowsLazyColumn(rowHeight = rowHeight) {
            item(key = "no_permission") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.sidebar_contacts_needs_permission),
                        color = LocalContentColor.current.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }) {
                        Text(stringResource(R.string.sidebar_contacts_allow_access))
                    }
                }
            }
        }
        return
    }
    val onClickLatest by rememberUpdatedState(onContactClick)
    val onRemoveLatest by rememberUpdatedState(onRemoveContact)
    val loadPhotoLatest by rememberUpdatedState(loadContactPhoto)
    val iconSize = overviewIconSize(viewMode, dock)
    val entries = remember(contacts, iconSize) {
        contacts.map { contact ->
            CategoryEntry(
                key = contact.id,
                label = contact.displayName,
                onClick = { onClickLatest(contact) },
                onRemove = { onRemoveLatest(contact) },
                icon = {
                    ContactPhotoIcon(
                        contactId = contact.contactId,
                        photoUri = contact.photoUri,
                        loadPhoto = { id, uri -> loadPhotoLatest(id, uri) },
                        size = iconSize
                    )
                }
            )
        }
    }
    CategoryOverviewList(
        entries = entries,
        rowHeight = rowHeight,
        viewMode = viewMode,
        cellSize = iconSize,
        emptyLabelRes = R.string.sidebar_contacts_empty,
        addLabelRes = R.string.sidebar_contacts_add.takeIf { showAddRow },
        onAddClick = onAddContactClick.takeIf { showAddRow }
    )
}

/** Contact photo, loaded lazily. */
@Composable
fun ContactPhotoIcon(contactId: Long, photoUri: String?, loadPhoto: suspend (Long, String?) -> ImageBitmap?, size: Dp = 28.dp) {
    var photo by remember(contactId) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(contactId, photoUri) { photo = loadPhoto(contactId, photoUri) }
    val loaded = photo
    if (loaded != null) {
        Image(bitmap = loaded, contentDescription = null, modifier = Modifier.size(size).clip(CircleShape))
    } else {
        Icon(Icons.Filled.Person, contentDescription = null, tint = LocalContentColor.current.copy(alpha = 0.7f), modifier = Modifier.size(size))
    }
}

private data class CategoryEntry(
    val key: Any,
    val label: String,
    val onClick: () -> Unit,
    val icon: @Composable () -> Unit,
    val onRemove: (() -> Unit)? = null
)

@Composable
private fun CategoryOverviewList(
    entries: List<CategoryEntry>,
    rowHeight: Dp,
    viewMode: SidebarCategoryViewMode,
    cellSize: Dp,
    emptyLabelRes: Int?,
    addLabelRes: Int?,
    onAddClick: (() -> Unit)?
) {
    // Long-press removal asks first, instead of the item just disappearing.
    var pendingRemoval by remember { mutableStateOf<CategoryEntry?>(null) }
    when (viewMode) {
        SidebarCategoryViewMode.LIST -> WholeRowsLazyColumn(rowHeight = rowHeight) {
            if (entries.isEmpty() && emptyLabelRes != null) {
                item(key = "empty") {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(emptyLabelRes), color = LocalContentColor.current.copy(alpha = 0.5f))
                    }
                }
            }
            items(entries, key = { it.key }) { entry ->
                CategoryListRow(entry, rowHeight, onRequestRemove = { pendingRemoval = entry })
            }
            if (addLabelRes != null && onAddClick != null) {
                item(key = "add") { AddRow(labelRes = addLabelRes, onClick = onAddClick, rowHeight = rowHeight) }
            }
        }
        SidebarCategoryViewMode.GRID -> CategoryGrid(entries, cellSize, emptyLabelRes, addLabelRes, onAddClick) { entry -> pendingRemoval = entry }
    }
    pendingRemoval?.let { entry ->
        ConfirmRemoveDialog(
            label = entry.label,
            onConfirm = { entry.onRemove?.invoke(); pendingRemoval = null },
            onDismiss = { pendingRemoval = null }
        )
    }
}

@Composable
private fun CategoryListRow(entry: CategoryEntry, rowHeight: Dp, onRequestRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .combinedClickable(onClick = entry.onClick, onLongClick = entry.onRemove?.let { { onRequestRemove() } })
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        entry.icon()
        Text(
            entry.label,
            color = LocalContentColor.current.copy(alpha = 0.87f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp).weight(1f)
        )
    }
}

@Composable
private fun CategoryGrid(
    entries: List<CategoryEntry>,
    cellSize: Dp,
    emptyLabelRes: Int?,
    addLabelRes: Int?,
    onAddClick: (() -> Unit)?,
    onRequestRemove: (CategoryEntry) -> Unit
) {
    val rows = entries.chunked(OVERVIEW_GRID_COLUMNS)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // The gap between cells and from the outer edges to the first/last cell is the same value,
        // so the row reads as evenly spaced rather than each cell just centering itself in a slice.
        // Also used as the top/bottom inset so the gap to the top edge matches the gap to the left edge.
        val gap = ((maxWidth - cellSize * OVERVIEW_GRID_COLUMNS) / (OVERVIEW_GRID_COLUMNS + 1)).coerceAtLeast(4.dp)
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = gap, horizontal = gap)
        ) {
            if (entries.isEmpty() && emptyLabelRes != null) {
                item(key = "empty") {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(emptyLabelRes), color = LocalContentColor.current.copy(alpha = 0.5f))
                    }
                }
            }
            items(rows, key = { row -> row.first().key }) { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    repeat(OVERVIEW_GRID_COLUMNS) { slot ->
                        Box(modifier = Modifier.width(cellSize)) {
                            row.getOrNull(slot)?.let { entry -> CategoryGridCell(entry, cellSize, onRequestRemove = { onRequestRemove(entry) }) }
                        }
                    }
                }
            }
            if (addLabelRes != null && onAddClick != null) {
                item(key = "add") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .noRippleClickable(onClick = onAddClick)
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = LocalContentColor.current.copy(alpha = 0.7f))
                        Text(stringResource(addLabelRes), color = LocalContentColor.current.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryGridCell(entry: CategoryEntry, cellSize: Dp, onRequestRemove: () -> Unit) {
    Column(
        modifier = Modifier
            .width(cellSize)
            .combinedClickable(onClick = entry.onClick, onLongClick = entry.onRemove?.let { { onRequestRemove() } })
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        entry.icon()
        Text(
            entry.label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalContentColor.current.copy(alpha = 0.87f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun AddRow(labelRes: Int, onClick: () -> Unit, rowHeight: Dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = LocalContentColor.current.copy(alpha = 0.7f))
        Text(stringResource(labelRes), color = LocalContentColor.current.copy(alpha = 0.7f), modifier = Modifier.padding(start = 8.dp))
    }
}
