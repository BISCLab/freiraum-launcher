package com.bisc.flauncher.ui.home.search

import com.bisc.flauncher.util.matchesWordStart
import android.content.ComponentName
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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

/**
 * Full-screen search, decoupled from the dock — reachable from a dock power button, a gesture, or
 * (later) a standalone icon, and fully optional/ignorable if none of those is configured. The
 * whole screen shares one flat [appearance]: result cards float on it with a small gap between
 * them (see [SearchResultsColumn]). The search field + category filter is one combined element
 * ([SearchInputBar]); below it sits the real system keyboard, edge-to-edge, with a small rounded
 * colored strip ([keyboardAppearance]) peeking out above it as the only "frame" we can give a
 * surface we don't control the shape of.
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
    keyboardAppearance: ElementAppearance,
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

    val colors = resolveElementColors(appearance)
    val keyboardColors = resolveElementColors(keyboardAppearance)

    // The results take whatever height is left above the system keyboard; hiding it (back
    // gesture) leaves the search.
    val imeVisible = WindowInsets.isImeVisible
    var imeWasShown by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible) {
        if (imeVisible) imeWasShown = true else if (imeWasShown) onHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background, alpha = colors.backgroundAlpha)
            .then(if (appearance.borderEnabled) Modifier.border(colors.borderWidth, colors.borderColor) else Modifier)
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    // Even with the status bar logically hidden, real hardware still has a physical camera cutout/notch.
                    .then(
                        if (statusBarHidden) {
                            Modifier
                                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
                                .padding(top = 16.dp)
                        } else {
                            Modifier.statusBarsPadding()
                        }
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                ) {
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
                        cardAppearance = appearance,
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
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                    SearchInputBar(
                        query = query,
                        placeholder = stringResource(R.string.search_field_hint),
                        onQueryChange = onQueryChange,
                        onSubmit = { appResults.firstOrNull()?.let(onAppClick) ?: onWebCommand() },
                        onHome = onHome,
                        appearance = appearance,
                        autoOpen = keyboardAutoOpen,
                        categories = categoryOrder.filter { it != SearchCategory.AI || aiAppLabel != null },
                        categoryColors = categoryColors,
                        selectedCategory = categoryFilter,
                        onToggleCategory = onToggleFilter,
                        bottomMargin = 8.dp
                    )
                }

                // The real IME surface is opaque and drawn above our content, so this box is only
                // actually visible as a short rounded-corner strip peeking above it (KEYBOARD_PEEK
                // taller than the live inset) — the only "frame" we can give a surface whose shape
                // we don't control. Deliberately edge-to-edge (no horizontal margin).
                val density = LocalDensity.current
                val imeHeightPx = WindowInsets.ime.getBottom(density)
                val imeHeightDp = with(density) { imeHeightPx.toDp() }
                if (imeHeightDp > 0.dp) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(imeHeightDp + KEYBOARD_PEEK)
                            .clip(RoundedCornerShape(topStart = SearchDimens.cornerRadius, topEnd = SearchDimens.cornerRadius))
                            .background(keyboardColors.solidBackground)
                    )
                }
            }
        }
    }
}

private val KEYBOARD_PEEK = 14.dp
