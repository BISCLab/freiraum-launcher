package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
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
    val otherPages = remember(allPages) { allPages.filter { it.kind != LauncherPageKind.ROWS } }

    fun gestureForPage(pageId: Long): HomeGesture? = HomeGesture.entries.firstOrNull { gesture ->
        gestureActions[gesture]?.type == GestureActionType.PAGE && gestureActions[gesture]?.payload?.toLongOrNull() == pageId
    }

    var renamingPage by remember { mutableStateOf<LauncherPage?>(null) }
    var pickingTemplateForDirection by remember { mutableStateOf<PageDirection?>(null) }

    SettingsSubScreen(title = stringResource(R.string.settings_section_pages), navController = navController, onBack = onBack) {
        SettingsGroupLabel(stringResource(R.string.settings_pages_carousel_label))
        // Icon and name live in each page's own Appearance screen (see PageIdentityRows), removal at
        // the bottom of its settings screen.
        PagesCarousel(
            pages = rowsPages,
            onTileClick = onOpenPage,
            onReorder = viewModel::reorderRowsPages,
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
                    // Which gesture opens it is shown above but set in one place only: Gestures.
                    IconButton(onClick = { renamingPage = page }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.settings_pages_rename))
                    }
                }
            }
        }

        // Gestures live here, not as their own top-level entry — they're page/navigation behavior.
        // They're the only place a gesture is assigned; the utility-page rows above just show it.
        SettingsPreferenceGroup(title = stringResource(R.string.settings_pages_behavior_label)) {
            SettingsPreference(
                icon = Icons.Filled.Gesture,
                title = stringResource(R.string.settings_section_gestures),
                summary = stringResource(R.string.settings_card_gestures_subtitle),
                onClick = { navController.navigate("settings/gestures") }
            )
        }
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
        RenamePageDialog(
            page = page,
            onSave = { name -> viewModel.renamePage(page.id, name); renamingPage = null },
            onDismiss = { renamingPage = null }
        )
    }
}

@Composable
private fun RenamePageDialog(page: LauncherPage, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember(page.id) { mutableStateOf(page.name.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
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
            TextButton(onClick = { onSave(name) }) { Text(stringResource(R.string.settings_rows_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.widget_notes_cancel)) }
        }
    )
}

/** A carousel page's own Icon and Name rows, for the top of its Appearance screen's card. */
@Composable
internal fun PageIdentityRows(page: LauncherPage, pages: List<LauncherPage>, onOpenIcon: () -> Unit, onRename: (String) -> Unit) {
    var renaming by remember { mutableStateOf(false) }
    val glyph = page.iconGlyph?.let { runCatching { PageIconGlyph.valueOf(it) }.getOrNull() } ?: PageIconGlyph.forPageKind(page.kind)
    val bg = page.iconBackgroundColor?.let { Color(it) } ?: glyph.defaultColor()
    val tint = page.iconColor?.let { Color(it) } ?: Color.White
    run {
        SettingsPreference(
            icon = null,
            title = stringResource(R.string.settings_page_icon_title),
            onClick = onOpenIcon,
            trailing = {
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    PageIconGlyphImage(glyph = glyph, tint = tint, modifier = Modifier.size(18.dp))
                }
            }
        )
        SettingsPreference(
            icon = null,
            title = stringResource(R.string.settings_pages_rename),
            summary = pageLabel(page, pages),
            onClick = { renaming = true },
            trailing = null
        )
    }
    if (renaming) {
        RenamePageDialog(page = page, onSave = { onRename(it); renaming = false }, onDismiss = { renaming = false })
    }
}

/** "Remove page" at the bottom of a removable page's settings — asks first, since everything on
 *  the page goes with it. */
@Composable
internal fun RemovePageGroup(label: String, onRemove: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }
    SettingsPreferenceGroup {
        SettingsPreference(
            icon = Icons.Filled.DeleteOutline,
            title = stringResource(R.string.settings_pages_remove),
            onClick = { confirming = true },
            trailing = null
        )
    }
    if (confirming) {
        com.bisc.flauncher.ui.home.ConfirmRemoveDialog(
            label = label,
            onConfirm = { confirming = false; onRemove() },
            onDismiss = { confirming = false }
        )
    }
}

/** Width:height of a real phone screen, not an icon swatch — this is what makes a carousel tile
 *  read as "a screen" instead of just another app-icon-sized button. */
private const val SCREEN_ASPECT_RATIO = 0.47f
private val CAROUSEL_OUTER_PADDING = 16.dp
private val CAROUSEL_SPACING = 12.dp

/** The swipeable pages side by side, a "+" ghost tile on each end. Long-press a page and drag it
 *  sideways to move it: it swaps with a neighbor once dragged past half a tile, and the new order
 *  is saved on release. */
@Composable
private fun PagesCarousel(
    pages: List<LauncherPage>,
    onTileClick: (LauncherPage) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onAddLeft: () -> Unit,
    onAddRight: () -> Unit
) {
    // Local working order while dragging; reset whenever the saved order arrives.
    var order by remember(pages) { mutableStateOf(pages) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
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
        val stepPx = with(androidx.compose.ui.platform.LocalDensity.current) { (tileWidth + CAROUSEL_SPACING).toPx() }
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(CAROUSEL_SPACING),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = CAROUSEL_OUTER_PADDING)
        ) {
            item { GhostTile(width = tileWidth, onClick = onAddLeft) }
            items(order, key = { it.id }) { page ->
                val dragging = page.id == draggingId
                PageTile(
                    page = page,
                    pages = order,
                    width = tileWidth,
                    onClick = { onTileClick(page) },
                    modifier = Modifier
                        .then(if (dragging) Modifier else Modifier.animateItem())
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer {
                            translationX = if (dragging) dragOffset else 0f
                            val scale = if (dragging) 1.06f else 1f
                            scaleX = scale
                            scaleY = scale
                        }
                        .pointerInput(page.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    draggingId = page.id
                                    dragOffset = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.x
                                    val index = order.indexOfFirst { it.id == page.id }
                                    if (dragOffset > stepPx / 2 && index < order.lastIndex) {
                                        order = order.toMutableList().apply { add(index + 1, removeAt(index)) }
                                        dragOffset -= stepPx
                                    } else if (dragOffset < -stepPx / 2 && index > 0) {
                                        order = order.toMutableList().apply { add(index - 1, removeAt(index)) }
                                        dragOffset += stepPx
                                    }
                                },
                                onDragEnd = {
                                    draggingId = null
                                    dragOffset = 0f
                                    if (order.map { it.id } != pages.map { it.id }) onReorder(order.map { it.id })
                                },
                                onDragCancel = {
                                    draggingId = null
                                    dragOffset = 0f
                                    order = pages
                                }
                            )
                        }
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
    modifier: Modifier = Modifier
) {
    val label = pageLabel(page, pages)
    val glyph = page.iconGlyph?.let { runCatching { PageIconGlyph.valueOf(it) }.getOrNull() } ?: PageIconGlyph.forPageKind(page.kind)
    val bg = page.iconBackgroundColor?.let { Color(it) } ?: glyph.defaultColor()
    val tint = page.iconColor?.let { Color(it) } ?: Color.White
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .width(width)
                .clickable(onClick = onClick)
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
