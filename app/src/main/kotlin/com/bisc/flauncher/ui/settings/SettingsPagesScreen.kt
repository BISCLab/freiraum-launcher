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
    val gestureActions by viewModel.gestureActions.collectAsState()
    val carouselEndless by viewModel.carouselEndless.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val rowsPages = remember(allPages) { allPages.filter { it.kind == LauncherPageKind.ROWS }.sortedBy { it.orderIndex } }
    val otherPages = remember(allPages) { allPages.filter { it.kind != LauncherPageKind.ROWS } }

    fun gestureForPage(pageId: Long): HomeGesture? = HomeGesture.entries.firstOrNull { gesture ->
        gestureActions[gesture]?.type == GestureActionType.PAGE && gestureActions[gesture]?.payload?.toLongOrNull() == pageId
    }

    var pickingTemplateForDirection by remember { mutableStateOf<PageDirection?>(null) }

    SettingsSubScreen(title = stringResource(R.string.settings_section_pages), navController = navController, onBack = onBack) {
        SettingsGroupLabel(stringResource(R.string.settings_pages_carousel_label))
        // Icon and name live in each page's own Appearance screen (see PageIdentityRows), removal at
        // the bottom of its settings screen.
        PagesCarousel(
            pages = rowsPages,
            gestureFor = ::gestureForPage,
            onTileClick = onOpenPage,
            onReorder = viewModel::reorderRowsPages,
            onAddLeft = { pickingTemplateForDirection = PageDirection.LEFT },
            onAddRight = { pickingTemplateForDirection = PageDirection.RIGHT }
        )
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_pages_carousel_endless),
                info = stringResource(R.string.settings_pages_carousel_endless_info),
                onClick = { viewModel.setCarouselEndless(!carouselEndless) },
                trailing = { Switch(checked = carouselEndless, onCheckedChange = viewModel::setCarouselEndless) }
            )
        }

        // App list and Text search don't swipe - set apart in their own row of the same tiles.
        // Their icon and name live in their own Appearance → Page screen.
        SettingsGroupLabel(stringResource(R.string.settings_pages_other_label))
        UtilityPagesRow(
            pages = otherPages,
            allPages = allPages,
            gestureFor = ::gestureForPage,
            onTileClick = onOpenPage
        )

        // Gestures live here, not as their own top-level entry - they're page/navigation behavior.
        // They're the only place a gesture is assigned; the page tiles above just show it.
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
    gestureFor: (Long) -> HomeGesture?,
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
    // extra pages yet) fill the available width with no sliver of a 4th peeking in - a real
    // aspect-ratio page preview instead of a small icon-sized square.
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val tileWidth = (maxWidth - CAROUSEL_OUTER_PADDING * 2 - CAROUSEL_SPACING * 2) / 3
        val stepPx = with(androidx.compose.ui.platform.LocalDensity.current) { (tileWidth + CAROUSEL_SPACING).toPx() }

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
                    gesture = gestureFor(page.id),
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

/** App list and Text search as tiles, sized exactly like the carousel's so the two rows line up. */
@Composable
private fun UtilityPagesRow(
    pages: List<LauncherPage>,
    allPages: List<LauncherPage>,
    gestureFor: (Long) -> HomeGesture?,
    onTileClick: (LauncherPage) -> Unit
) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val tileWidth = (maxWidth - CAROUSEL_OUTER_PADDING * 2 - CAROUSEL_SPACING * 2) / 3
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = CAROUSEL_OUTER_PADDING, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(CAROUSEL_SPACING)
        ) {
            pages.forEach { page ->
                PageTile(
                    page = page,
                    pages = allPages,
                    width = tileWidth,
                    onClick = { onTileClick(page) },
                    gesture = gestureFor(page.id)
                )
            }
        }
    }
}

@Composable
private fun PageTile(
    page: LauncherPage,
    pages: List<LauncherPage>,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** The gesture that opens this page, shown as a small badge in the tile's corner. */
    gesture: HomeGesture? = null
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
                if (gesture != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            gestureIcon(gesture),
                            contentDescription = gestureLabel(gesture),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
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

/** Step 1 of adding a carousel page: which kind to start from (currently a blank page, see
 *  [HomeViewModel.addPage]); its own step so more starting templates fit in without changes here. */
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
