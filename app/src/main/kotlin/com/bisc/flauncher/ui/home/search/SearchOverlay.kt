package com.bisc.flauncher.ui.home.search

import com.bisc.flauncher.util.matchesWordStart
import android.content.ComponentName
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.AppInfo
import com.bisc.flauncher.data.model.ContactInfo
import com.bisc.flauncher.data.model.Note
import com.bisc.flauncher.data.model.PinnedShortcut
import com.bisc.flauncher.data.model.PinnedWebsite
import com.bisc.flauncher.data.model.SettingsShortcut
import com.bisc.flauncher.data.model.SettingsShortcuts
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.SearchCategory
import com.bisc.flauncher.data.model.SearchKeyboardType
import com.bisc.flauncher.data.settings.Handedness
import com.bisc.flauncher.data.settings.IconShape
import com.bisc.flauncher.data.model.BackgroundConfig
import com.bisc.flauncher.ui.home.dock.BackgroundFill
import com.bisc.flauncher.ui.home.dock.DockDimens

private val RESULTS_KEYBOARD_GAP = 12.dp

/** Full-screen search reached by double-tapping the dock, full-bleed top to bottom. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchOverlay(
    query: String,
    appResults: List<AppInfo>,
    calculatorResult: String?,
    contactResults: List<ContactInfo>,
    noteResults: List<Note>,
    websiteResults: List<PinnedWebsite>,
    shortcutResults: List<PinnedShortcut>,
    folderResults: List<com.bisc.flauncher.data.model.Folder> = emptyList(),
    mapTargetResults: List<com.bisc.flauncher.data.model.SavedMapTarget> = emptyList(),
    contactsPermissionGranted: Boolean,
    backdrop: BackgroundConfig,
    keyboardAppearance: ElementAppearance,
    searchResultCardAppearance: ElementAppearance,
    statusBarHidden: Boolean,
    categoryOrder: List<SearchCategory>,
    activeCategories: Set<SearchCategory>,
    categoryFilter: SearchCategory?,
    aiAppPackage: String?,
    onToggleFilter: (SearchCategory) -> Unit,
    keyboardType: SearchKeyboardType,
    showKeyboardNumberRow: Boolean,
    keyboardLayout: com.bisc.flauncher.data.model.KeyboardLayout = com.bisc.flauncher.data.model.KeyboardLayout.QWERTY,
    keyboardHaptics: Boolean = true,
    keyboardSparks: Boolean = true,
    keyboardSparkColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF34D058),
    handedness: Handedness,
    iconShape: IconShape,
    loadIcon: (ComponentName) -> ImageBitmap?,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onContactClick: (ContactInfo) -> Unit,
    onSettingsShortcutClick: (SettingsShortcut) -> Unit,
    onNoteClick: (Note) -> Unit,
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

    Box(modifier = Modifier.fillMaxSize()) {
        BackgroundFill(backdrop, Modifier.fillMaxSize())

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
            val innerWidth = (maxWidth - DockDimens.horizontalMargin * 2).coerceAtLeast(0.dp)
            val panelShape = RoundedCornerShape(com.bisc.flauncher.ui.home.dock.dockDimens(innerWidth).cornerRadius)

            var keyboardHeightPx by remember { mutableIntStateOf(0) }
            val density = LocalDensity.current
            val keyboardBottomInset = with(density) { keyboardHeightPx.toDp() } + RESULTS_KEYBOARD_GAP

            if (keyboardType == SearchKeyboardType.SYSTEM) {
                // The results take whatever height is left above the system keyboard; hiding it (back gesture) leaves the search.
                val imeVisible = WindowInsets.isImeVisible
                var imeWasShown by remember { mutableStateOf(false) }
                LaunchedEffect(imeVisible) {
                    if (imeVisible) imeWasShown = true else if (imeWasShown) onHome()
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                ) {
            SearchResultsColumn(
                query = query,
                appResults = appResults,
                calculatorResult = calculatorResult,
                contactResults = contactResults,
                settingsResults = settingsResults,
                noteResults = noteResults,
                websiteResults = websiteResults,
                shortcutResults = shortcutResults,
                folderResults = folderResults,
                mapTargetResults = mapTargetResults,
                contactsPermissionGranted = contactsPermissionGranted,
                order = categoryOrder,
                active = activeCategories,
                filter = categoryFilter,
                aiAppLabel = aiAppLabel,
                onToggleFilter = onToggleFilter,
                loadIcon = loadIcon,
                onAppClick = onAppClick,
                onContactClick = onContactClick,
                onSettingsShortcutClick = onSettingsShortcutClick,
                onNoteClick = onNoteClick,
                onFolderClick = onFolderClick,
                onMapTargetClick = onMapTargetClick,
                onWebsiteClick = onWebsiteClick,
                onShortcutClick = onShortcutClick,
                onWebCommand = onWebCommand,
                onMapsCommand = onMapsCommand,
                onEmailCommand = onEmailCommand,
                onAiCommand = onAiCommand,
                onCalculatorResultClick = { result -> onQueryChange(result) },
                cardAppearance = searchResultCardAppearance,
                shape = panelShape,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = DockDimens.horizontalMargin).padding(bottom = RESULTS_KEYBOARD_GAP)
            )
                    SystemSearchField(
                        query = query,
                        placeholder = stringResource(R.string.search_field_hint),
                        onQueryChange = onQueryChange,
                        onSubmit = { appResults.firstOrNull()?.let(onAppClick) ?: onWebCommand() },
                        onHome = onHome,
                        appearance = keyboardAppearance,
                        bottomMargin = if (imeVisible) 8.dp else DockDimens.bottomMargin
                    )
                }
                return@BoxWithConstraints
            }

            SearchResultsColumn(
                query = query,
                appResults = appResults,
                calculatorResult = calculatorResult,
                contactResults = contactResults,
                settingsResults = settingsResults,
                noteResults = noteResults,
                websiteResults = websiteResults,
                shortcutResults = shortcutResults,
                folderResults = folderResults,
                mapTargetResults = mapTargetResults,
                contactsPermissionGranted = contactsPermissionGranted,
                order = categoryOrder,
                active = activeCategories,
                filter = categoryFilter,
                aiAppLabel = aiAppLabel,
                onToggleFilter = onToggleFilter,
                loadIcon = loadIcon,
                onAppClick = onAppClick,
                onContactClick = onContactClick,
                onSettingsShortcutClick = onSettingsShortcutClick,
                onNoteClick = onNoteClick,
                onFolderClick = onFolderClick,
                onMapTargetClick = onMapTargetClick,
                onWebsiteClick = onWebsiteClick,
                onShortcutClick = onShortcutClick,
                onWebCommand = onWebCommand,
                onMapsCommand = onMapsCommand,
                onEmailCommand = onEmailCommand,
                onAiCommand = onAiCommand,
                onCalculatorResultClick = { result -> onQueryChange(result) },
                cardAppearance = searchResultCardAppearance,
                shape = panelShape,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = DockDimens.horizontalMargin)
                    .padding(bottom = keyboardBottomInset)
            )

            CustomKeyboard(
                query = query,
                placeholder = stringResource(R.string.search_field_hint),
                onKeyPress = { char -> onQueryChange(query + char) },
                onBackspace = { if (query.isNotEmpty()) onQueryChange(query.dropLast(1)) },
                onSpace = { onQueryChange("$query ") },
                onHome = onHome,
                showNumberRow = showKeyboardNumberRow,
                layout = keyboardLayout,
                hapticsEnabled = keyboardHaptics,
                sparksEnabled = keyboardSparks,
                sparkColor = keyboardSparkColor,
                handedness = handedness,
                iconShape = iconShape,
                appearance = keyboardAppearance,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onGloballyPositioned { coordinates -> keyboardHeightPx = coordinates.size.height }
            )
        }
    }
}
