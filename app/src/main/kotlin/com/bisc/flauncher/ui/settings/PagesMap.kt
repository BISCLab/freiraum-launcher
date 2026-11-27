package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ActionButtonActionType
import com.bisc.flauncher.data.model.DockVisibilityMode
import com.bisc.flauncher.data.model.GestureAction
import com.bisc.flauncher.data.model.GestureActionType
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.HomeGesture
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.data.model.LauncherPageKind
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.dock.PageIconGlyph
import com.bisc.flauncher.ui.home.dock.PageIconGlyphImage
import com.bisc.flauncher.ui.pageLabel

/** How a function page is reached: the gestures that open it, and whether a symbol leads there. */
internal data class PageWays(val gestures: List<HomeGesture>, val bySymbol: Boolean) {
    val reachable get() = gestures.isNotEmpty() || bySymbol
}

/** The swipes the Dock and the Panel take for themselves, with why; they open no page. */
@Composable
internal fun rememberTakenGestures(home: HomeViewModel): Map<HomeGesture, String> {
    val rowsByPage by home.rowsByPage.collectAsState()
    val pages by home.pages.collectAsState()
    val homeRows = rowsByPage[pages.find { it.isHome }?.id].orEmpty()
    val dock = homeRows.find { it.type == PageRowType.SEARCH_BAR }
    val panel = homeRows.find { it.type == PageRowType.PANEL }
    val byDock = stringResource(R.string.settings_gesture_locked_by_dock)
    val byPanel = stringResource(R.string.settings_gesture_locked_by_panel)
    return buildMap {
        if (dock?.enabled == true && dock.dockVisibilityMode == DockVisibilityMode.MANUAL) put(HomeGesture.SWIPE_UP, byDock)
        if (panel?.enabled == true) put(HomeGesture.SWIPE_DOWN, byPanel)
    }
}

@Composable
internal fun rememberPageWays(page: LauncherPage, home: HomeViewModel): PageWays {
    val gestureActions by home.gestureActions.collectAsState()
    val buttons by home.actionButtons.collectAsState()
    val taken = rememberTakenGestures(home)
    val gestures = HomeGesture.entries.filter { g ->
        g !in taken && gestureActions[g]?.type == GestureActionType.PAGE && gestureActions[g]?.payload?.toLongOrNull() == page.id
    }
    val links = rememberUsages(GridItemType.PAGE, page.id.toString(), home)
    val button = buttons.any { it.actionType == ActionButtonActionType.PAGE && it.actionPageId == page.id }
    return PageWays(gestures, links.isNotEmpty() || button)
}

/** What "Open with" says: the first gesture, else a symbol, else that there is no way yet. */
@Composable
private fun waysLine(ways: PageWays): Pair<ImageVector, String> = when {
    ways.gestures.isNotEmpty() -> gestureIcon(ways.gestures.first()) to gestureLabel(ways.gestures.first())
    ways.bySymbol -> Icons.Filled.Apps to stringResource(R.string.pages_way_symbol)
    else -> Icons.Filled.ErrorOutline to stringResource(R.string.pages_way_none)
}

/** A page's colored mark, as on its tile. */
@Composable
internal fun PageMark(page: LauncherPage, size: androidx.compose.ui.unit.Dp) {
    val glyph = page.iconGlyph?.let { runCatching { PageIconGlyph.valueOf(it) }.getOrNull() } ?: PageIconGlyph.forPageKind(page.kind)
    val bg = page.iconBackgroundColor?.let { Color(it) } ?: com.bisc.flauncher.ui.home.dock.pageDefaultColor(page, glyph)
    Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(bg), contentAlignment = Alignment.Center) {
        PageIconGlyphImage(glyph = glyph, tint = page.iconColor?.let { Color(it) } ?: Color.White, modifier = Modifier.size(size * 0.55f))
    }
}

/** The line "Open with …" as a chip: a tap asks which gesture opens the page. */
@Composable
private fun OpenWithChip(ways: PageWays, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (icon, text) = waysLine(ways)
    val none = !ways.reachable
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (none) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val ink = if (none) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** The function pages as tiles, two side by side, all the same size: each with its mark, its name
 *  and how it opens. The tile opens its settings, the line below asks which gesture opens it. */
@Composable
internal fun FunctionPagesGrid(pages: List<LauncherPage>, allPages: List<LauncherPage>, home: HomeViewModel, navController: NavController, onOpen: (LauncherPage) -> Unit) {
    var asking by remember { mutableStateOf<LauncherPage?>(null) }
    pages.chunked(2).forEach { pair ->
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            pair.forEach { page ->
                val ways = rememberPageWays(page, home)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable { onOpen(page) }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PageMark(page, 40.dp)
                    Text(pageLabel(page, allPages), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.weight(1f))
                    OpenWithChip(ways, onClick = { asking = page }, modifier = Modifier.fillMaxWidth())
                }
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainer))
        }
    }
    asking?.let { page -> OpenWithSheet(page, allPages, home, navController, onDismiss = { asking = null }) }
}

