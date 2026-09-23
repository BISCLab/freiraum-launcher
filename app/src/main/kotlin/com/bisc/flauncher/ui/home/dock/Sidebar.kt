package com.bisc.flauncher.ui.home.dock

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import com.bisc.flauncher.ui.home.rememberAppIcon
import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.AppInfo
import com.bisc.flauncher.data.model.Folder
import com.bisc.flauncher.data.model.FolderItem
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.data.model.Note
import com.bisc.flauncher.data.model.PinnedContact
import com.bisc.flauncher.data.model.PinnedShortcut
import com.bisc.flauncher.data.model.PinnedWebsite
import com.bisc.flauncher.data.model.SavedMapTarget
import com.bisc.flauncher.data.model.SidebarCategoryViewMode
import com.bisc.flauncher.data.model.SidebarIconOption
import com.bisc.flauncher.data.model.SidebarPinnedApp
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.settings.Handedness

private val RAIL_ICON_BUTTON_SIZE = 48.dp
private val RAIL_ICON_GLYPH_SIZE = 24.dp
private val RAIL_APP_IMAGE_SIZE = 28.dp
private val RAIL_ITEM_GAP = 4.dp

private const val RAIL_SELECTED_BACKGROUND_ALPHA = 0.16f
private val RAIL_VERTICAL_PADDING = 12.dp
private val RAIL_DIVIDER_HORIZONTAL_INSET = 12.dp

private val RAIL_DIVIDER_WIDTH = 1.dp

/** Quick overview panel: category rail and the content of the selected category. */
@Composable
fun SidebarOverviewPanel(
    entry: SidebarEntryType,
    items: List<ResolvedSidebarItem>,
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
    onSidebarItemClick: (ResolvedSidebarItem) -> Unit,
    onSettingsClick: () -> Unit,
    panelAppearance: ElementAppearance,
    shape: Shape,
    handedness: Handedness,
    dock: ScaledDock,
    modifier: Modifier = Modifier
) {
    val railWidth = dock.innerHorizontalPadding + dock.buttonSize + dock.itemSpacing
    val railTotalWidth = railWidth + RAIL_DIVIDER_WIDTH

    DockPanelSurface(appearance = panelAppearance, shape = shape, modifier = modifier) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val contentWidth = (maxWidth - railTotalWidth).coerceAtLeast(0.dp)
            val metrics = remember {
                SidebarLayout.Metrics(iconSize = RAIL_ICON_BUTTON_SIZE, itemGap = RAIL_ITEM_GAP, containerVerticalPadding = RAIL_VERTICAL_PADDING)
            }
            val fit = remember(items.size, maxHeight) { SidebarLayout.fit(items.size, maxHeight, metrics) }
            val contentRowHeight = remember(installedApps.size, maxHeight) { overviewRowHeight(maxHeight, installedApps.size) }
            val listState = rememberLazyListState()
            if (fit.needsScroll) {
                LaunchedEffect(fit.needsScroll) { listState.scrollToItem(items.size + 1) }
            }

            val content = @Composable {
                Box(modifier = Modifier.width(contentWidth).fillMaxHeight()) {
                    OverviewContent(
                        entry = entry,
                        pages = pages,
                        installedApps = installedApps,
                        sidebarPinnedApps = sidebarPinnedApps,
                        folders = folders,
                        folderItemsByFolder = folderItemsByFolder,
                        notes = notes,
                        savedMapTargets = savedMapTargets,
                        pinnedWebsites = pinnedWebsites,
                        pinnedShortcuts = pinnedShortcuts,
                        hasShortcutPermission = hasShortcutPermission,
                        rowHeight = contentRowHeight,
                        viewMode = viewMode,
                        showAddRow = showAddRow,
                        loadIcon = loadIcon,
                        loadPackIcon = loadPackIcon,
                        onAppClick = onAppClick,
                        onPageClick = onPageClick,
                        onFolderClick = onFolderClick,
                        onAddFolderClick = onAddFolderClick,
                        onSaveNote = onSaveNote,
                        onDeleteNote = onDeleteNote,
                        onCreateNote = onCreateNote,
                        onReorderNotes = onReorderNotes,
                        onMapTargetClick = onMapTargetClick,
                        onRemoveMapTarget = onRemoveMapTarget,
                        onAddMapTargetClick = onAddMapTargetClick,
                        onWebsiteClick = onWebsiteClick,
                        onRemoveWebsite = onRemoveWebsite,
                        onAddWebsiteClick = onAddWebsiteClick,
                        onShortcutClick = onShortcutClick,
                        onRemoveShortcut = onRemoveShortcut,
                        onAddShortcutClick = onAddShortcutClick,
                        onAddSidebarAppsClick = onAddSidebarAppsClick,
                        onRemoveSidebarApp = onRemoveSidebarApp,
                        pinnedContacts = pinnedContacts,
                        hasContactsPermission = hasContactsPermission,
                        onContactClick = onContactClick,
                        onRemoveContact = onRemoveContact,
                        onAddContactClick = onAddContactClick,
                        onContactsPermissionResult = onContactsPermissionResult,
                        loadContactPhoto = loadContactPhoto,
                        dock = dock
                    )
                }
            }
            val dividerColor = resolveElementColors(panelAppearance).dividerColor
            val divider = @Composable {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = RAIL_DIVIDER_HORIZONTAL_INSET)
                        .width(RAIL_DIVIDER_WIDTH)
                        .background(dividerColor)
                )
            }
            val itemsLatest = rememberUpdatedState(items)
            val onSidebarItemClickLatest = rememberUpdatedState(onSidebarItemClick)
            val touchSlop = androidx.compose.ui.platform.LocalViewConfiguration.current.touchSlop
            val iconRail = @Composable {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .width(railWidth)
                        .fillMaxHeight()
                        // Dragging down/up scrubs through the categories live, without tapping each icon;
                        // items that launch something (apps, shortcuts, places, Settings) stay tap-only,
                        // since triggering those mid-drag would be surprising.
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                var dragging = false
                                val startY = down.position.y
                                var lastKey: Any? = null
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    if (!dragging && kotlin.math.abs(change.position.y - startY) > touchSlop) dragging = true
                                    if (dragging) {
                                        change.consume()
                                        val y = change.position.y
                                        val info = listState.layoutInfo.visibleItemsInfo
                                            .firstOrNull { y >= it.offset && y < it.offset + it.size }
                                        val key = info?.key
                                        if (key != null && key != lastKey) {
                                            lastKey = key
                                            itemsLatest.value.find { it.raw.id == key }
                                                ?.takeIf { it is ResolvedSidebarItem.Category }
                                                ?.let { onSidebarItemClickLatest.value(it) }
                                        }
                                    }
                                }
                            }
                        },
                    contentPadding = PaddingValues(vertical = RAIL_VERTICAL_PADDING),
                    verticalArrangement = Arrangement.spacedBy(RAIL_ITEM_GAP),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(items, key = { it.raw.id }) { item ->
                        val isSelected = item is ResolvedSidebarItem.Category && item.entryType == entry
                        IconButton(
                            onClick = { onSidebarItemClick(item) },
                            modifier = Modifier
                                .size(RAIL_ICON_BUTTON_SIZE)
                                .background(
                                    color = if (isSelected) LocalContentColor.current.copy(alpha = RAIL_SELECTED_BACKGROUND_ALPHA) else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            SidebarItemIcon(item = item, loadIcon = loadIcon, glyphSize = RAIL_ICON_GLYPH_SIZE, appImageSize = RAIL_APP_IMAGE_SIZE)
                        }
                    }
                    item(key = "rail_divider") { SidebarDivider(horizontalPadding = 8.dp, color = dividerColor) }
                    item(key = "rail_settings") {
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.size(RAIL_ICON_BUTTON_SIZE)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.home_menu_settings),
                                tint = LocalContentColor.current.copy(alpha = 0.8f),
                                modifier = Modifier.size(RAIL_ICON_GLYPH_SIZE)
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxSize()) {
                if (handedness == Handedness.LEFT) {
                    iconRail(); divider(); content()
                } else {
                    content(); divider(); iconRail()
                }
            }
        }
    }
}

