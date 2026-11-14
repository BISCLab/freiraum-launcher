package com.bisc.flauncher.ui.home.search

import com.bisc.flauncher.ui.home.rememberAppIcon
import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.AppInfo
import com.bisc.flauncher.data.model.ContactInfo
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.PinnedShortcut
import com.bisc.flauncher.data.model.PinnedWebsite
import com.bisc.flauncher.data.model.SearchCategory
import com.bisc.flauncher.data.model.SettingsShortcut
import com.bisc.flauncher.ui.home.dock.DockPanelSurface
import com.bisc.flauncher.ui.theme.noRippleClickable
import androidx.compose.ui.res.stringResource

private const val APP_GRID_COLUMNS = 4
private const val APP_GRID_COLLAPSED_ROWS = 2
private const val SECTION_COLLAPSED_LIMIT = 3
/** Between the cards, and between the field and the first card. */
internal val SEARCH_CARD_GAP = 10.dp
private val CARD_GAP = SEARCH_CARD_GAP

/** Search results, one rounded card per category (Quick-Search-style), stacked with a small gap. */
@Composable
fun SearchResultsColumn(
    query: String,
    calculatorResult: String?,
    converterResult: String?,
    appResults: List<AppInfo>,
    contactResults: List<ContactInfo>,
    settingsResults: List<SettingsShortcut>,
    launcherSettingsResults: List<com.bisc.flauncher.data.model.LauncherSettingsTarget>,
    websiteResults: List<PinnedWebsite>,
    shortcutResults: List<com.bisc.flauncher.data.model.ShortcutResult>,
    folderResults: List<com.bisc.flauncher.data.model.Folder> = emptyList(),
    mapTargetResults: List<com.bisc.flauncher.data.model.SavedMapTarget> = emptyList(),
    contactsPermissionGranted: Boolean,
    order: List<SearchCategory>,
    active: Set<SearchCategory>,
    webChoices: List<CommandChoice>,
    aiChoices: List<CommandChoice>,
    cardAppearance: ElementAppearance,
    categoryColors: Map<SearchCategory, Int?>,
    loadIcon: (ComponentName) -> ImageBitmap?,
    onAppClick: (AppInfo) -> Unit,
    onContactClick: (ContactInfo) -> Unit,
    onSettingsShortcutClick: (SettingsShortcut) -> Unit,
    onLauncherSettingsClick: (com.bisc.flauncher.data.model.LauncherSettingsTarget) -> Unit,
    onFolderClick: (com.bisc.flauncher.data.model.Folder) -> Unit = {},
    onMapTargetClick: (com.bisc.flauncher.data.model.SavedMapTarget) -> Unit = {},
    onWebsiteClick: (PinnedWebsite) -> Unit,
    onShortcutClick: (PinnedShortcut) -> Unit,
    onWebCommand: (String) -> Unit,
    onMapsCommand: () -> Unit,
    onAiCommand: (String) -> Unit,
    onCalculatorResultClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Card corner roundness, 0-100% of [SearchDimens.cornerRadius]; null = fully rounded. */
    cardCornerRadiusPercent: Int? = null,
    /** Off = the same fine side margin every other element keeps. */
    edgeToEdge: Boolean = false,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    /** Favorites to show as their own card; empty = no card. */
    favoriteApps: List<AppInfo> = emptyList(),
    favoriteIds: Set<String> = emptySet(),
    onToggleFavorite: (AppInfo) -> Unit = {},
    /** The category picked in the rail: its card stays as it is, the others are greyed out. */
    focus: SearchCategory? = null,
    onClearFocus: () -> Unit = {},
    /** Each category with a card, and the position of its card in the list. */
    onCardsChange: (Map<SearchCategory, Int>) -> Unit = {},
    /** How far the card at a position has arrived while the search opens, 0 to 1. */
    arrival: (Int) -> Float = { 1f }
) {
    val sideMargin = if (edgeToEdge) 0.dp else com.bisc.flauncher.ui.home.dock.DockDimens.horizontalMargin
    val cardShape = RoundedCornerShape(SearchDimens.cornerRadius * ((cardCornerRadiusPercent ?: 100).coerceIn(0, 100) / 100f))
    var expandedCategories by remember(query) { mutableStateOf(emptySet<SearchCategory>()) }
    fun isExpanded(category: SearchCategory) = category in expandedCategories
    fun toggleExpanded(category: SearchCategory) {
        expandedCategories = if (isExpanded(category)) expandedCategories - category else expandedCategories + category
    }

    val shownAppResults = if (SearchCategory.APPS in active) appResults else emptyList()
    val shownContactResults = if (SearchCategory.CONTACTS in active && contactsPermissionGranted) contactResults else emptyList()
    val shownSettingsResults = if (SearchCategory.SETTINGS in active) settingsResults else emptyList()
    val shownLauncherSettingsResults = if (SearchCategory.LAUNCHER_SETTINGS in active) launcherSettingsResults else emptyList()
    val shownWebsiteResults = if (SearchCategory.WEBSITES in active) websiteResults else emptyList()
    val shownShortcutResults = if (SearchCategory.SHORTCUTS in active) shortcutResults else emptyList()
    val shownFolderResults = if (SearchCategory.FOLDERS in active) folderResults else emptyList()
    val shownMapTargetResults = if (SearchCategory.PLACES in active) mapTargetResults else emptyList()
    val showWeb = SearchCategory.WEB in active && webChoices.isNotEmpty() && query.isNotBlank()
    val showMaps = SearchCategory.PLACES in active && query.isNotBlank()
    val showAi = SearchCategory.AI in active && aiChoices.isNotEmpty() && query.isNotBlank()
    val hasDirectResults = shownAppResults.isNotEmpty() || shownContactResults.isNotEmpty() || shownSettingsResults.isNotEmpty() ||
        shownLauncherSettingsResults.isNotEmpty() ||
        shownWebsiteResults.isNotEmpty() || shownShortcutResults.isNotEmpty() ||
        shownFolderResults.isNotEmpty() || shownMapTargetResults.isNotEmpty()

    fun categoryColor(category: SearchCategory): Color = categoryColors[category]?.let { Color(it) } ?: category.defaultColor

    // Where each category's card sits in the list, worked out the same way the list below is built,
    // so the rail can jump to it.
    val cards = buildMap {
        var index = 0
        if (calculatorResult != null) index++
        if (converterResult != null) index++
        order.forEach { category ->
            val count = when (category) {
                SearchCategory.FAVORITES -> if (favoriteApps.isNotEmpty()) 1 else 0
                SearchCategory.APPS -> if (shownAppResults.isNotEmpty()) 1 else 0
                SearchCategory.FOLDERS -> if (shownFolderResults.isNotEmpty()) 1 else 0
                SearchCategory.CONTACTS -> if (shownContactResults.isNotEmpty()) 1 else 0
                SearchCategory.SETTINGS -> if (shownSettingsResults.isNotEmpty()) 1 else 0
                SearchCategory.WEBSITES -> if (shownWebsiteResults.isNotEmpty()) 1 else 0
                SearchCategory.PLACES -> (if (shownMapTargetResults.isNotEmpty()) 1 else 0) + (if (showMaps) 1 else 0)
                SearchCategory.SHORTCUTS -> if (shownShortcutResults.isNotEmpty()) 1 else 0
                SearchCategory.LAUNCHER_SETTINGS -> if (shownLauncherSettingsResults.isNotEmpty()) 1 else 0
                SearchCategory.WEB -> if (showWeb) 1 else 0
                SearchCategory.AI -> if (showAi) 1 else 0
            }
            if (count > 0) put(category, index)
            index += count
        }
    }
    androidx.compose.runtime.LaunchedEffect(cards) { onCardsChange(cards) }
    // Scrolling by hand ends the focus on one category.
    androidx.compose.runtime.LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { if (it is androidx.compose.foundation.interaction.DragInteraction.Start) onClearFocus() }
    }
    fun dimmed(category: SearchCategory) = focus != null && focus != category

    CompositionLocalProvider(LocalResultCardShape provides cardShape, LocalCardArrival provides arrival) {
    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        // Same edge fades as the app list: cards fade out toward the keyboard while there's more to
        // scroll, and toward the input bar once scrolled away from the top.
        val fadeStart = (1f - FADE_TRANSITION_DISTANCE / maxHeight).coerceIn(0f, 1f)
        val canScrollFurther by remember { derivedStateOf { listState.canScrollForward } }
        val topFadePx = with(LocalDensity.current) { TOP_FADE_HEIGHT.toPx() }
        // Fine side margin (or edge to edge); the gap to the input bar equals the gap between cards.
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .then(if (canScrollFurther) Modifier.fadeBackgroundToBottom(fadeStart, 1f) else Modifier)
                .fadeListTop(TOP_FADE_HEIGHT) {
                    if (listState.firstVisibleItemIndex > 0) 1f else (listState.firstVisibleItemScrollOffset / topFadePx).coerceIn(0f, 1f)
                },
            contentPadding = PaddingValues(
                start = sideMargin, end = sideMargin, top = CARD_GAP, bottom = CARD_GAP
            ),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(CARD_GAP)
        ) {
            if (calculatorResult != null) {
                item {
                    ResultCard(cardAppearance, order = 0, dimmed = focus != null, onUndim = onClearFocus) {
                        CalculatorResultRow(expression = query, result = calculatorResult, onClick = { onCalculatorResultClick(calculatorResult) })
                    }
                }
            }
            if (converterResult != null) {
                item {
                    ResultCard(cardAppearance, order = if (calculatorResult != null) 1 else 0, dimmed = focus != null, onUndim = onClearFocus) {
                        CalculatorResultRow(expression = query, result = converterResult, onClick = {})
                    }
                }
            }

            order.forEach { category ->
                when (category) {
                    SearchCategory.FAVORITES -> if (favoriteApps.isNotEmpty()) {
                        item(key = category.name) {
                            ResultCard(cardAppearance, order = cards[category] ?: 0, dimmed = dimmed(category), onUndim = onClearFocus) {
                                SectionTitle(stringResource(R.string.search_section_favorites), categoryColor(category))
                                AppTileGrid(
                                    apps = favoriteApps,
                                    expanded = true,
                                    loadIcon = loadIcon,
                                    onAppClick = onAppClick,
                                    onToggleExpand = {},
                                    favoriteIds = favoriteIds,
                                    onToggleFavorite = onToggleFavorite
                                )
                            }
                        }
                    }
                    SearchCategory.APPS -> if (shownAppResults.isNotEmpty()) {
                        item(key = category.name) {
                            ResultCard(cardAppearance, order = cards[category] ?: 0, dimmed = dimmed(category), onUndim = onClearFocus) {
                                SectionTitle(stringResource(R.string.search_section_apps), categoryColor(category))
                                AppTileGrid(
                                    apps = shownAppResults,
                                    expanded = isExpanded(category),
                                    loadIcon = loadIcon,
                                    onAppClick = onAppClick,
                                    onToggleExpand = { toggleExpanded(category) },
                                    favoriteIds = favoriteIds,
                                    onToggleFavorite = onToggleFavorite
                                )
                            }
                        }
                    }
                    SearchCategory.FOLDERS -> sectionCard(cards[category] ?: 0, dimmed(category), onClearFocus, category, shownFolderResults, categoryColor(category), cardAppearance, isExpanded(category), { toggleExpanded(category) }) { folder ->
                        ResultRow(leading = { Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(28.dp), tint = categoryColor(category)) }, title = folder.name, onClick = { onFolderClick(folder) })
                    }
                    SearchCategory.CONTACTS -> sectionCard(cards[category] ?: 0, dimmed(category), onClearFocus, category, shownContactResults, categoryColor(category), cardAppearance, isExpanded(category), { toggleExpanded(category) }) { contact ->
                        ResultRow(
                            leading = { Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(28.dp), tint = categoryColor(category)) },
                            title = contact.displayName,
                            subtitle = contact.email?.takeIf { contact.matchedByEmail },
                            onClick = { onContactClick(contact) }
                        )
                    }
                    SearchCategory.SETTINGS -> sectionCard(cards[category] ?: 0, dimmed(category), onClearFocus, category, shownSettingsResults, categoryColor(category), cardAppearance, isExpanded(category), { toggleExpanded(category) }) { shortcut ->
                        ResultRow(leading = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(28.dp), tint = categoryColor(category)) }, title = stringResource(shortcut.labelRes), onClick = { onSettingsShortcutClick(shortcut) })
                    }
                    SearchCategory.WEBSITES -> sectionCard(cards[category] ?: 0, dimmed(category), onClearFocus, category, shownWebsiteResults, categoryColor(category), cardAppearance, isExpanded(category), { toggleExpanded(category) }) { website ->
                        ResultRow(leading = { Icon(Icons.Filled.Bookmark, contentDescription = null, modifier = Modifier.size(28.dp), tint = categoryColor(category)) }, title = website.label, onClick = { onWebsiteClick(website) })
                    }
                    SearchCategory.PLACES -> {
                        sectionCard(cards[category] ?: 0, dimmed(category), onClearFocus, category, shownMapTargetResults, categoryColor(category), cardAppearance, isExpanded(category), { toggleExpanded(category) }) { target ->
                            ResultRow(
                                leading = { Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(28.dp), tint = categoryColor(category)) },
                                title = target.label,
                                subtitle = target.address.takeIf { it.isNotBlank() },
                                onClick = { onMapTargetClick(target) }
                            )
                        }
                        if (showMaps) {
                            item(key = "maps_command") {
                                ResultCard(cardAppearance, order = cards[category] ?: 0, dimmed = dimmed(category), onUndim = onClearFocus) {
                                    CommandRow(icon = Icons.Filled.Place, color = categoryColor(category), text = stringResource(R.string.search_command_maps, query), prominent = false, onClick = onMapsCommand)
                                }
                            }
                        }
                    }
                    SearchCategory.SHORTCUTS -> sectionCard(cards[category] ?: 0, dimmed(category), onClearFocus, category, shownShortcutResults, categoryColor(category), cardAppearance, isExpanded(category), { toggleExpanded(category) }) { result ->
                        ResultRow(
                            leading = { Icon(Icons.AutoMirrored.Filled.Shortcut, contentDescription = null, modifier = Modifier.size(28.dp), tint = categoryColor(category)) },
                            title = result.shortcut.label,
                            subtitle = result.appLabel.takeIf { it.isNotBlank() },
                            onClick = { onShortcutClick(result.shortcut) }
                        )
                    }
                    SearchCategory.LAUNCHER_SETTINGS -> sectionCard(cards[category] ?: 0, dimmed(category), onClearFocus, category, shownLauncherSettingsResults, categoryColor(category), cardAppearance, isExpanded(category), { toggleExpanded(category) }) { target ->
                        ResultRow(leading = { Icon(category.shownIcon(), contentDescription = null, modifier = Modifier.size(28.dp), tint = categoryColor(category)) }, title = stringResource(target.labelRes), onClick = { onLauncherSettingsClick(target) })
                    }
                    SearchCategory.WEB -> if (showWeb) {
                        item(key = category.name) {
                            ResultCard(cardAppearance, order = cards[category] ?: 0, dimmed = dimmed(category), onUndim = onClearFocus) {
                                val withEngine = stringResource(R.string.search_command_web_with)
                                CommandChoicesContent(webChoices, text = { withEngine.format(query, it.label) }, tint = categoryColor(category), prominent = !hasDirectResults, onRun = onWebCommand)
                            }
                        }
                    }
                    SearchCategory.AI -> if (showAi) {
                        item(key = category.name) {
                            ResultCard(cardAppearance, order = cards[category] ?: 0, dimmed = dimmed(category), onUndim = onClearFocus) {
                                val askApp = stringResource(R.string.search_command_ai)
                                CommandChoicesContent(aiChoices, text = { askApp.format(it.label, query) }, tint = categoryColor(category), prominent = !hasDirectResults && !showWeb, onRun = onAiCommand)
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

/** One category's section as its own card: title + up to [SECTION_COLLAPSED_LIMIT] rows + a "More" toggle. */
private fun <T> androidx.compose.foundation.lazy.LazyListScope.sectionCard(
    order: Int,
    dimmed: Boolean,
    onUndim: () -> Unit,
    category: SearchCategory,
    results: List<T>,
    color: Color,
    cardAppearance: ElementAppearance,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    row: @Composable (T) -> Unit
) {
    if (results.isEmpty()) return
    item(key = category.name) {
        ResultCard(cardAppearance, order = order, dimmed = dimmed, onUndim = onUndim) {
            SectionTitle(stringResource(category.sectionTitleRes), color)
            val shown = if (expanded || results.size <= SECTION_COLLAPSED_LIMIT) results else results.take(SECTION_COLLAPSED_LIMIT)
            shown.forEach { row(it) }
            if (results.size > SECTION_COLLAPSED_LIMIT) {
                TextButton(onClick = onToggleExpand) {
                    Text(stringResource(if (expanded) R.string.search_show_less else R.string.search_show_all))
                }
            }
        }
    }
}

private val SearchCategory.sectionTitleRes: Int
    get() = when (this) {
        SearchCategory.APPS -> R.string.search_section_apps
        SearchCategory.FOLDERS -> R.string.search_section_folders
        SearchCategory.CONTACTS -> R.string.search_section_contacts
        SearchCategory.SETTINGS -> R.string.search_section_settings
        SearchCategory.WEBSITES -> R.string.search_section_websites
        SearchCategory.PLACES -> R.string.search_section_places
        SearchCategory.SHORTCUTS -> R.string.search_section_shortcuts
        SearchCategory.LAUNCHER_SETTINGS -> R.string.search_section_launcher_settings
        SearchCategory.WEB -> R.string.search_section_apps
        SearchCategory.AI -> R.string.search_section_apps
        SearchCategory.FAVORITES -> R.string.search_section_favorites
    }

/** How far a card at a position has arrived while the search opens; see [SearchResultsColumn]. */
private val LocalCardArrival = staticCompositionLocalOf<(Int) -> Float> { { 1f } }

/** Every card's shape, provided once by [SearchResultsColumn] instead of threaded through each call. */
private val LocalResultCardShape = staticCompositionLocalOf { RoundedCornerShape(SearchDimens.cornerRadius) }

/** One result card. [dimmed] greys it out while another category is picked in the rail; a tap on
 *  it then only ends that, it doesn't open anything. */
@Composable
private fun ResultCard(appearance: ElementAppearance, order: Int = 0, dimmed: Boolean = false, onUndim: () -> Unit = {}, content: @Composable () -> Unit) {
    val alpha by androidx.compose.animation.core.animateFloatAsState(if (dimmed) 0.3f else 1f, label = "cardDim")
    val arrival = LocalCardArrival.current
    val rise = with(LocalDensity.current) { 18.dp.toPx() }
    DockPanelSurface(
        appearance = appearance,
        shape = LocalResultCardShape.current,
        effectTarget = "cards",
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            val p = arrival(order)
            // Fades without a layer of its own, so a 3D shadow below the card shows from the start.
            compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
            this.alpha = alpha * p
            translationY = (1f - p) * rise
        }
    ) {
        Box {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { content() }
            if (dimmed) Box(Modifier.matchParentSize().noRippleClickable(onClick = onUndim))
        }
    }
}

@Composable
private fun SectionTitle(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun AppTileGrid(
    apps: List<AppInfo>,
    expanded: Boolean,
    loadIcon: (ComponentName) -> ImageBitmap?,
    onAppClick: (AppInfo) -> Unit,
    onToggleExpand: () -> Unit,
    favoriteIds: Set<String> = emptySet(),
    onToggleFavorite: (AppInfo) -> Unit = {}
) {
    val collapsedLimit = APP_GRID_COLUMNS * APP_GRID_COLLAPSED_ROWS
    val shown = if (expanded || apps.size <= collapsedLimit) apps else apps.take(collapsedLimit)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        shown.chunked(APP_GRID_COLUMNS).forEach { rowApps ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowApps.forEach { app ->
                    val bitmap = rememberAppIcon(app.componentName, loadIcon)
                    val menu = com.bisc.flauncher.ui.home.rememberAppMenu(app.componentName, app.label)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .combinedClickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null,
                                onClick = { onAppClick(app) },
                                onLongClick = menu?.open
                            )
                            .padding(vertical = 6.dp, horizontal = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        menu?.Menu?.invoke()
                        if (bitmap != null) {
                            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(48.dp * (com.bisc.flauncher.ui.home.LocalIconSizeResolver.current?.invoke(app.componentName.flattenToString()) ?: 1f)))
                        } else {
                            Spacer(Modifier.size(48.dp))
                        }
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                repeat(APP_GRID_COLUMNS - rowApps.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (apps.size > collapsedLimit) {
            TextButton(onClick = onToggleExpand) {
                Text(
                    stringResource(
                        if (expanded) R.string.search_show_less else R.string.search_show_all
                    )
                )
            }
        }
    }
}

@Composable
private fun ResultRow(leading: @Composable () -> Unit, title: String, onClick: () -> Unit, subtitle: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(title)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CalculatorResultRow(expression: String, result: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = expression.trim(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "= $result",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun CommandRow(icon: ImageVector, color: Color, text: String, prominent: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color)
        }
        Text(
            text = text,
            style = if (prominent) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
        )
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
