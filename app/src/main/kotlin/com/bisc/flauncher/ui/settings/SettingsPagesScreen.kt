package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.GestureAction
import com.bisc.flauncher.data.model.GestureActionType
import com.bisc.flauncher.data.model.HomeGesture
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.data.model.PageDirection
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.dock.PageIconGlyph
import com.bisc.flauncher.ui.home.dock.PageIconGlyphImage
import com.bisc.flauncher.ui.home.dock.defaultColor
import com.bisc.flauncher.ui.pageLabel
import kotlinx.coroutines.launch

/**
 * Two distinct groups, no longer one flat drag-reorderable list: a horizontal carousel preview of
 * the swipeable ROWS pages (Home always centered, a "+" ghost tile on each end to add a page in
 * that direction), and below it a plain list of the pages that don't swipe at all (App overview,
 * Text search, Quick overview — autonomous singletons, reachable by link/gesture rather than by
 * paging). The old flat list let you drag a singleton tile in among the swipeable ones with no
 * real effect on the pager (which only ever reads the ROWS subset) — this split makes that
 * distinction visible instead of silently cosmetic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPagesScreen(
    navController: NavController,
    onBack: () -> Unit,
    onOpenPage: (LauncherPage) -> Unit,
    onOpenIcon: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val allPages by viewModel.allPages.collectAsState()
    val gestureActions by viewModel.gestureActions.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val rowsPages = remember(allPages) { allPages.filter { it.kind == LauncherPageKind.ROWS }.sortedBy { it.orderIndex } }
    // Quick overview (SIDEBAR) is disabled for now — hidden here rather than deleted, pending a
    // bigger rework; see SettingsScreen's own root card and HomeViewModel.homePanelEnabled for the
    // other two places this is hidden.
    val otherPages = remember(allPages) { allPages.filter { it.kind != LauncherPageKind.ROWS && it.kind != LauncherPageKind.SIDEBAR } }

    fun gestureForPage(pageId: Long): HomeGesture? = HomeGesture.entries.firstOrNull { gesture ->
        gestureActions[gesture]?.type == GestureActionType.PAGE && gestureActions[gesture]?.payload?.toLongOrNull() == pageId
    }

    var renamingPage by remember { mutableStateOf<LauncherPage?>(null) }
    var pickingTemplateForDirection by remember { mutableStateOf<PageDirection?>(null) }
    var pickingInvocationForPage by remember { mutableStateOf<LauncherPage?>(null) }

    SettingsSubScreen(title = stringResource(R.string.settings_section_pages), navController = navController, onBack = onBack) {
        SettingsGroupLabel(stringResource(R.string.settings_pages_carousel_label))
        PagesCarousel(
            pages = rowsPages,
            onTileClick = onOpenPage,
            onEditIcon = { page -> onOpenIcon(page.id) },
            onRename = { page -> renamingPage = page },
            onRemove = { page -> viewModel.removePage(context, page) },
            onAddLeft = { pickingTemplateForDirection = PageDirection.LEFT },
            onAddRight = { pickingTemplateForDirection = PageDirection.RIGHT }
        )

        SettingsPreferenceGroup(title = stringResource(R.string.settings_pages_other_label)) {
            otherPages.forEach { page ->
                val label = pageLabel(page, allPages)
                val glyph = page.iconGlyph?.let { runCatching { PageIconGlyph.valueOf(it) }.getOrNull() } ?: PageIconGlyph.forPageKind(page.kind)
                val bg = page.iconBackgroundColor?.let { Color(it) } ?: glyph.defaultColor()
                val tint = page.iconColor?.let { Color(it) } ?: Color.White
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenPage(page) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(bg).clickable { onOpenIcon(page.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        PageIconGlyphImage(glyph = glyph, tint = tint, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.titleMedium)
                        val boundGesture = gestureForPage(page.id)
                        Text(
                            boundGesture?.let { gestureLabel(it) } ?: stringResource(R.string.settings_pages_invocation_none),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { pickingInvocationForPage = page }) {
                        Icon(Icons.Filled.TouchApp, contentDescription = stringResource(R.string.settings_pages_invocation_title))
                    }
                    IconButton(onClick = { renamingPage = page }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.settings_pages_rename))
                    }
                }
            }
        }

        // Folded in here per the masterplan's final nav (Phase 9) — Gestures is no longer its own
        // top-level Settings entry, since it's fundamentally page/navigation behavior rather than a
        // separate concern. App overview used to have its own shortcut row here too, duplicating the
        // "Utility pages" row above (same destination, same route) but with less functionality (that
        // row also offers rename/icon/gesture-invocation) — removed in favor of just that one row.
        SettingsPreferenceGroup(title = stringResource(R.string.settings_pages_behavior_label)) {
            SettingsPreference(
                icon = Icons.Filled.Gesture,
                title = stringResource(R.string.settings_section_gestures),
                summary = stringResource(R.string.settings_card_gestures_subtitle),
                onClick = { navController.navigate("settings/gestures") }
            )
        }
    }

    pickingInvocationForPage?.let { page ->
        val current = gestureForPage(page.id)
        val taken = gestureActions.filterValues { it.type != GestureActionType.NONE }.keys - setOfNotNull(current)
        PageInvocationPickerSheet(
            current = current,
            takenGestures = taken,
            onSelect = { gesture ->
                if (current != null && current != gesture) viewModel.setGestureAction(current, GestureAction.NONE)
                if (gesture != null) viewModel.setGestureAction(gesture, GestureAction(GestureActionType.PAGE, page.id.toString()))
                pickingInvocationForPage = null
            },
            onDismiss = { pickingInvocationForPage = null }
        )
    }

    pickingTemplateForDirection?.let { direction ->
        PageTemplatePickerSheet(
            onBlankPageSelected = {
                coroutineScope.launch {
                    val newId = viewModel.addPageInDirection(direction)
                    pickingTemplateForDirection = null
                    // Built directly from the returned id rather than looked up in `allPages` — that
                    // state won't have recomposed with the new row yet at this exact point, and
                    // onOpenPage only ever reads .id/.kind for routing (see MainActivity's dispatch),
                    // so a minimal synthetic object is both correct and simpler than waiting on it.
                    onOpenPage(LauncherPage(id = newId, orderIndex = 0, kind = LauncherPageKind.ROWS))
                }
            },
            onDismiss = { pickingTemplateForDirection = null }
        )
    }

    renamingPage?.let { page ->
        var name by remember(page.id) { mutableStateOf(page.name.orEmpty()) }
        AlertDialog(
            onDismissRequest = { renamingPage = null },
            title = { Text(stringResource(R.string.settings_pages_rename)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_pages_rename_hint)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renamePage(page.id, name)
                    renamingPage = null
                }) { Text(stringResource(R.string.settings_rows_save)) }
            },
            dismissButton = {
                TextButton(onClick = { renamingPage = null }) { Text(stringResource(R.string.widget_notes_cancel)) }
            }
        )
    }
}

/** Width:height of a real phone screen, not an icon swatch — this is what makes a carousel tile
 *  read as "a screen" instead of just another app-icon-sized button. */
