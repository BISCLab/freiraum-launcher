package com.bisc.flauncher.ui.settings

import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Interests
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.ui.theme.noRippleClickable
import com.bisc.flauncher.util.DefaultLauncherUtil

/** Top-level settings list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsListScreen(navController: NavController, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val isDefault = DefaultLauncherUtil.isDefaultLauncher(context)
    val home: com.bisc.flauncher.ui.home.HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val designsViewModel: DesignsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val packsViewModel: WallpaperPacksViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val allPages by home.allPages.collectAsState()
    val designs by designsViewModel.designs.collectAsState()
    val packs by packsViewModel.packs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = settingsTopBarInsets(),
                // The first page: no arrow, the back gesture leads home.
                title = { Text(stringResource(R.string.settings_title)) },
                actions = { SettingsSearchAction(navController) }
            )
        },
        bottomBar = { SettingsBreadcrumbBar(navController) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.widthIn(max = com.bisc.flauncher.ui.MaxContentWidth).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AboutHeaderCard(onClick = { onNavigate("about") })
            if (com.bisc.flauncher.ui.isLargeScreen()) {
                SettingsPreferenceGroup {
                    SettingsPreference(
                        icon = null,
                        title = stringResource(R.string.settings_tablet_beta_title),
                        summary = stringResource(R.string.settings_tablet_beta_summary),
                        onClick = null,
                        trailing = null
                    )
                }
            }
            // With the sections on the left (wide tablets) the same list here would only repeat them.
            if (!settingsTwoPane()) {
                // Three levels, global to specific: Appearance (looks the same everywhere), Pages (each
                // page and utility page, plus gestures), Apps & elements (the library of apps, folders,
                // places, links and action buttons) - then System for everything about fitting into the phone.
                SettingsTileGrid(
                    title = stringResource(R.string.settings_group_basics),
                    tiles = listOf(
                        SettingsTile(Icons.Filled.Palette, stringResource(R.string.settings_design_title), stringResource(R.string.settings_card_design_short), area = com.bisc.flauncher.ui.theme.SettingsArea.APPEARANCE) { onNavigate("appearance") },
                        SettingsTile(Icons.Filled.ViewCarousel, stringResource(R.string.settings_section_pages), stringResource(R.string.settings_card_pages_short), area = com.bisc.flauncher.ui.theme.SettingsArea.PAGES) { onNavigate("pages") },
                        SettingsTile(Icons.Filled.Apps, stringResource(R.string.settings_section_elements), stringResource(R.string.settings_card_elements_short), area = com.bisc.flauncher.ui.theme.SettingsArea.ELEMENTS) { onNavigate("elements") },
                        SettingsTile(
                            Icons.Filled.PhoneAndroid,
                            stringResource(R.string.settings_system_title),
                            stringResource(if (isDefault) R.string.settings_card_system_short else R.string.settings_default_launcher_description),
                            area = com.bisc.flauncher.ui.theme.SettingsArea.SYSTEM
                        ) { onNavigate("system") }
                    )
                )
            }
            // What can be added to the launcher, each straight to its own page.
                SettingsTileGrid(
                    title = stringResource(R.string.settings_group_extensions),
                    columns = 3,
                    tiles = listOf(
                        SettingsTile(Icons.Filled.AutoAwesome, stringResource(R.string.designs_title),
                            badge = androidx.compose.ui.res.pluralStringResource(R.plurals.extensions_available, designs.size, designs.size), area = com.bisc.flauncher.ui.theme.SettingsArea.EXTENSIONS) { onNavigate("appearance/designs") },
                        SettingsTile(Icons.Filled.Wallpaper, stringResource(R.string.packs_title),
                            badge = androidx.compose.ui.res.pluralStringResource(R.plurals.extensions_available, packs.size, packs.size), area = com.bisc.flauncher.ui.theme.SettingsArea.EXTENSIONS) { onNavigate("wallpaper_packs") },
                        SettingsTile(androidx.compose.material.icons.Icons.Filled.Widgets, stringResource(R.string.extensions_widgets_title), area = com.bisc.flauncher.ui.theme.SettingsArea.EXTENSIONS) { onNavigate("extensions/widgets") }
                    )
                )
        }
        }
    }
}

/** The route of the settings search; reached from every settings page by its magnifier. */
const val SETTINGS_FIND_ROUTE = "settings/find"

