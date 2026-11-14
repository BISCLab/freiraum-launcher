package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.RowEntry
import com.bisc.flauncher.data.model.WidgetKind
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.PageRowDimens
import com.bisc.flauncher.ui.home.dock.BackgroundFill
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.home.rememberAppIcon
import com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark

private val MINI_ROW_RADIUS = 10.dp
private val MINI_SIDE_MARGIN = 4.dp
private val STATUS_BAR = 40.dp
private val ICON = 56.dp

/** A miniature of one carousel page drawn from its live settings, true to scale: its background,
 *  then (on Home) the Panel's edge at the top and the Dock at the bottom, and every visible row in
 *  between in its real order, height, fill, corners and side margin, with what it shows: the real
 *  app icons, the widget's own symbol, an Android widget's app icon and name. Tapping a part opens
 *  that part's own settings. */
@Composable
fun PagePreview(
    pageId: Long,
    isHome: Boolean,
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    /** The row picked in the small page, drawn with an accent edge. */
    selectedRowId: Long? = null,
    /** A tap on a row picks it instead of opening it; null = it opens. */
    onRowTap: ((PageRow) -> Unit)? = null
) {
    val rowsByPage by viewModel.rowsByPage.collectAsState()
    val entriesByRow by viewModel.rowEntriesByRow.collectAsState()
    val background by remember(pageId) { viewModel.pageBackgroundConfig(pageId) }.collectAsState(initial = BackgroundConfig())
    val pages by viewModel.pages.collectAsState()
    val rows = rowsByPage[pageId].orEmpty()
    // The Dock and the Panel are Home's and show on every page - previewed everywhere, but only
    // editable from Home.
    val homeRows = rowsByPage[pages.find { it.isHome }?.id].orEmpty()
    val dock = homeRows.find { it.type == PageRowType.SEARCH_BAR }
    val panel = homeRows.find { it.type == PageRowType.PANEL }
    val contentRows = rows.filter { it.enabled && it.type != PageRowType.SEARCH_BAR && it.type != PageRowType.PANEL }
    val withoutDockPanel by viewModel.pagesWithoutDockPanel.collectAsState()
    val unitCounts by viewModel.rowUnitCounts.collectAsState()
    val surfaceDark = rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), background)
    val phoneShape = RoundedCornerShape(24.dp)
    val base = "settings/pages/$pageId"
    // The whole screen, bars included, as the page is laid out on it.
    val density = LocalDensity.current
    val rootView = androidx.compose.ui.platform.LocalView.current.rootView
    // Held upright, like the page is counted (see PageGrid): the same page whichever way the device
    // is turned while the settings are open.
    val rawHeight = with(density) { rootView.height.toDp() }.takeIf { it > 0.dp } ?: LocalConfiguration.current.screenHeightDp.dp
    val rawWidth = with(density) { rootView.width.toDp() }.takeIf { it > 0.dp } ?: LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = maxOf(rawHeight, rawWidth)
    val screenWidth = minOf(rawHeight, rawWidth)
    val shownDock = dock?.takeIf { it.enabled && pageId !in withoutDockPanel.first }
    val shownPanel = panel?.takeIf { pageId !in withoutDockPanel.second }
    // Rows stretch to fill the page like on the phone (see HomeScreen): from below the status bar
    // down to one row gap above the Dock.
    // The same measures as on the phone (see HomeScreen): rows start 16 dp below the status bar and
    // end one row gap above the Dock, which sits on the navigation bar.
    val navBar = androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val topInset = maxOf(
        androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
        androidx.compose.foundation.layout.WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    ).takeIf { it > 0.dp } ?: STATUS_BAR
    val dockHeight = shownDock?.let { com.bisc.flauncher.ui.home.dock.DockDimens.height(it) + navBar }
    val bottomReserve = dockHeight?.let { it + PageRowDimens.rowSpacing } ?: (navBar + 16.dp)
    val unit = PageRowDimens.stretchedUnit(screenHeight - topInset - 16.dp - bottomReserve, unitCounts[pageId] ?: 0)

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio((screenWidth / screenHeight).coerceIn(0.4f, 0.75f))
                .clip(phoneShape)
                .border(3.dp, MaterialTheme.colorScheme.outline, phoneShape)
        ) {
            // Everything at the phone's own proportions, scaled down.
            val scale = (maxHeight / screenHeight).coerceIn(0.1f, 1f)
            // Background - a tap anywhere no other part covers opens the page's Appearance.
            BackgroundFill(background, Modifier.fillMaxSize())
            Box(
                modifier = Modifier.fillMaxSize().clickable {
                    navController.navigate(if (isHome) "$base/home/appearance" else "$base/appearance")
                }
            )

            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.padding(top = (topInset + 16.dp) * scale),
                    verticalArrangement = Arrangement.spacedBy(PageRowDimens.rowSpacing * scale)
                ) {
                    contentRows.forEach { row ->
                        MiniRow(row, entriesByRow[row.id].orEmpty(), surfaceDark, scale, unit, viewModel, picked = row.id == selectedRowId) {
                            if (onRowTap != null) onRowTap(row) else when (row.type) {
                                PageRowType.WIDGET, PageRowType.SYSTEM_WIDGET -> navController.navigate("$base/rows/${row.id}")
                                else -> navController.navigate("$base/rows/${row.id}/elementbox")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (shownDock != null && dockHeight != null) {
                    MiniDock(shownDock, entriesByRow[dock.id].orEmpty(), surfaceDark, scale, dockHeight * scale, viewModel, onClick = if (isHome) ({ navController.navigate("$base/home/layout/dock") }) else null)
                }
            }
            // The Panel lies over the page, pulled down on demand; shown as its edge, taking no room.
            if (shownPanel != null && shownPanel.enabled) {
                MiniPanelEdge(shownPanel, surfaceDark, onClick = if (isHome) ({ navController.navigate("$base/home/panel") }) else null)
            }
        }
    }
}