private const val SCREEN_ASPECT_RATIO = 0.47f
private val CAROUSEL_OUTER_PADDING = 16.dp
private val CAROUSEL_SPACING = 12.dp

@Composable
private fun PagesCarousel(
    pages: List<LauncherPage>,
    onTileClick: (LauncherPage) -> Unit,
    onEditIcon: (LauncherPage) -> Unit,
    onRename: (LauncherPage) -> Unit,
    onRemove: (LauncherPage) -> Unit,
    onAddLeft: () -> Unit,
    onAddRight: () -> Unit
) {
    val homeIndex = pages.indexOfFirst { it.isHome }.coerceAtLeast(0)
    val listState = rememberLazyListState()
    var didInitialScroll by remember { mutableStateOf(false) }
    LaunchedEffect(pages.size) {
        if (!didInitialScroll && pages.isNotEmpty()) {
            // +1 for the leading "+" ghost tile ahead of the page tiles.
            listState.scrollToItem((homeIndex + 1).coerceAtMost((pages.size + 1).coerceAtLeast(0)))
            didInitialScroll = true
        }
    }
    // Sized so exactly 3 tiles (Home plus one ghost "+" on each side, the common case with no
    // extra pages yet) fill the available width with no sliver of a 4th peeking in — a real
    // aspect-ratio page preview instead of a small icon-sized square.
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val tileWidth = (maxWidth - CAROUSEL_OUTER_PADDING * 2 - CAROUSEL_SPACING * 2) / 3
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(CAROUSEL_SPACING),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = CAROUSEL_OUTER_PADDING)
        ) {
            item { GhostTile(width = tileWidth, onClick = onAddLeft) }
            items(pages, key = { it.id }) { page ->
                PageTile(
                    page = page,
                    pages = pages,
                    width = tileWidth,
                    onClick = { onTileClick(page) },
                    onEditIcon = { onEditIcon(page) },
                    onRename = { onRename(page) },
                    onRemove = { onRemove(page) }
                )
            }
            item { GhostTile(width = tileWidth, onClick = onAddRight) }
        }
    }
}

