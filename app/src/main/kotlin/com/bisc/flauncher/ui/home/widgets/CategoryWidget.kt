package com.bisc.flauncher.ui.home.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.CategoryWidgetConfig
import com.bisc.flauncher.data.model.CategoryWidgetMode
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.PageRow
import com.bisc.flauncher.data.model.ResolvedCategory
import com.bisc.flauncher.data.model.appearance
import com.bisc.flauncher.ui.home.CategoryChip
import com.bisc.flauncher.ui.home.CategoryMemberGrid
import com.bisc.flauncher.ui.home.HomeViewModel
import com.bisc.flauncher.ui.home.LauncherCategories
import com.bisc.flauncher.ui.home.categoryColor
import com.bisc.flauncher.ui.home.categoryColumns
import com.bisc.flauncher.ui.home.categoryIcon
import com.bisc.flauncher.ui.home.inkOn
import kotlinx.coroutines.launch

/** A widget's choice while it is edited and not yet saved; the widget preview shows it. */
val LocalCategoryWidgetDraft = androidx.compose.runtime.compositionLocalOf<CategoryWidgetConfig?> { null }

/** The categories a widget shows, in its own order; all of them when it names none. */
internal fun widgetCategories(config: CategoryWidgetConfig, all: List<ResolvedCategory>): List<ResolvedCategory> =
    if (config.ids.isEmpty()) all else config.ids.mapNotNull { id -> all.find { it.category.id == id } }

/**
 * Categories as a widget: one category's apps ([CategoryWidgetMode.SINGLE]), several with tabs to
 * switch between them right in the widget ([CategoryWidgetMode.TABS]), or the categories as small
 * tiles that open the Categories page ([CategoryWidgetMode.TILES]), or several one below the other
 * ([CategoryWidgetMode.ROWS]). Tabs switch with the chips, by
 * swiping, or with the paging buttons, as the widget is set.
 */
@Composable
fun CategoryWidget(
    row: PageRow,
    horizontalMargin: Dp = 0.dp,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val all by viewModel.resolvedCategories.collectAsState()
    val configs by viewModel.categoryWidgetConfigs.collectAsState()
    val paging by viewModel.widgetPaging.collectAsState()
    val config = LocalCategoryWidgetDraft.current ?: configs[CategoryWidgetConfig.keyFor(row)] ?: CategoryWidgetConfig()
    val shown = widgetCategories(config, all)
    val appearance = row.appearance()
    val ink = com.bisc.flauncher.ui.home.dock.resolveElementColors(appearance).contentColor

    WidgetCard(appearance = appearance, cornerRadiusPercent = row.cornerRadiusPercent, horizontalMargin = horizontalMargin, contentPadding = 10.dp) {
        if (shown.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().clickable { LauncherCategories.open(LauncherCategories.ALL) }, contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.widget_categories_empty), color = ink, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
            }
            return@WidgetCard
        }
        when (config.mode) {
            CategoryWidgetMode.TILES -> CategoryTiles(shown, ink, config.counts)
            CategoryWidgetMode.ROWS -> CategoryRows(shown, ink, Modifier.weight(1f), viewModel, config.symbols, config.counts)
            CategoryWidgetMode.SINGLE -> {
                val r = shown.first()
                CategoryHead(r, ink, config.symbols)
                MembersArea(r, ink, Modifier.weight(1f), viewModel)
            }
            CategoryWidgetMode.TABS -> {
                val pagerState = rememberPagerState(pageCount = { shown.size })
                val scope = rememberCoroutineScope()
                val chipsState = rememberLazyListState()
                val slide = androidx.compose.animation.core.tween<Float>(280, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    if (config.chips) {
                        LazyRow(
                            state = chipsState,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(shown, key = { it.category.id }) { r ->
                                val index = shown.indexOf(r)
                                CategoryChip(
                                    category = r.category,
                                    label = r.category.name,
                                    count = null,
                                    selected = pagerState.currentPage == index,
                                    look = ElementAppearance(fillMode = com.bisc.flauncher.data.model.ElementFillMode.NONE, contentColor = ink.toArgb()),
                                    onClick = { scope.launch { pagerState.animateScrollToPage(index, animationSpec = slide) } },
                                    showSymbol = config.symbols
                                )
                            }
                        }
                        LaunchedEffect(pagerState) {
                            snapshotFlow { pagerState.currentPage }.collect { chipsState.animateScrollToItem(it) }
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f)) { CategoryHead(shown[pagerState.currentPage.coerceIn(0, shown.lastIndex)], ink, config.symbols) }
                    }
                    if (row.pagingButtons && shown.size > 1) {
                        PagingButton(
                            forward = false, width = 28.dp, height = 28.dp, style = paging,
                            enabled = pagerState.currentPage > 0,
                            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1, animationSpec = slide) } },
                            modifier = Modifier.padding(start = 6.dp)
                        )
                        PagingButton(
                            forward = true, width = 28.dp, height = 28.dp, style = paging,
                            enabled = pagerState.currentPage < shown.size - 1,
                            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1, animationSpec = slide) } },
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.size(6.dp))
                HorizontalPager(state = pagerState, userScrollEnabled = !row.pagingButtons, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                    MembersArea(shown[page], ink, Modifier.fillMaxSize(), viewModel)
                }
            }
        }
    }
}

