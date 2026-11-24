package com.bisc.flauncher.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.CategoryMember
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.GridItemType
import com.bisc.flauncher.data.model.ResolvedCategory
import com.bisc.flauncher.data.model.RowEntry
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.theme.PartCornerShape
import com.bisc.flauncher.ui.theme.designGlow
import kotlinx.coroutines.launch

/** Cells narrower than this get fewer columns. */
private val CATEGORY_CELL_MIN_WIDTH = 78.dp

/** How many members fit side by side in [width]. */
internal fun categoryColumns(width: Dp): Int = (width / CATEGORY_CELL_MIN_WIDTH).toInt().coerceIn(3, 8)

/** The members of the given categories together, each element once. */
internal fun unionMembers(categories: List<ResolvedCategory>): List<CategoryMember> {
    val seen = HashSet<Pair<GridItemType, String>>()
    return categories.flatMap { it.members }.filter { seen.add(it.type to it.payload) }
}

/** The name of a member as shown, for searching. */
@Composable
internal fun rememberMemberLabeler(viewModel: HomeViewModel): (CategoryMember) -> String {
    val websites by viewModel.pinnedWebsites.collectAsState()
    val places by viewModel.savedMapTargets.collectAsState()
    val contacts by viewModel.pinnedContacts.collectAsState()
    return remember(websites, places, contacts) {
        { member ->
            when (member.type) {
                GridItemType.APP -> android.content.ComponentName.unflattenFromString(member.payload)?.let(viewModel::resolveLabel).orEmpty()
                GridItemType.WEBSITE -> websites.find { it.id == member.payload.toLongOrNull() }?.label.orEmpty()
                GridItemType.MAP_TARGET -> places.find { it.id == member.payload.toLongOrNull() }?.label.orEmpty()
                GridItemType.CONTACT -> contacts.find { it.contactId == member.payload.toLongOrNull() }?.displayName.orEmpty()
                GridItemType.SHORTCUT -> member.payload.split("|").getOrNull(2).orEmpty()
                else -> ""
            }
        }
    }
}

/** Members as cells, the same cells the element boxes use, [columns] side by side. */
@Composable
internal fun CategoryMemberGrid(
    members: List<CategoryMember>,
    columns: Int,
    labelColor: Color,
    iconSizeScale: Float = 1f,
    showLabels: Boolean = true,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val websites by viewModel.pinnedWebsites.collectAsState()
    val places by viewModel.savedMapTargets.collectAsState()
    val contacts by viewModel.pinnedContacts.collectAsState()
    val entries = remember(members, websites, places, contacts) {
        members.filter { m ->
            when (m.type) {
                GridItemType.WEBSITE -> websites.any { it.id == m.payload.toLongOrNull() }
                GridItemType.MAP_TARGET -> places.any { it.id == m.payload.toLongOrNull() }
                GridItemType.CONTACT -> contacts.any { it.contactId == m.payload.toLongOrNull() }
                else -> true
            }
        }.mapIndexed { index, m -> RowEntry(id = -(index + 1L), pageRowId = -1L, orderIndex = index, type = m.type, payload = m.payload) }
    }
    CompositionLocalProvider(LocalAppIconLoader provides viewModel.iconLoader) {
        GridEntries(
            entries = entries,
            columns = columns,
            folders = emptyList(),
            folderItemsByFolder = emptyMap(),
            savedMapTargets = places,
            pinnedWebsites = websites,
            pages = emptyList(),
            loadIcon = viewModel::loadIcon,
            loadPackIcon = viewModel::loadPackIcon,
            resolveLabel = viewModel::resolveLabel,
            onFolderClick = {},
            onShortcutClick = viewModel::launchFolderShortcut,
            onOpenPageLink = { _, _ -> },
            pinnedContacts = contacts,
            loadContactPhoto = viewModel::loadContactPhoto,
            onContactClick = { contactId ->
                contacts.find { it.contactId == contactId }?.let { contact ->
                    com.bisc.flauncher.util.openContact(
                        context,
                        com.bisc.flauncher.data.model.ContactInfo(contact.contactId, contact.lookupKey, contact.displayName, contact.photoUri)
                    )
                }
            },
            showLabels = showLabels,
            labelColor = labelColor,
            iconSizeScale = iconSizeScale
        )
    }
}

