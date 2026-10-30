package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.PageRowType
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.dock.BackgroundFill
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark

private val MINI_ROW_RADIUS = 10.dp
private val MINI_SIDE_MARGIN = 4.dp
private val MINI_WIDGET_ROW_HEIGHT = 14.dp

/** A miniature of one carousel page drawn from its live settings: its background, then (on Home)
 *  the Panel's edge at the top and the Dock at the bottom, and every visible row in between in its
 *  real order, fill, corners and side margin. Tapping a part opens that part's own settings. */
@Composable
fun PagePreview(pageId: Long, isHome: Boolean, navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    val rowsByPage by viewModel.rowsByPage.collectAsState()
    val background by remember(pageId) { viewModel.pageBackgroundConfig(pageId) }.collectAsState(initial = BackgroundConfig())
    val pages by viewModel.pages.collectAsState()
    val rows = rowsByPage[pageId].orEmpty()
    // The Dock and the Panel are Home's and show on every page - previewed everywhere, but only
    // editable from Home.
    val homeRows = rowsByPage[pages.find { it.isHome }?.id].orEmpty()
    val dock = homeRows.find { it.type == PageRowType.SEARCH_BAR }
    val panel = homeRows.find { it.type == PageRowType.PANEL }
    val contentRows = rows.filter { it.enabled && it.type != PageRowType.SEARCH_BAR && it.type != PageRowType.PANEL }
    val surfaceDark = rememberSurfaceIsDark(ElementAppearance(fillMode = ElementFillMode.NONE), background)
    val phoneShape = RoundedCornerShape(24.dp)
    val base = "settings/pages/$pageId"

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(0.5f)
                .clip(phoneShape)
                .border(3.dp, MaterialTheme.colorScheme.outline, phoneShape)
        ) {
            // Background - a tap anywhere no other part covers opens the page's Appearance.
            BackgroundFill(background, Modifier.fillMaxSize())
            Box(
                modifier = Modifier.fillMaxSize().clickable {
                    navController.navigate(if (isHome) "$base/home/appearance" else "$base/appearance")
                }
            )

            Column(modifier = Modifier.fillMaxSize()) {
                if (panel != null && panel.enabled) {
                    // The Panel lives above the page, pulled down on demand - shown as its edge.
                    MiniPanelEdge(panel, surfaceDark, onClick = if (isHome) ({ navController.navigate("$base/home/panel") }) else null)
                } else {
                    Spacer(modifier = Modifier.height(18.dp))
                }
                Column(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    contentRows.forEach { row ->
                        MiniRow(row, surfaceDark) {
                            when (row.type) {
                                PageRowType.WIDGET, PageRowType.SYSTEM_WIDGET -> navController.navigate("$base/rows/${row.id}")
                                else -> navController.navigate("$base/rows/${row.id}/elementbox")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (dock != null && dock.enabled) {
                    MiniDock(dock, surfaceDark, onClick = if (isHome) ({ navController.navigate("$base/home/layout/dock") }) else null)
                }
            }
        }
    }
}

private fun miniRadius(percent: Int): Dp = MINI_ROW_RADIUS * (percent.coerceIn(0, 100) / 100f)

@Composable
private fun MiniRow(row: PageRow, surfaceDark: Boolean, onClick: () -> Unit) {
    val appearance = row.appearance()
    val colors = resolveElementColors(appearance, surfaceDark)
    val shape: Shape = RoundedCornerShape(miniRadius(row.cornerRadiusPercent))
    val side = if (row.edgeToEdge) 0.dp else MINI_SIDE_MARGIN
    val height = when (row.type) {
        PageRowType.ELEMENT_BOX -> if (row.pillMode) 20.dp else 30.dp
        else -> MINI_WIDGET_ROW_HEIGHT * row.spanY.coerceIn(1, 6)
    }
    Box(
        modifier = Modifier
            .padding(horizontal = side)
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(colors.background, alpha = colors.backgroundAlpha)
            .then(if (appearance.borderEnabled) Modifier.border(colors.borderWidth / 2, colors.borderColor, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (row.type == PageRowType.ELEMENT_BOX) {
            MiniDots(colors.contentColor, count = if (row.pillMode) 5 else row.columns.coerceIn(1, 6))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.fillMaxWidth(0.5f).height(5.dp).clip(CircleShape).background(colors.contentColor.copy(alpha = 0.45f)))
                Box(modifier = Modifier.fillMaxWidth(0.3f).height(4.dp).clip(CircleShape).background(colors.contentColor.copy(alpha = 0.25f)))
            }
        }
    }
}

@Composable
private fun MiniDots(color: androidx.compose.ui.graphics.Color, count: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(3.dp)).background(color.copy(alpha = 0.5f))) }
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

@Composable
private fun MiniDock(dock: PageRow, surfaceDark: Boolean, onClick: (() -> Unit)?) {
    val appearance = dock.appearance()
    val colors = resolveElementColors(appearance, surfaceDark)
    val radius = miniRadius(dock.cornerRadiusPercent)
    val shape = RoundedCornerShape(topStart = radius, topEnd = radius)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.background, alpha = colors.backgroundAlpha)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        MiniDots(colors.contentColor, count = 5)
    }
}