private fun miniRadius(percent: Int): Dp = MINI_ROW_RADIUS * (percent.coerceIn(0, 100) / 100f)

/** One row in small at its real height: an element box with its real icons in its columns, a
 *  widget with its own symbol, an Android widget with its app's icon and name. */
@Composable
private fun MiniRow(row: PageRow, entries: List<RowEntry>, surfaceDark: Boolean, scale: Float, unit: Dp, viewModel: HomeViewModel, picked: Boolean = false, onClick: () -> Unit) {
    val appearance = row.appearance()
    val colors = resolveElementColors(appearance, surfaceDark)
    val shape: Shape = RoundedCornerShape(miniRadius(row.cornerRadiusPercent))
    val side = if (row.edgeToEdge) 0.dp else MINI_SIDE_MARGIN
    val density = LocalDensity.current
    val realHeight = when (row.type) {
        PageRowType.ELEMENT_BOX -> {
            val natural = if (row.pillMode) ICON + 24.dp else PageRowDimens.elementBoxHeight(entries.size, row.columns.coerceAtLeast(1))
            PageRowDimens.spanHeight(unit, PageRowDimens.unitsFor(natural, unit))
        }
        // Only an Android widget may have an exact height of its own, like on the phone.
        PageRowType.SYSTEM_WIDGET -> row.heightPx?.takeIf { it > 0 }?.let { with(density) { it.toDp() } } ?: PageRowDimens.spanHeight(unit, row.spanY.coerceAtLeast(1))
        else -> PageRowDimens.spanHeight(unit, row.spanY.coerceAtLeast(1))
    }
    Box(
        modifier = Modifier
            .padding(horizontal = side)
            .fillMaxWidth()
            .height(realHeight * scale)
            .clip(shape)
            .background(colors.background, alpha = colors.backgroundAlpha)
            .then(if (appearance.borderEnabled) Modifier.border(colors.borderWidth / 2, colors.borderColor, shape) else Modifier)
            .then(if (picked) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        when (row.type) {
            PageRowType.ELEMENT_BOX -> MiniIcons(entries, if (row.pillMode) 5 else row.columns.coerceIn(1, 6), colors.contentColor, scale, viewModel)
            PageRowType.SYSTEM_WIDGET -> MiniAndroidWidget(row.payload.toIntOrNull(), colors.contentColor, viewModel)
            else -> MiniOwnWidget(row.payload, colors.contentColor)
        }
    }
}

/** The real icons of a row's apps in its columns; other kinds of entries as neutral tiles. */
@Composable
private fun MiniIcons(entries: List<RowEntry>, columns: Int, ink: Color, scale: Float, viewModel: HomeViewModel) {
    val size = (ICON * scale).coerceAtLeast(8.dp)
    val shown: List<RowEntry?> = entries.sortedBy { it.orderIndex }.ifEmpty { List(columns) { null } }
    Column(verticalArrangement = Arrangement.spacedBy(size / 3)) {
        shown.chunked(columns).forEach { line ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                line.forEach { entry ->
                    val component = entry?.takeIf { it.type == GridItemType.APP }?.let { ComponentName.unflattenFromString(it.payload) }
                    val icon = rememberAppIcon(component, viewModel::loadIcon)
                    if (icon != null) {
                        Image(icon, contentDescription = null, modifier = Modifier.size(size))
                    } else {
                        Box(modifier = Modifier.size(size).clip(RoundedCornerShape(size / 3)).background(ink.copy(alpha = 0.35f)))
                    }
                }
                repeat(columns - line.size) { Spacer(Modifier.size(size)) }
            }
        }
    }
}