/** One chip: the category's symbol and name, filled with its color while chosen. [category] null
 *  is "All". */
@Composable
internal fun CategoryChip(
    category: com.bisc.flauncher.data.model.Category?,
    label: String,
    count: Int?,
    selected: Boolean,
    look: ElementAppearance,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = resolveElementColors(look)
    val accent = categoryColor(category)
    val shape = androidx.compose.foundation.shape.CircleShape
    val ink = if (selected) inkOn(accent) else colors.contentColor
    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .then(if (selected) Modifier.background(accent) else Modifier.background(colors.background, alpha = colors.backgroundAlpha))
            .then(if (colors.showBorder && !selected) Modifier.border(colors.borderWidth, colors.borderColor, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (category == null) {
            Icon(androidx.compose.material.icons.Icons.Filled.Apps, contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
        } else if (category.hasSymbol) {
            CategorySymbol(category, 18.dp, if (selected) ink else accent)
        }
        Text(label, color = ink, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        if (count != null) {
            Text(count.toString(), color = ink.copy(alpha = if (com.bisc.flauncher.ui.AccessibilityMode.on) 1f else 0.7f), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** A card in the look of the element boxes, holding one category or a choice of them. */
@Composable
internal fun CategoryCard(
    look: ElementAppearance,
    cornerRadiusPercent: Int,
    modifier: Modifier = Modifier,
    animateSize: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = resolveElementColors(look)
    val shape = PartCornerShape(cornerRadiusPercent)
    CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .then(Modifier.designGlow(shape, "widgets"))
                .clip(shape)
                // Inside the shape: growing and shrinking clips to the card's box, which would cut
                // the 3D shadow around the round corners into hard ones.
                .then(if (animateSize) Modifier.animateContentSize() else Modifier)
                .background(colors.background, alpha = colors.backgroundAlpha)
                .then(if (colors.showBorder) Modifier.border(colors.borderWidth, colors.borderColor, shape) else Modifier)
                .padding(12.dp)
        ) { content() }
    }
}

/** The head of a category card: symbol, name, count, and the button that opens or closes it. */
@Composable
private fun CategoryCardHead(resolved: ResolvedCategory, expandable: Boolean, expanded: Boolean, onToggle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            CategoryNamePill(resolved.category, MaterialTheme.typography.titleMedium)
        }
        Text(
            resolved.members.size.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = LocalContentColor.current.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 12.dp, end = if (expandable) 0.dp else 8.dp)
        )
        if (expandable) {
            TextButton(onClick = onToggle) {
                Text(stringResource(if (expanded) R.string.categories_show_less else R.string.categories_show_all), color = LocalContentColor.current)
            }
        }
    }
}

/**
 * The Categories page: all categories as chips on top (several can be chosen, their contents show
 * together), below them every category as a card, as tabs or as a list, and a search field that
 * searches what is shown.
 */
@Composable
fun HomeCategoriesPage(
    viewModel: HomeViewModel,
    hideStatusBar: Boolean,
    surfaceDark: Boolean,
    iconSizeScale: Float,
    onManage: () -> Unit
) {
    val resolved by viewModel.resolvedCategories.collectAsState()
    val rawFilter by viewModel.categoryFilter.collectAsState()
    // A category deleted meanwhile no longer counts as chosen.
    val filter = remember(rawFilter, resolved) { rawFilter.filter { id -> resolved.any { it.category.id == id } }.toSet() }
    val view by viewModel.categoriesView.collectAsState()
    val showSearch by viewModel.categoriesShowSearch.collectAsState()
    val cardRows by viewModel.categoriesCardRows.collectAsState()
    val boxLook by viewModel.categoryBoxLook.collectAsState()
    val (look, corner) = boxLook
    val fieldCorner by viewModel.searchFieldCornerRadiusPercent.collectAsState()
    val labeler = rememberMemberLabeler(viewModel)
    var query by remember { mutableStateOf("") }
    val labelColor = resolveElementColors(look).contentColor
    val pageInk = if (surfaceDark) Color.White else Color.Black
    // Leaving for an app closes the page, like the app grid does after a start.
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_STOP) { viewModel.closeCategories() }

    fun matches(member: CategoryMember): Boolean = query.isBlank() || labeler(member).contains(query.trim(), ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (hideStatusBar) Modifier.windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top)) else Modifier.statusBarsPadding())
            // Above the keyboard while searching, above the navigation bar otherwise.
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = com.bisc.flauncher.ui.MaxContentWidth)) {
            if (resolved.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.categories_page_empty), color = pageInk, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    androidx.compose.material3.FilledTonalButton(onClick = onManage) { Text(stringResource(R.string.categories_page_make)) }
                }
                return@Column
            }
            if (showSearch) {
                com.bisc.flauncher.ui.home.search.AppSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    onSubmit = {},
                    appearance = look,
                    cornerRadiusPercent = fieldCorner ?: corner,
                    autoOpen = false,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 10.dp)
                )
            }
            // Tabs: a single chosen category (opened from an element, a gesture or the search) is its tab.
            val tabs = view == "TABS" && filter.size <= 1
            val pagerState = rememberPagerState(pageCount = { resolved.size })
            LaunchedEffect(tabs, filter) {
                val index = filter.singleOrNull()?.let { id -> resolved.indexOfFirst { it.category.id == id } } ?: -1
                if (tabs && index >= 0) pagerState.scrollToPage(index)
            }
            val scope = rememberCoroutineScope()
            val chipsState = rememberLazyListState()
            // The chips: "All" and every category. In tabs they pick the tab; otherwise each tap
            // adds or takes away a category from what is shown.
            LazyRow(
                state = chipsState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // In tabs every chip is a tab; "All" only makes sense where several show at once.
                if (!tabs) item(key = "all") {
                    CategoryChip(
                        category = null,
                        label = stringResource(R.string.categories_all),
                        count = null,
                        selected = filter.isEmpty() && !tabs,
                        look = look,
                        onClick = { viewModel.toggleCategoryFilter(LauncherCategories.ALL) }
                    )
                }
                items(resolved, key = { it.category.id }) { r ->
                    val index = resolved.indexOf(r)
                    CategoryChip(
                        category = r.category,
                        label = r.category.name,
                        count = r.members.size,
                        selected = if (tabs) pagerState.currentPage == index else r.category.id in filter,
                        look = look,
                        onClick = {
                            if (tabs) scope.launch { pagerState.animateScrollToPage(index) }
                            else viewModel.toggleCategoryFilter(r.category.id)
                        }
                    )
                }
            }
            if (tabs) {
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.currentPage }.collect { page -> chipsState.animateScrollToItem(page.coerceIn(0, (resolved.size - 1).coerceAtLeast(0))) }
                }
            }
            Spacer(Modifier.height(12.dp))
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val columns = categoryColumns(maxWidth - 56.dp)
                val chosen = if (tabs) emptyList() else resolved.filter { it.category.id in filter }
                when {
                    chosen.isNotEmpty() -> {
                        val members = unionMembers(chosen).filter(::matches)
                        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
                            item {
                                CategoryCard(look, corner) {
                                    if (members.isEmpty()) {
                                        Text(stringResource(R.string.categories_nothing), modifier = Modifier.padding(12.dp), color = labelColor)
                                    } else {
                                        CategoryMemberGrid(members, columns, labelColor, iconSizeScale, viewModel = viewModel)
                                    }
                                }
                            }
                        }
                    }
                    tabs -> HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        val r = resolved[page]
                        val members = r.members.filter(::matches)
                        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
                            item {
                                CategoryCard(look, corner) {
                                    CategoryCardHead(r, expandable = false, expanded = true, onToggle = {})
                                    if (members.isEmpty()) {
                                        Text(stringResource(R.string.categories_nothing), modifier = Modifier.padding(12.dp), color = labelColor)
                                    } else {
                                        CategoryMemberGrid(members, columns, labelColor, iconSizeScale, viewModel = viewModel)
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        val list = view == "LIST"
                        val searching = query.isNotBlank()
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (searching && resolved.none { r -> r.members.any(::matches) }) {
                                item { Text(stringResource(R.string.categories_nothing), color = pageInk, modifier = Modifier.padding(12.dp)) }
                            }
                            items(resolved, key = { it.category.id }) { r ->
                                val members = r.members.filter(::matches)
                                if (searching && members.isEmpty()) return@items
                                var expanded by remember(r.category.id) { mutableStateOf(false) }
                                val limit = columns * cardRows
                                val open = list || searching || expanded
                                CategoryCard(look, corner, animateSize = true) {
                                    CategoryCardHead(r, expandable = !list && !searching && members.size > limit, expanded = expanded, onToggle = { expanded = !expanded })
                                    if (members.isEmpty()) {
                                        Text(stringResource(R.string.categories_nothing), modifier = Modifier.padding(12.dp), color = labelColor)
                                    } else {
                                        CategoryMemberGrid(if (open) members else members.take(limit), columns, labelColor, iconSizeScale, viewModel = viewModel)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The Categories page as an overlay over Home, opening and closing like the App grid. */
@Composable
fun HomeCategoriesLayer(
    viewModel: HomeViewModel,
    visible: Boolean,
    hideStatusBar: Boolean,
    background: com.bisc.flauncher.data.model.BackgroundConfig,
    motion: com.bisc.flauncher.ui.MotionLevel,
    iconSizeScale: Float,
    onManage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val origin = com.bisc.flauncher.ui.rememberOpenOrigin(visible)
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxSize(),
        enter = com.bisc.flauncher.ui.zoomIn(motion, origin),
        exit = com.bisc.flauncher.ui.zoomOut(motion, origin)
    ) {
        val surfaceDark = com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(ElementAppearance(fillMode = com.bisc.flauncher.data.model.ElementFillMode.NONE), background)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {}
        ) {
            com.bisc.flauncher.ui.home.dock.BackgroundFill(background, Modifier.fillMaxSize())
            HomeCategoriesPage(viewModel, hideStatusBar, surfaceDark, iconSizeScale, onManage)
        }
    }
}

/** The apps of one category, out of [apps] (the App list's own), in their order there. */
internal fun categoryApps(categoryId: Long?, apps: List<com.bisc.flauncher.data.model.AppInfo>): List<com.bisc.flauncher.data.model.AppInfo> {
    val members = LauncherCategories.find(categoryId)?.members.orEmpty()
        .filter { it.type == GridItemType.APP }.map { it.payload }.toSet()
    return apps.filter { it.componentName.flattenToString() in members }
}

/** Category chips over the App list: one picked shows only its apps, "All" shows them all again. */
@Composable
internal fun AppListCategoryChips(selected: Long?, onSelect: (Long?) -> Unit, surfaceDark: Boolean, modifier: Modifier = Modifier) {
    val look = ElementAppearance(
        fillMode = com.bisc.flauncher.data.model.ElementFillMode.NONE,
        borderEnabled = true,
        contentColor = if (surfaceDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
    )
    val withApps = LauncherCategories.resolved.filter { r -> r.members.any { it.type == GridItemType.APP } }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "all") {
            CategoryChip(null, stringResource(R.string.categories_all), null, selected == null, look, onClick = { onSelect(null) })
        }
        items(withApps, key = { it.category.id }) { r ->
            CategoryChip(
                r.category, r.category.name, r.members.count { it.type == GridItemType.APP }, selected == r.category.id, look,
                onClick = { onSelect(if (selected == r.category.id) null else r.category.id) }
            )
        }
    }
}