/** A category's symbol and name; a tap opens it on the Categories page. */
@Composable
private fun CategoryHead(r: ResolvedCategory, ink: androidx.compose.ui.graphics.Color, symbols: Boolean) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        com.bisc.flauncher.ui.home.CategoryNamePill(
            r.category,
            MaterialTheme.typography.labelLarge,
            modifier = Modifier.clickable { LauncherCategories.open(r.category.id) },
            ink = ink,
            iconSize = 16.dp,
                                showSymbol = symbols
        )
    }
}

/** The members of one category in a grid as wide as the widget; more than fit can be scrolled. */
@Composable
private fun MembersArea(r: ResolvedCategory, ink: androidx.compose.ui.graphics.Color, modifier: Modifier, viewModel: HomeViewModel) {
    BoxWithConstraints(modifier = modifier) {
        val columns = categoryColumns(maxWidth)
        if (r.members.isEmpty()) {
            Text(stringResource(R.string.categories_nothing), color = ink, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.Center))
        } else {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                CategoryMemberGrid(r.members, columns, ink, viewModel = viewModel)
            }
        }
    }
}

/** Several categories one below the other, each with its name and its grid. A fling stops at the
 *  start of a category, so none is left cut in half at the top. */
@Composable
private fun CategoryRows(shown: List<ResolvedCategory>, ink: androidx.compose.ui.graphics.Color, modifier: Modifier, viewModel: HomeViewModel, symbols: Boolean, counts: Boolean) {
    val listState = rememberLazyListState()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = categoryColumns(maxWidth)
        LazyColumn(
            state = listState,
            flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(listState),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(shown, key = { it.category.id }) { r ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            com.bisc.flauncher.ui.home.CategoryNamePill(
                                r.category,
                                MaterialTheme.typography.labelLarge,
                                modifier = Modifier.clickable { LauncherCategories.open(r.category.id) },
                                ink = ink,
                                iconSize = 16.dp,
                                showSymbol = symbols
                            )
                        }
                        if (counts) {
                            Text(
                                r.members.size.toString(),
                                color = ink.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                            )
                        }
                    }
                    Spacer(Modifier.size(4.dp))
                    if (r.members.isEmpty()) {
                        Text(stringResource(R.string.categories_nothing), color = ink, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    } else {
                        CategoryMemberGrid(r.members, columns, ink, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

/** The categories as small tiles with symbol, name and count. */
@Composable
private fun CategoryTiles(shown: List<ResolvedCategory>, ink: androidx.compose.ui.graphics.Color, counts: Boolean) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = (maxWidth / 96.dp).toInt().coerceIn(2, 6)
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            shown.chunked(columns).forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    line.forEach { r ->
                        val accent = categoryColor(r.category)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                                .background(accent.copy(alpha = if (com.bisc.flauncher.ui.AccessibilityMode.on) 1f else 0.18f))
                                .clickable { LauncherCategories.open(r.category.id) }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            com.bisc.flauncher.ui.home.CategoryMark(r.category, modifier = Modifier.size(32.dp), ink = ink)
                            Spacer(Modifier.size(6.dp))
                            Text(
                                r.category.name,
                                color = if (com.bisc.flauncher.ui.AccessibilityMode.on) inkOn(accent) else ink,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            if (counts) {
                                Text(
                                    r.members.size.toString(),
                                    color = (if (com.bisc.flauncher.ui.AccessibilityMode.on) inkOn(accent) else ink).copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    repeat(columns - line.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