@Composable
private fun PageTile(
    page: LauncherPage,
    pages: List<LauncherPage>,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    onEditIcon: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit
) {
    val label = pageLabel(page, pages)
    val glyph = page.iconGlyph?.let { runCatching { PageIconGlyph.valueOf(it) }.getOrNull() } ?: PageIconGlyph.forPageKind(page.kind)
    val bg = page.iconBackgroundColor?.let { Color(it) } ?: glyph.defaultColor()
    val tint = page.iconColor?.let { Color(it) } ?: Color.White
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        Column(
            modifier = Modifier
                .width(width)
                .combinedClickable(onClick = onClick, onLongClick = { menuExpanded = true })
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(SCREEN_ASPECT_RATIO)
                    .clip(RoundedCornerShape(20.dp))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                PageIconGlyphImage(glyph = glyph, tint = tint, modifier = Modifier.size(width * 0.4f))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings_page_icon_title)) },
                leadingIcon = { Icon(Icons.Filled.Palette, contentDescription = null) },
                onClick = { onEditIcon(); menuExpanded = false }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings_pages_rename)) },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                onClick = { onRename(); menuExpanded = false }
            )
            if (!page.isHome) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings_pages_remove)) },
                    leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
                    onClick = { onRemove(); menuExpanded = false }
                )
            }
        }
    }
}

@Composable
private fun GhostTile(width: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(width).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(SCREEN_ASPECT_RATIO)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = stringResource(R.string.settings_pages_add),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(width * 0.3f)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text("", style = MaterialTheme.typography.labelMedium)
    }
}

/** Which, if any, of the 4 rebindable [HomeGesture]s jumps straight to this page — the same global
 *  binding Settings -> Gestures manages, just surfaced from the page's own side. Picking a gesture
 *  already bound elsewhere isn't offered (see [takenGestures]); picking "None" clears this page's
 *  current binding, if it has one, without touching anything else. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageInvocationPickerSheet(
    current: HomeGesture?,
    takenGestures: Set<HomeGesture>,
    onSelect: (HomeGesture?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            RadioPreference(stringResource(R.string.settings_pages_invocation_none), null, current, onSelect)
            HomeGesture.entries.forEach { gesture ->
                val taken = gesture in takenGestures
                RadioPreference(
                    label = gestureLabel(gesture),
                    value = gesture,
                    selected = current,
                    onSelect = onSelect,
                    enabled = !taken,
                    summary = if (taken) stringResource(R.string.settings_gesture_already_used) else null
                )
            }
        }
    }
}

/** Step 1 of adding a carousel page: which kind to start from. Only one real option today ("blank
 *  page", matching the old [HomeViewModel.addPage] behavior), but kept as its own step/sheet so
 *  more starting templates can be added later without reshuffling the call site. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageTemplatePickerSheet(onBlankPageSelected: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            SettingsPreferenceGroup(title = stringResource(R.string.settings_pages_add_template_label)) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_pages_add_template_blank),
                    onClick = onBlankPageSelected,
                    trailing = null
                )
            }
        }
    }
}
