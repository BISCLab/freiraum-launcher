package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ActionButtonActionType
import com.bisc.flauncher.data.model.GestureActionType
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.HomeGesture
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.pageLabel
import com.bisc.flauncher.ui.home.dock.PageIconGlyph
import com.bisc.flauncher.ui.home.dock.PageIconGlyphImage

/** How a page to open is reached: the gestures that open it, and whether a symbol leads there. */
internal data class PageWays(val gestures: List<HomeGesture>, val bySymbol: Boolean) {
    val reachable get() = gestures.isNotEmpty() || bySymbol
}

@Composable
internal fun rememberPageWays(page: LauncherPage, home: HomeViewModel): PageWays {
    val gestureActions by home.gestureActions.collectAsState()
    val buttons by home.actionButtons.collectAsState()
    val rowsByPage by home.rowsByPage.collectAsState()
    val pages by home.pages.collectAsState()
    val homeRows = rowsByPage[pages.find { it.isHome }?.id].orEmpty()
    // A swipe the Dock or the Panel takes opens no page, whatever is set for it.
    val dock = homeRows.find { it.type == com.bisc.flauncher.data.model.PageRowType.SEARCH_BAR }
    val panel = homeRows.find { it.type == com.bisc.flauncher.data.model.PageRowType.PANEL }
    val taken = buildSet {
        if (dock?.enabled == true && dock.dockVisibilityMode == com.bisc.flauncher.data.model.DockVisibilityMode.MANUAL) add(HomeGesture.SWIPE_UP)
        if (panel?.enabled == true) add(HomeGesture.SWIPE_DOWN)
    }
    val gestures = HomeGesture.entries.filter { g ->
        g !in taken && gestureActions[g]?.type == GestureActionType.PAGE && gestureActions[g]?.payload?.toLongOrNull() == page.id
    }
    val links = rememberUsages(GridItemType.PAGE, page.id.toString(), home)
    val button = buttons.any { it.actionType == ActionButtonActionType.PAGE && it.actionPageId == page.id }
    return PageWays(gestures, links.isNotEmpty() || button)
}

@Composable
private fun waysText(ways: PageWays): String = when {
    ways.gestures.isNotEmpty() -> ways.gestures.map { gestureLabel(it) }.joinToString() +
        if (ways.bySymbol) " · " + stringResource(R.string.pages_way_symbol) else ""
    ways.bySymbol -> stringResource(R.string.pages_way_symbol)
    else -> stringResource(R.string.pages_way_none)
}

/**
 * The pages as a small map: the pages to swipe in their row with the start page marked, above it
 * what a swipe down opens, below it what a swipe up opens, and the rest with how they open. A tap
 * on a page opens its settings.
 */
@Composable
internal fun PagesMap(rowsPages: List<LauncherPage>, openPages: List<LauncherPage>, allPages: List<LauncherPage>, home: HomeViewModel, onOpen: (LauncherPage) -> Unit) {
    val ways = openPages.associateWith { rememberPageWays(it, home) }
    val down = setOf(HomeGesture.SWIPE_DOWN, HomeGesture.TWO_FINGER_SWIPE_DOWN)
    val up = setOf(HomeGesture.SWIPE_UP, HomeGesture.TWO_FINGER_SWIPE_UP)
    val above = openPages.filter { p -> ways[p]!!.gestures.any { it in down } }
    val below = openPages.filter { p -> ways[p]!!.gestures.any { it in up } && p !in above }
    val rest = openPages - above.toSet() - below.toSet()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SettingsCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (above.isNotEmpty()) {
            MapRow(above.map { it to "↓ " + gestureLabel(ways[it]!!.gestures.first { g -> g in down }) }, allPages, onOpen)
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            rowsPages.forEachIndexed { i, p ->
                if (i > 0) Text("‹›", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MapPage(p, allPages, note = null, strong = p.isHome, onOpen = onOpen)
            }
        }
        if (below.isNotEmpty()) {
            MapRow(below.map { it to "↑ " + gestureLabel(ways[it]!!.gestures.first { g -> g in up }) }, allPages, onOpen)
        }
        if (rest.isNotEmpty()) {
            MapRow(rest.map { it to waysText(ways[it]!!) }, allPages, onOpen, warnFor = rest.filter { !ways[it]!!.reachable }.toSet())
        }
        Text(
            stringResource(R.string.pages_map_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MapRow(pages: List<Pair<LauncherPage, String>>, allPages: List<LauncherPage>, onOpen: (LauncherPage) -> Unit, warnFor: Set<LauncherPage> = emptySet()) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        pages.forEach { (p, note) -> MapPage(p, allPages, note, strong = false, warn = p in warnFor, onOpen = onOpen) }
    }
}

/** One page on the map: its colored mark, its name and, for pages to open, how. */
@Composable
private fun MapPage(page: LauncherPage, allPages: List<LauncherPage>, note: String?, strong: Boolean, onOpen: (LauncherPage) -> Unit, warn: Boolean = false) {
    val glyph = page.iconGlyph?.let { runCatching { PageIconGlyph.valueOf(it) }.getOrNull() } ?: PageIconGlyph.forPageKind(page.kind)
    val bg = page.iconBackgroundColor?.let { Color(it) } ?: com.bisc.flauncher.ui.home.dock.pageDefaultColor(page, glyph)
    val tint = page.iconColor?.let { Color(it) } ?: Color.White
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .clip(shape)
            .then(if (strong) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .clickable { onOpen(page) }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(bg), contentAlignment = Alignment.Center) {
            PageIconGlyphImage(glyph = glyph, tint = tint, modifier = Modifier.size(20.dp))
        }
        Text(pageLabel(page, allPages), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        if (note != null) {
            Text(
                note,
                style = MaterialTheme.typography.labelSmall,
                color = if (warn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }
}

/** The pages to open as a list: each with how it opens, or a friendly note and the way to a
 *  gesture when nothing opens it yet. */
@Composable
internal fun OpenPagesList(pages: List<LauncherPage>, allPages: List<LauncherPage>, home: HomeViewModel, onOpen: (LauncherPage) -> Unit, onGestures: () -> Unit) {
    SettingsPreferenceGroup {
        pages.forEach { page ->
            val ways = rememberPageWays(page, home)
            SettingsPreference(
                icon = null,
                title = pageLabel(page, allPages),
                summary = waysText(ways),
                onClick = { onOpen(page) },
                leading = {
                    val glyph = page.iconGlyph?.let { runCatching { PageIconGlyph.valueOf(it) }.getOrNull() } ?: PageIconGlyph.forPageKind(page.kind)
                    val bg = page.iconBackgroundColor?.let { Color(it) } ?: com.bisc.flauncher.ui.home.dock.pageDefaultColor(page, glyph)
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(bg), contentAlignment = Alignment.Center) {
                        PageIconGlyphImage(glyph = glyph, tint = page.iconColor?.let { Color(it) } ?: Color.White, modifier = Modifier.size(20.dp))
                    }
                },
                trailing = {
                    if (!ways.reachable) {
                        TextButton(onClick = onGestures) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.pages_way_set_gesture), modifier = Modifier.padding(start = 4.dp))
                        }
                    } else {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
            )
        }
    }
}