/** The magnifier in a settings page's top bar: opens the settings search with its quick access. */
@Composable
fun SettingsSearchAction(navController: NavController) {
    IconButton(onClick = { navController.navigate(SETTINGS_FIND_ROUTE) { launchSingleTop = true } }) {
        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.settings_search_settings_hint))
    }
}

/** The settings search: the field on top, below it the matches, or while nothing is typed the
 *  quick access to the pages most likely wanted. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsFindScreen(navController: NavController, onBack: () -> Unit) {
    val home: com.bisc.flauncher.ui.home.HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val allPages by home.allPages.collectAsState()
    val homeId = allPages.firstOrNull { it.isHome }?.id
    var query by rememberSaveable { mutableStateOf("") }
    // The page the search was opened on, for what to offer first.
    val fromRoute = remember { navController.previousBackStackEntry?.destination?.route }
    val openTarget: (String) -> Unit = { route ->
        if (route == "settings") {
            navController.popBackStack("settings", inclusive = false)
        } else if (homeId != null || com.bisc.flauncher.data.model.LauncherSettingsTarget.HOME !in route) {
            navController.navigate(route.replace(com.bisc.flauncher.data.model.LauncherSettingsTarget.HOME, homeId.toString()))
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = settingsTopBarInsets(),
                title = { SettingsSearchField(query = query, onQueryChange = { query = it }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
                actions = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(androidx.compose.material.icons.Icons.Filled.Close, contentDescription = androidx.compose.ui.res.stringResource(com.bisc.flauncher.R.string.a11y_clear_input))
                        }
                    }
                }
            )
        },
        bottomBar = { SettingsBreadcrumbBar(navController) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            SettingsSearchResults(query = query, fromRoute = fromRoute, onOpen = openTarget)
        }
    }
}

/** The search field in the settings' top bar, focused as soon as it shows. */
@Composable
private fun SettingsSearchField(query: String, onQueryChange: (String) -> Unit) {
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(Unit) { focus.requestFocus() }
    androidx.compose.foundation.text.BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
        decorationBox = { field ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Box(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    if (query.isEmpty()) Text(stringResource(R.string.settings_search_settings_hint), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    field()
                }
            }
        }
    )
}

/** Matches of the settings search, found like in the text search: by name and by what is on a page. */
@Composable
private fun SettingsSearchResults(query: String, fromRoute: String?, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val results = remember(query) {
        if (query.isBlank()) emptyList()
        else com.bisc.flauncher.data.model.LauncherSettingsTargets.all.filter {
            com.bisc.flauncher.util.matchesLabelOrKeywords(context.getString(it.labelRes), it.keywords, query)
        }.sortedByDescending { com.bisc.flauncher.util.labelOrKeywordsScore(context.getString(it.labelRes), it.keywords, query) }
    }
    Column(
        modifier = Modifier.widthIn(max = com.bisc.flauncher.ui.MaxContentWidth).fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when {
            // Nothing typed yet: the pages most likely wanted, one tap away.
            query.isBlank() -> SettingsQuickAccessGrid(fromRoute = fromRoute, onOpen = onOpen)
            results.isEmpty() -> Text(
                stringResource(R.string.settings_search_settings_none, query),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            else -> SettingsPreferenceGroup {
                results.forEach { target ->
                    SettingsPreference(
                        icon = null,
                        leading = { AreaSymbol(settingsAreaIcon(target.route), com.bisc.flauncher.ui.theme.settingsAreaFor(target.route), 36.dp) },
                        title = stringResource(target.labelRes),
                        summary = settingsAreaLabel(target.route),
                        onClick = { onOpen(target.route) }
                    )
                }
            }
        }
    }
}

/** The area a settings route belongs to, shown under a search match. */
@Composable
private fun settingsAreaLabel(route: String): String? {
    val part = route.removePrefix("settings/").substringBefore('/')
    return when (part) {
        "appearance" -> if (route.startsWith("settings/appearance/designs")) stringResource(R.string.settings_group_extensions) else stringResource(R.string.settings_design_title)
        "wallpaper_packs", "extensions" -> stringResource(R.string.settings_group_extensions)
        "pages", "gestures" -> stringResource(R.string.settings_section_pages)
        "search" -> stringResource(R.string.settings_section_search_full)
        "app_overview" -> stringResource(R.string.welcome_screen_app_list)
        "app_grid" -> stringResource(R.string.settings_pages_app_grid)
        "folder", "elements", "apps" -> stringResource(R.string.settings_section_elements)
        "system", "permissions", "about" -> stringResource(R.string.settings_system_title)
        else -> null
    }
}

private fun settingsAreaIcon(route: String): ImageVector {
    val part = route.removePrefix("settings/").substringBefore('/')
    return when (part) {
        "appearance" -> if (route.startsWith("settings/appearance/designs")) Icons.Filled.AutoAwesome else Icons.Filled.Palette
        "wallpaper_packs" -> Icons.Filled.Wallpaper
        "extensions" -> Icons.Filled.Extension
        "pages" -> Icons.Filled.ViewCarousel
        "gestures" -> Icons.Filled.Gesture
        "search" -> Icons.Filled.Search
        "app_overview" -> androidx.compose.material.icons.Icons.AutoMirrored.Filled.List
        "app_grid" -> androidx.compose.material.icons.Icons.Filled.GridView
        "folder" -> Icons.Filled.Folder
        "elements", "apps" -> Icons.Filled.Apps
        else -> Icons.Filled.PhoneAndroid
    }
}

/** Rounded group of preference rows with an optional title and an optional explanation that covers
 *  the whole group. When [info] is given, a neutral-gray "?" button next to the title expands it
 *  inline - this is the shared spot for a group's explanatory text, so a group's rows generally
 *  shouldn't also carry their own [SettingsPreference.info] (see that param's doc). */
@Composable
fun SettingsPreferenceGroup(title: String? = null, info: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (title != null) {
            SettingsGroupLabel(title, info = info)
        }
        Column(
            modifier = Modifier.clip(SettingsCardShape),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content
        )
    }
}

