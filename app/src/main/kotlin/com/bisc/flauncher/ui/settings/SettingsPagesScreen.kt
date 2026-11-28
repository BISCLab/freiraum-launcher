package com.bisc.flauncher.ui.settings

import com.bisc.flauncher.ui.marqueeWhenCut
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material3.Switch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.foundation.gestures.scrollBy
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
 * Two distinct groups: a horizontal carousel preview of the swipeable ROWS pages (Home always
 * centered, a "+" ghost tile on each end to add a page in that direction), and below it the pages
 * that don't swipe at all (App overview, Text search - singletons reached by link or gesture
 * rather than by paging).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPagesScreen(
    navController: NavController,
    onBack: () -> Unit,
    onOpenPage: (LauncherPage) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val allPages by viewModel.allPages.collectAsState()
    val carouselEndless by viewModel.carouselEndless.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val rowsPages = remember(allPages) { allPages.filter { it.kind == LauncherPageKind.ROWS }.sortedBy { it.orderIndex } }
    val otherPages = remember(allPages) { allPages.filter { it.kind != LauncherPageKind.ROWS }.sortedBy { it.orderIndex } }


    var pickingTemplateForDirection by remember { mutableStateOf<PageDirection?>(null) }

    // A new page is made right away and opened, empty, to fill.
    fun addPage(direction: PageDirection) {
        coroutineScope.launch {
            val newId = viewModel.addPageInDirection(direction)
            onOpenPage(LauncherPage(id = newId, orderIndex = 0, kind = LauncherPageKind.ROWS))
        }
    }

    SettingsSubScreen(title = stringResource(R.string.settings_section_pages), navController = navController, onBack = onBack) {
        // The pages to swipe, the start page marked; the endless switch belongs to them.
        SettingsPreferenceGroup(title = stringResource(R.string.settings_pages_carousel_label), subtitle = stringResource(R.string.pages_swipe_info)) {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)) {
                PagesCarousel(
                    pages = rowsPages,
                    onTileClick = onOpenPage,
                    onReorder = viewModel::reorderRowsPages,
                    onAddLeft = { addPage(PageDirection.LEFT) },
                    onAddRight = { addPage(PageDirection.RIGHT) }
                )
            }
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_pages_carousel_endless),
                summary = stringResource(R.string.settings_pages_carousel_endless_summary),
                info = stringResource(R.string.settings_pages_carousel_endless_info),
                onClick = { viewModel.setCarouselEndless(!carouselEndless) },
                trailing = { Switch(checked = carouselEndless, onCheckedChange = viewModel::setCarouselEndless) }
            )
        }
        // The function pages, each with how it opens.
        SettingsPreferenceGroup(title = stringResource(R.string.settings_pages_other_label), subtitle = stringResource(R.string.pages_open_info)) {
            FunctionPagesGrid(otherPages, allPages, viewModel, navController, onOpenPage)
        }
        val inUse = rememberGesturesInUse(viewModel)
        SettingsPreferenceGroup(title = stringResource(R.string.settings_section_gestures)) {
            SettingsPreference(
                icon = Icons.Filled.Gesture,
                title = stringResource(R.string.pages_all_gestures),
                summary = stringResource(R.string.pages_gestures_in_use, inUse, HomeGesture.entries.size),
                onClick = { navController.navigate("settings/gestures") },
                trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
            )
        }
    }

    pickingTemplateForDirection?.let { direction ->
        PageTemplatePickerSheet(
            onBlankPageSelected = {
                coroutineScope.launch {
                    val newId = viewModel.addPageInDirection(direction)
                    pickingTemplateForDirection = null
                    // Built directly from the returned id rather than looked up in `allPages` - that
                    // state won't have recomposed with the new row yet at this exact point, and
                    // onOpenPage only ever reads .id/.kind for routing (see MainActivity's dispatch),
                    // so a minimal synthetic object is both correct and simpler than waiting on it.
                    onOpenPage(LauncherPage(id = newId, orderIndex = 0, kind = LauncherPageKind.ROWS))
                }
            },
            onDismiss = { pickingTemplateForDirection = null }
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
    val bg = page.iconBackgroundColor?.let { Color(it) } ?: com.bisc.flauncher.ui.home.dock.pageDefaultColor(page, glyph)
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

/** Icon and name of a utility page (App list, Text search), as its own card at the top of its
 *  Appearance → Page screen - the same rows a carousel page has in its Appearance screen. */
@Composable
internal fun UtilityPageIdentityGroup(kind: LauncherPageKind, navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    val allPages by viewModel.allPages.collectAsState()
    val page = allPages.find { it.kind == kind } ?: return
    SettingsPreferenceGroup {
        PageIdentityRows(
            page = page,
            pages = allPages,
            onOpenIcon = { navController.navigate("settings/pages/${page.id}/icon") },
            onRename = { name -> viewModel.renamePage(page.id, name) }
        )
    }
}

/** "Remove page" at the bottom of a removable page's settings - asks first, since everything on
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

/** Width:height of a real phone screen, not an icon swatch - this is what makes a carousel tile
 *  read as "a screen" instead of just another app-icon-sized button. */
private const val SCREEN_ASPECT_RATIO = 0.47f

/** Low screens (tablets held sideways): a page tile stays at most 40 % of the screen height tall. */
@Composable
private fun carouselTileMaxWidth(): androidx.compose.ui.unit.Dp =
    com.bisc.flauncher.ui.windowHeight() * 0.4f * SCREEN_ASPECT_RATIO
private val CAROUSEL_OUTER_PADDING = 16.dp
private val CAROUSEL_SPACING = 12.dp
/** How far the carousel scrolls per frame while a dragged page is held at its edge. */
private const val AUTO_SCROLL_STEP_PX = 14f

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
    var dragOffset by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val homeIndex = pages.indexOfFirst { it.isHome }.coerceAtLeast(0)
    val listState = rememberLazyListState()
    var didInitialScroll by remember { mutableStateOf(false) }
    LaunchedEffect(pages.size) {
        if (!didInitialScroll && pages.isNotEmpty()) {
            // Home in the middle: the tile left of it first. With the leading "+" tile at index 0,
            // Home sits at homeIndex + 1, so its left neighbor is at homeIndex.
            listState.scrollToItem(homeIndex.coerceIn(0, pages.size))
            didInitialScroll = true
        }
    }
    // Sized so exactly 3 tiles (Home plus one ghost "+" on each side, the common case with no
    // extra pages yet) fill the available width with no sliver of a 4th peeking in - a real
    // aspect-ratio page preview instead of a small icon-sized square.
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val tileWidth = minOf((maxWidth - CAROUSEL_OUTER_PADDING * 2 - CAROUSEL_SPACING * 2) / 3, carouselTileMaxWidth())
        // Where the tiles are held smaller (low screens), the gaps grow instead, so it stays exactly
        // three tiles across and no fourth peeks in.
        val spacing = ((maxWidth - CAROUSEL_OUTER_PADDING * 2 - tileWidth * 3) / 2).coerceAtLeast(CAROUSEL_SPACING)
        val stepPx = with(androidx.compose.ui.platform.LocalDensity.current) { (tileWidth + spacing).toPx() }

        // Swaps the dragged page with a neighbor once it's been dragged past half a tile.
        fun swapIfPastHalf(pageId: Long) {
            val index = order.indexOfFirst { it.id == pageId }
            if (dragOffset > stepPx / 2 && index < order.lastIndex) {
                order = order.toMutableList().apply { add(index + 1, removeAt(index)) }
                dragOffset -= stepPx
            } else if (dragOffset < -stepPx / 2 && index > 0) {
                order = order.toMutableList().apply { add(index - 1, removeAt(index)) }
                dragOffset += stepPx
            }
        }

        // While the dragged page is held near either edge, the carousel scrolls along so it can be
        // carried past every other page in one drag. Scrolling moves the page's own slot, so the
        // drag offset takes the scrolled distance to keep the tile under the finger.
        val scope = rememberCoroutineScope()
        var autoScroll by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
        fun updateAutoScroll(pageId: Long) {
            val layout = listState.layoutInfo
            val info = layout.visibleItemsInfo.find { it.key == pageId } ?: return
            val start = info.offset + dragOffset
            val end = start + info.size
            val edgeZone = stepPx / 4
            val direction = when {
                start < layout.viewportStartOffset + edgeZone -> -1f
                end > layout.viewportEndOffset - edgeZone -> 1f
                else -> 0f
            }
            if (direction == 0f) {
                autoScroll?.cancel()
                autoScroll = null
            } else if (autoScroll?.isActive != true) {
                autoScroll = scope.launch {
                    while (draggingId == pageId) {
                        val scrolled = listState.scrollBy(direction * AUTO_SCROLL_STEP_PX)
                        if (scrolled == 0f) break
                        dragOffset += scrolled
                        swapIfPastHalf(pageId)
                        kotlinx.coroutines.delay(16)
                    }
                }
            }
        }
        fun endDrag() {
            autoScroll?.cancel()
            autoScroll = null
            draggingId = null
            dragOffset = 0f
        }

        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            // A fling always stops with one tile in the middle, never with half tiles at the edges.
            flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(
                listState,
                androidx.compose.foundation.gestures.snapping.SnapPosition.Center
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
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
                                    swapIfPastHalf(page.id)
                                    updateAutoScroll(page.id)
                                },
                                onDragEnd = {
                                    endDrag()
                                    if (order.map { it.id } != pages.map { it.id }) onReorder(order.map { it.id })
                                },
                                onDragCancel = {
                                    endDrag()
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
    val bg = page.iconBackgroundColor?.let { Color(it) } ?: com.bisc.flauncher.ui.home.dock.pageDefaultColor(page, glyph)
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
                // The start page: a house and "Start", so the middle shows without words.
                if (page.isHome) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Home, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(14.dp))
                        Text(stringResource(R.string.pages_start_badge), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().marqueeWhenCut()
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

/** Step 1 of adding a carousel page: which kind to start from (currently a blank page, see
 *  [HomeViewModel.addPage]); its own step so more starting templates fit in without changes here. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageTemplatePickerSheet(onBlankPageSelected: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = onDismiss) {
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