@Composable
private fun SidebarItemIcon(
    item: ResolvedSidebarItem,
    loadIcon: (ComponentName) -> ImageBitmap?,
    glyphSize: androidx.compose.ui.unit.Dp,
    appImageSize: androidx.compose.ui.unit.Dp
) {
    val customIcon = remember(item.raw.customIconKey) {
        item.raw.customIconKey?.let { key -> runCatching { SidebarIconOption.valueOf(key) }.getOrNull() }
    }
    if (customIcon != null) {
        Icon(
            imageVector = customIcon.icon,
            contentDescription = null,
            tint = LocalContentColor.current.copy(alpha = 0.8f),
            modifier = Modifier.size(glyphSize)
        )
        return
    }
    when (item) {
        is ResolvedSidebarItem.Category -> Icon(
            imageVector = item.entryType.icon,
            contentDescription = stringResource(item.entryType.labelRes),
            tint = LocalContentColor.current.copy(alpha = 0.8f),
            modifier = Modifier.size(glyphSize)
        )
        is ResolvedSidebarItem.App -> {
            val bitmap = rememberAppIcon(item.app.componentName, loadIcon)
            if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = item.app.label, modifier = Modifier.size(appImageSize))
            } else {
                Icon(
                    imageVector = SidebarIconOption.APPS.icon,
                    contentDescription = item.app.label,
                    tint = LocalContentColor.current.copy(alpha = 0.8f),
                    modifier = Modifier.size(glyphSize)
                )
            }
        }
        is ResolvedSidebarItem.Shortcut -> Icon(
            imageVector = SidebarIconOption.SHORTCUT.icon,
            contentDescription = item.shortcut.label,
            tint = LocalContentColor.current.copy(alpha = 0.8f),
            modifier = Modifier.size(glyphSize)
        )
        is ResolvedSidebarItem.MapTarget -> Icon(
            imageVector = SidebarIconOption.PLACE.icon,
            contentDescription = item.target.label,
            tint = LocalContentColor.current.copy(alpha = 0.8f),
            modifier = Modifier.size(glyphSize)
        )
    }
}

@Composable
private fun SidebarDivider(horizontalPadding: androidx.compose.ui.unit.Dp, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 2.dp)
            .height(1.dp)
            .background(color)
    )
}