/** Whether the "?" tip buttons next to settings show at all - the global "Tips" switch, off by default. */
val LocalShowSettingsTips = compositionLocalOf { false }

/** The small secondary-colored label [SettingsPreferenceGroup] renders above its own card, with an
 *  optional expandable [info] text next to it (hidden while tips are off, see [LocalShowSettingsTips]). */
@Composable
fun SettingsGroupLabel(title: String, info: String? = null) {
    @Suppress("NAME_SHADOWING") val info = info.takeIf { LocalShowSettingsTips.current }
    var infoOpen by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 4.dp)
            )
            if (info != null) {
                IconButton(onClick = { infoOpen = !infoOpen }, modifier = Modifier.size(24.dp).padding(start = 2.dp)) {
                    Icon(
                        Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = stringResource(R.string.settings_info_button),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        if (info != null) {
            AnimatedVisibility(visible = infoOpen) {
                Text(
                    info,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
        }
    }
}

/** One row of a [SettingsPreferenceGroup]. */
@Composable
fun SettingsPreference(
    icon: ImageVector?,
    title: String,
    summary: String? = null,
    /** Null = a row that only says or holds something (a note, a control at its end). */
    onClick: (() -> Unit)?,
    titleFontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    leading: (@Composable () -> Unit)? = icon?.let {
        {
            Icon(
                it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    },
    trailing: (@Composable () -> Unit)? = { ChevronIndicator() },
    /** Tip behind a "?" button - only shown while tips are on, see [LocalShowSettingsTips]. */
    info: String? = null,
    infoAction: Pair<String, () -> Unit>? = null,
    /** Extra icon button rendered just left of the info "?" button - for a row-specific action
     *  (e.g. opening a dedicated settings subpage) that doesn't fit [onClick] or [trailing]. */
    extra: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    val shownInfo = info.takeIf { LocalShowSettingsTips.current }
    var infoOpen by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Same height as a row with a Switch (48dp touch target + padding), so rows with and
                // without a trailing control line up as equals inside one group.
                .heightIn(min = 80.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .then(if (enabled && onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leading?.invoke()
            Column(modifier = Modifier.padding(start = if (leading != null) 16.dp else 0.dp).weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontFamily = titleFontFamily ?: MaterialTheme.typography.titleMedium.fontFamily))
                if (summary != null) {
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (extra != null) {
                extra()
            }
            if (shownInfo != null) {
                IconButton(onClick = { infoOpen = !infoOpen }, modifier = Modifier.padding(start = 4.dp).size(32.dp)) {
                    Icon(
                        Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = stringResource(R.string.settings_info_button),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (trailing != null) {
                Box(modifier = Modifier.padding(start = 12.dp)) {
                    trailing()
                }
            }
        }
        if (shownInfo != null) {
            AnimatedVisibility(visible = infoOpen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(shownInfo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (infoAction != null) {
                        TextButton(onClick = infoAction.second) { Text(infoAction.first) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChevronIndicator() {
    Icon(
        Icons.AutoMirrored.Filled.ArrowForwardIos,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Shared scaffold for every sub-screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsFixedScreen(title: String, navController: NavController, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = settingsTopBarInsets(),
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(com.bisc.flauncher.R.string.settings_back))
                    }
                }
            )
        },
        bottomBar = { SettingsBreadcrumbBar(navController) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = com.bisc.flauncher.ui.MaxContentWidth).padding(horizontal = 16.dp, vertical = 8.dp), content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSubScreen(
    title: String,
    navController: NavController,
    onBack: () -> Unit,
    /** Editor mode: changes are kept as a draft and only written by this, from a Save button pinned
     *  to the bottom. Null = a plain screen whose changes apply right away. */
    onSave: (() -> Unit)? = null,
    /** Whether the draft differs from what's saved - enables Save and makes leaving ask first. */
    dirty: Boolean = false,
    /** Stays on top while the content below scrolls, e.g. a live preview of what is edited. */
    header: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    // Every way out (back gesture, the arrow, a breadcrumb) goes through this: with unsaved changes
    // it asks Save / Discard / Keep editing first, otherwise it just leaves.
    var pendingLeave by remember { mutableStateOf<(() -> Unit)?>(null) }
    val guardLeave: (() -> Unit) -> Unit = { leave -> if (onSave != null && dirty) pendingLeave = leave else leave() }
    androidx.activity.compose.BackHandler(enabled = onSave != null && dirty) { pendingLeave = onBack }
    // The sections on the left (wide tablets) ask through this before they leave the page.
    val unsaved by androidx.compose.runtime.rememberUpdatedState(onSave != null && dirty)
    androidx.compose.runtime.DisposableEffect(Unit) {
        val guard: ((() -> Unit)) -> Unit = { leave -> if (unsaved) pendingLeave = leave else leave() }
        SettingsLeaveGuard.current = guard
        onDispose { if (SettingsLeaveGuard.current === guard) SettingsLeaveGuard.current = null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = settingsTopBarInsets(),
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { guardLeave(onBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(com.bisc.flauncher.R.string.settings_back))
                    }
                },
                actions = { if (onSave == null) SettingsSearchAction(navController) }
            )
        },
        bottomBar = {
            Column {
                if (onSave != null) {
                    SettingsSaveBar(enabled = dirty, onSave = { onSave(); onBack() })
                }
                SettingsBreadcrumbBar(navController, guardLeave)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(modifier = Modifier.widthIn(max = com.bisc.flauncher.ui.MaxContentWidth).fillMaxWidth()) {
                if (header != null) {
                    Box(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)) { header() }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    content = content
                )
            }
        }
    }

    pendingLeave?.let { leave ->
        UnsavedChangesDialog(
            onSave = { pendingLeave = null; onSave?.invoke(); leave() },
            onDiscard = { pendingLeave = null; leave() },
            onKeepEditing = { pendingLeave = null }
        )
    }
}

/** The editor's Save button, pinned above the breadcrumbs; only active once something changed. */
@Composable
internal fun SettingsSaveBar(enabled: Boolean, onSave: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        val barModifier = Modifier.align(Alignment.Center).widthIn(max = com.bisc.flauncher.ui.MaxContentWidth - 32.dp).fillMaxWidth().heightIn(min = 52.dp)
        if (enabled) {
            androidx.compose.material3.Button(onClick = onSave, shape = SettingsCardShape, modifier = barModifier) {
                Text(stringResource(R.string.settings_save))
            }
        } else {
            // Nothing changed: everything is already stored, not a button waiting for something.
            Row(
                modifier = barModifier
                    .clip(SettingsCardShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(androidx.compose.material.icons.Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                Text(
                    stringResource(R.string.settings_all_saved),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/** Asked when leaving an editor with unsaved changes. */
@Composable
internal fun UnsavedChangesDialog(onSave: () -> Unit, onDiscard: () -> Unit, onKeepEditing: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text(stringResource(R.string.settings_unsaved_title)) },
        text = { Text(stringResource(R.string.settings_unsaved_message)) },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.settings_save)) } },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscard) { Text(stringResource(R.string.settings_unsaved_discard)) }
                TextButton(onClick = onKeepEditing) { Text(stringResource(R.string.settings_unsaved_keep_editing)) }
            }
        }
    )
}

/** A working copy of [saved] for an editor screen: starts as (and resets to) the saved value
 *  whenever that changes, e.g. right after saving. */
@Composable
fun <T> rememberDraft(saved: T): androidx.compose.runtime.MutableState<T> = remember(saved) { mutableStateOf(saved) }