/** The block "Open with" on a function page's own settings: how it opens, and the way to change it. */
@Composable
internal fun OpenWithGroup(kind: LauncherPageKind, home: HomeViewModel, navController: NavController) {
    val allPages by home.allPages.collectAsState()
    val page = allPages.find { it.kind == kind } ?: return
    val ways = rememberPageWays(page, home)
    var asking by remember { mutableStateOf(false) }
    val (icon, text) = waysLine(ways)
    SettingsPreferenceGroup(title = stringResource(R.string.pages_open_with)) {
        SettingsPreference(
            icon = icon,
            title = text,
            summary = stringResource(R.string.pages_open_with_change),
            onClick = { asking = true },
            trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
        )
    }
    if (asking) OpenWithSheet(page, allPages, home, navController, onDismiss = { asking = false })
}

/**
 * "Open with which gesture?": every gesture with what it does now. A tap makes it open [page] right
 * away; a gesture that does something else asks first. Gestures the Dock or the Panel takes are
 * faint with the reason.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OpenWithSheet(page: LauncherPage, allPages: List<LauncherPage>, home: HomeViewModel, navController: NavController, onDismiss: () -> Unit) {
    val actions by home.gestureActions.collectAsState()
    val apps by home.browsableApps.collectAsState()
    val taken = rememberTakenGestures(home)
    var replacing by remember { mutableStateOf<HomeGesture?>(null) }
    val pageName = pageLabel(page, allPages)
    val opensThis = { g: HomeGesture -> actions[g]?.type == GestureActionType.PAGE && actions[g]?.payload?.toLongOrNull() == page.id }
    val assign = { g: HomeGesture -> home.setGestureAction(g, GestureAction(GestureActionType.PAGE, page.id.toString())); onDismiss() }
    ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = com.bisc.flauncher.ui.sheetMaxHeight())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.pages_open_with_title, pageName), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 4.dp))
            SettingsPreferenceGroup {
                HomeGesture.entries.forEach { g ->
                    val reason = taken[g]
                    val current = actions[g] ?: GestureAction.NONE
                    val mine = opensThis(g)
                    SettingsPreference(
                        icon = gestureIcon(g),
                        title = gestureLabel(g),
                        summary = reason ?: gestureActionSummary(current, allPages, apps),
                        enabled = reason == null,
                        // The one it has already: to its own options, to change or clear it.
                        onClick = when {
                            reason != null -> null
                            mine -> ({ onDismiss(); navController.navigate("settings/gestures/${g.name}") })
                            else -> ({ if (current.type == GestureActionType.NONE) assign(g) else replacing = g })
                        },
                        trailing = if (mine) ({ Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }) else null
                    )
                }
            }
            TextButton(onClick = { onDismiss(); navController.navigate("settings/gestures") }) {
                Text(stringResource(R.string.pages_open_with_all_gestures))
            }
        }
    }
    replacing?.let { g ->
        val current = actions[g] ?: GestureAction.NONE
        AlertDialog(
            onDismissRequest = { replacing = null },
            title = { Text(gestureLabel(g)) },
            text = { Text(stringResource(R.string.pages_open_with_replace, gestureActionSummary(current, allPages, apps), pageName)) },
            confirmButton = { TextButton(onClick = { replacing = null; assign(g) }) { Text(stringResource(R.string.pages_open_with_replace_ok)) } },
            dismissButton = { TextButton(onClick = { replacing = null }) { Text(stringResource(R.string.widget_notes_cancel)) } }
        )
    }
}

/** How many gestures do something. */
@Composable
internal fun rememberGesturesInUse(home: HomeViewModel): Int {
    val actions by home.gestureActions.collectAsState()
    val taken = rememberTakenGestures(home)
    return HomeGesture.entries.count { it in taken || (actions[it]?.type ?: GestureActionType.NONE) != GestureActionType.NONE }
}