/** Freiraum's own widgets by their symbol; the clock shows a time. */
@Composable
private fun MiniOwnWidget(kind: String, ink: Color) {
    when (kind) {
        WidgetKind.SPACER -> Unit
        WidgetKind.CLOCK -> Text("12:30", color = ink, style = MaterialTheme.typography.titleMedium)
        else -> Icon(
            when (kind) {
                WidgetKind.WEATHER -> Icons.Filled.Cloud
                WidgetKind.MUSIC -> Icons.Filled.MusicNote
                else -> Icons.Filled.EditNote
            },
            contentDescription = null,
            tint = ink.copy(alpha = 0.8f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/** An Android widget by its app's icon and its name. */
@Composable
private fun MiniAndroidWidget(appWidgetId: Int?, ink: Color, viewModel: HomeViewModel) {
    val context = LocalContext.current
    val info = remember(appWidgetId) { appWidgetId?.let(viewModel::getSystemWidgetInfo) }
    val icon = remember(info) {
        info?.let { runCatching { it.loadIcon(context, context.resources.displayMetrics.densityDpi)?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull() }
    }
    val label = remember(info) { info?.loadLabel(context.packageManager) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (icon != null) Image(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        if (label != null) {
            Text(label, color = ink.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MiniPanelEdge(panel: PageRow, surfaceDark: Boolean, onClick: (() -> Unit)?) {
    val colors = resolveElementColors(panel.appearance(), surfaceDark)
    val radius = miniRadius(panel.cornerRadiusPercent)
    val shape = RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .clip(shape)
            .background(colors.background, alpha = colors.backgroundAlpha)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.BottomCenter
    ) {
        // The pull-down handle.
        Box(modifier = Modifier.padding(bottom = 5.dp).fillMaxWidth(0.25f).height(3.dp).clip(CircleShape).background(colors.contentColor.copy(alpha = 0.5f)))
    }
}

/** The Dock with its real icons. */
@Composable
private fun MiniDock(dock: PageRow, entries: List<RowEntry>, surfaceDark: Boolean, scale: Float, height: Dp, viewModel: HomeViewModel, onClick: (() -> Unit)?) {
    val appearance = dock.appearance()
    val colors = resolveElementColors(appearance, surfaceDark)
    val radius = miniRadius(dock.cornerRadiusPercent)
    val shape = RoundedCornerShape(topStart = radius, topEnd = radius)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Its real height, the navigation bar under it included.
            .height(height)
            .clip(shape)
            .background(colors.background, alpha = colors.backgroundAlpha)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 6.dp)
            .padding(top = com.bisc.flauncher.ui.home.dock.DockDimens.topPadding * scale),
        contentAlignment = Alignment.TopCenter
    ) {
        MiniIcons(entries.take(5), 5, colors.contentColor, scale, viewModel)
    }
}
