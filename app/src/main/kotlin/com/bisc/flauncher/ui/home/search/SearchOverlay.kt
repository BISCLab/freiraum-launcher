package com.bisc.flauncher.ui.home.search

import com.bisc.flauncher.util.matchesWordStart
import android.content.ComponentName
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.AppInfo
import com.bisc.flauncher.data.model.ContactInfo
import com.bisc.flauncher.data.model.PinnedShortcut
import com.bisc.flauncher.data.model.PinnedWebsite
import com.bisc.flauncher.data.model.SettingsShortcut
import com.bisc.flauncher.data.model.SettingsShortcuts
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.SearchCategory
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.home.dock.BackgroundFill
import com.bisc.flauncher.data.model.BackgroundConfig

/**
 * Full-screen search, decoupled from the dock — reachable from a dock action button, a gesture, or
 * (later) a standalone icon, and fully optional/ignorable if none of those is configured. The
 * whole screen shares one flat [appearance]: result cards float on it with a small gap between
 * them (see [SearchResultsColumn]). The search field + category filter is one combined element
 * ([SearchInputBar]) pinned flush to the top, the same "permanently-shown Panel" treatment the
 * home screen's own Panel uses; results fill the space below it, with the real system keyboard
 * (no custom in-app keyboard) appearing at the very bottom when focused.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchOverlay(
    query: String,
    appResults: List<AppInfo>,
    calculatorResult: String?,
    converterResult: String?,
    contactResults: List<ContactInfo>,
    websiteResults: List<PinnedWebsite>,
    shortcutResults: List<PinnedShortcut>,
    folderResults: List<com.bisc.flauncher.data.model.Folder> = emptyList(),
    mapTargetResults: List<com.bisc.flauncher.data.model.SavedMapTarget> = emptyList(),
    contactsPermissionGranted: Boolean,
    appearance: ElementAppearance,
    cornerRadiusPercent: Int,
    /** Backdrop under [appearance]'s fill; null = none (whatever is behind shows through). */
    background: BackgroundConfig? = null,
    /** The input bar's own look; null = same as [appearance] / [cornerRadiusPercent]. */
    barAppearance: ElementAppearance? = null,
    barCornerRadiusPercent: Int? = null,
    /** The result cards' own look; null = same as [appearance], fully rounded. */
    resultsAppearance: ElementAppearance? = null,
    resultsCornerRadiusPercent: Int? = null,
    resultsEdgeToEdge: Boolean = false,
    /** Keyboard extension strip color; null = off. While shown, it is the keyboard's top edge for
     *  the results above it. */
    keyboardCapColor: Int? = null,
    keyboardCapCornerRadiusPercent: Int = 100,
    statusBarHidden: Boolean,
    categoryOrder: List<SearchCategory>,
    activeCategories: Set<SearchCategory>,
    categoryFilter: SearchCategory?,
    categoryColors: Map<SearchCategory, Int?>,
    aiAppPackage: String?,
    onToggleFilter: (SearchCategory) -> Unit,
    keyboardAutoOpen: Boolean,
    loadIcon: (ComponentName) -> ImageBitmap?,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onContactClick: (ContactInfo) -> Unit,
    onSettingsShortcutClick: (SettingsShortcut) -> Unit,
    onFolderClick: (com.bisc.flauncher.data.model.Folder) -> Unit = {},
    onMapTargetClick: (com.bisc.flauncher.data.model.SavedMapTarget) -> Unit = {},
    onWebsiteClick: (PinnedWebsite) -> Unit,
    onShortcutClick: (PinnedShortcut) -> Unit,
    onRefreshContactsPermission: () -> Unit,
    onHome: () -> Unit,
    onWebCommand: () -> Unit,
    onMapsCommand: () -> Unit,
    onEmailCommand: () -> Unit,
    onAiCommand: () -> Unit
) {
    val context = LocalContext.current
    val aiAppLabel = remember(aiAppPackage) {
        aiAppPackage?.let { pkg ->
            runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString() }.getOrNull()
        }
    }

    LaunchedEffect(Unit) { onRefreshContactsPermission() }

    val settingsResults = remember(query, activeCategories) {
        if (query.isBlank() || SearchCategory.SETTINGS !in activeCategories) {
            emptyList()
        } else {
            val normalizedQuery = query.filter { it.isLetterOrDigit() }
            SettingsShortcuts.all.filter {
                val label = context.getString(it.labelRes)
                matchesWordStart(label, query) ||
                    label.filter { c -> c.isLetterOrDigit() }.startsWith(normalizedQuery, ignoreCase = true)
            }
        }
    }

    // The page has no fill of its own over its background any more — [appearance] only contributes
    // the background's opacity and the text color, automatic = readable against that background.
    val noFill = appearance.copy(fillMode = com.bisc.flauncher.data.model.ElementFillMode.NONE)
    val colors = if (background != null) {
        resolveElementColors(noFill, rememberSurfaceIsDark(noFill, background))
    } else {
        resolveElementColors(noFill)
    }

    // The results take whatever height is left above the system keyboard; hiding it (back
    // gesture) leaves the search.
    val imeVisible = WindowInsets.isImeVisible
    var imeWasShown by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible) {
        if (imeVisible) imeWasShown = true else if (imeWasShown) onHome()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // The screen's own backdrop (global, own image or own color); below full opacity whatever
        // is behind the search shows through it.
        if (background != null) {
            BackgroundFill(background, Modifier.matchParentSize().graphicsLayer { alpha = appearance.opacity.coerceIn(0f, 1f) })
        }
        CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
            // The top inset goes *inside* the input bar (its background fills it up to the screen
            // edge, like the Panel), not around it. Even with the status bar logically hidden, real
            // hardware still has a physical camera cutout/notch.
            val topInset = if (statusBarHidden) {
                WindowInsets.displayCutout.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding() + 16.dp
            } else {
                WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            }
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                ) {
                    SearchInputBar(
                        query = query,
                        placeholder = stringResource(R.string.search_field_hint),
                        onQueryChange = onQueryChange,
                        onSubmit = { appResults.firstOrNull()?.let(onAppClick) ?: onWebCommand() },
                        onHome = onHome,
                        appearance = barAppearance ?: appearance,
                        autoOpen = keyboardAutoOpen,
                        categories = categoryOrder.filter { it != SearchCategory.AI || aiAppLabel != null },
                        categoryColors = categoryColors,
                        selectedCategory = categoryFilter,
                        onToggleCategory = onToggleFilter,
                        cornerRadiusPercent = barCornerRadiusPercent ?: cornerRadiusPercent,
                        topInset = topInset
                    )
                    SearchResultsColumn(
                        query = query,
                        calculatorResult = calculatorResult,
                        converterResult = converterResult,
                        appResults = appResults,
                        contactResults = contactResults,
                        settingsResults = settingsResults,
                        websiteResults = websiteResults,
                        shortcutResults = shortcutResults,
                        folderResults = folderResults,
                        mapTargetResults = mapTargetResults,
                        contactsPermissionGranted = contactsPermissionGranted,
                        order = categoryOrder,
                        active = activeCategories,
                        aiAppLabel = aiAppLabel,
                        cardAppearance = resultsAppearance ?: appearance,
                        categoryColors = categoryColors,
                        loadIcon = loadIcon,
                        onAppClick = onAppClick,
                        onContactClick = onContactClick,
                        onSettingsShortcutClick = onSettingsShortcutClick,
                        onFolderClick = onFolderClick,
                        onMapTargetClick = onMapTargetClick,
                        onWebsiteClick = onWebsiteClick,
                        onShortcutClick = onShortcutClick,
                        onWebCommand = onWebCommand,
                        onMapsCommand = onMapsCommand,
                        onEmailCommand = onEmailCommand,
                        onAiCommand = onAiCommand,
                        onCalculatorResultClick = { result -> onQueryChange(result) },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        cardCornerRadiusPercent = resultsCornerRadiusPercent,
                        edgeToEdge = resultsEdgeToEdge
                    )
                    // Sits directly on top of the keyboard (this Column is IME-padded), so the
                    // results' bottom edge and fade end at the strip instead of the keyboard itself.
                    if (keyboardCapColor != null && imeVisible) {
                        val capRadius = KEYBOARD_CAP_HEIGHT * (keyboardCapCornerRadiusPercent.coerceIn(0, 100) / 100f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(KEYBOARD_CAP_HEIGHT)
                                .clip(RoundedCornerShape(topStart = capRadius, topEnd = capRadius))
                                .background(Color(keyboardCapColor))
                        )
                    }
                }
            }
        }
    }
}

private val KEYBOARD_CAP_HEIGHT = 20.dp
