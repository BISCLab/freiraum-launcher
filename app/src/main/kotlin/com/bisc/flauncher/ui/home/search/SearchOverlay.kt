package com.bisc.flauncher.ui.home.search

import com.bisc.flauncher.util.matchesLabelOrKeywords
import android.content.ComponentName
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.ui.theme.noRippleClickable
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
 * Full-screen search, decoupled from the dock - reachable from a dock action button, a gesture, or
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
    shortcutResults: List<com.bisc.flauncher.data.model.ShortcutResult>,
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
    /** Shows a small arrow in the strip and makes tapping the strip close the keyboard. */
    keyboardCapArrow: Boolean = false,
    /** The thin lines above the search field and between it and the categories (search bar settings). */
    barTopLine: Boolean = true,
    barMiddleLine: Boolean = true,
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
    onLauncherSettingsClick: (com.bisc.flauncher.data.model.LauncherSettingsTarget) -> Unit,
    onFolderClick: (com.bisc.flauncher.data.model.Folder) -> Unit = {},
    onMapTargetClick: (com.bisc.flauncher.data.model.SavedMapTarget) -> Unit = {},
    onWebsiteClick: (PinnedWebsite) -> Unit,
    onShortcutClick: (PinnedShortcut) -> Unit,
    onRefreshContactsPermission: () -> Unit,
    onHome: () -> Unit,
    onWebCommand: () -> Unit,
    onMapsCommand: () -> Unit,
    onAiCommand: () -> Unit
) {
    val context = LocalContext.current
    val aiAppLabel = remember(aiAppPackage) {
        aiAppPackage?.let { pkg ->
            runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString() }.getOrNull()
        }
    }

    LaunchedEffect(Unit) { onRefreshContactsPermission() }

    // Only the system settings screens this phone can open - some aren't offered by every maker.
    val availableSettings = remember {
        SettingsShortcuts.all.filter { context.packageManager.resolveActivity(android.content.Intent(it.action), 0) != null }
    }
    val settingsResults = remember(query, activeCategories, availableSettings) {
        if (query.isBlank() || SearchCategory.SETTINGS !in activeCategories) emptyList()
        else availableSettings.filter { matchesLabelOrKeywords(context.getString(it.labelRes), it.keywords, query) }
    }
    val launcherSettingsResults = remember(query, activeCategories) {
        if (query.isBlank() || SearchCategory.LAUNCHER_SETTINGS !in activeCategories) emptyList()
        else com.bisc.flauncher.data.model.LauncherSettingsTargets.all.filter {
            matchesLabelOrKeywords(context.getString(it.labelRes), it.keywords, query)
        }
    }

    // The page has no fill of its own over its background - [appearance] only contributes the text
    // color, automatic = readable against that background.
    val noFill = appearance.copy(fillMode = com.bisc.flauncher.data.model.ElementFillMode.NONE)
    // Not yet customized, the bar and the cards look like every other element (Dock, Panel, …):
    // the theme's default box, not the page's own (fill-less) appearance.
    val defaultElement = com.bisc.flauncher.ui.theme.defaultBoxAppearanceFor(
        androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.5f
    )
    val colors = if (background != null) {
        resolveElementColors(noFill, rememberSurfaceIsDark(noFill, background))
    } else {
        resolveElementColors(noFill)
    }

    // The results take whatever height is left above the system keyboard. The first back gesture
    // only closes the keyboard (the system handles that itself), so the results get the whole
    // screen; the next one leaves the search (HomeScreen's own BackHandler for it).
    val imeVisible = WindowInsets.isImeVisible

    Box(modifier = Modifier.fillMaxSize()) {
        // The screen's own backdrop (global, own image or own color), always fully opaque so the
        // home page and its dock never show through - same as the app list.
        if (background != null) {
            BackgroundFill(background, Modifier.matchParentSize())
        }
        CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
            // The top inset goes *inside* the input bar (its background fills it up to the screen
            // edge, like the Panel), not around it. Even with the status bar logically hidden, real
            // hardware still has a physical camera cutout/notch.
            val cameraGap = rememberCameraGap()
            val topInset = if (statusBarHidden) {
                // As much space below the camera as there is above it, when the cutout says where it is.
                cameraGap ?: (WindowInsets.displayCutout.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding() + 16.dp)
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
                        appearance = barAppearance ?: defaultElement,
                        autoOpen = keyboardAutoOpen,
                        categories = categoryOrder.filter { it != SearchCategory.AI || aiAppLabel != null },
                        categoryColors = categoryColors,
                        selectedCategory = categoryFilter,
                        onToggleCategory = onToggleFilter,
                        cornerRadiusPercent = barCornerRadiusPercent ?: cornerRadiusPercent,
                        topInset = topInset,
                        showTopLine = barTopLine,
                        showMiddleLine = barMiddleLine
                    )
                    SearchResultsColumn(
                        query = query,
                        calculatorResult = calculatorResult,
                        converterResult = converterResult,
                        appResults = appResults,
                        contactResults = contactResults,
                        settingsResults = settingsResults,
                        launcherSettingsResults = launcherSettingsResults,
                        websiteResults = websiteResults,
                        shortcutResults = shortcutResults,
                        folderResults = folderResults,
                        mapTargetResults = mapTargetResults,
                        contactsPermissionGranted = contactsPermissionGranted,
                        order = categoryOrder,
                        active = activeCategories,
                        aiAppLabel = aiAppLabel,
                        cardAppearance = resultsAppearance ?: defaultElement,
                        categoryColors = categoryColors,
                        loadIcon = loadIcon,
                        onAppClick = onAppClick,
                        onContactClick = onContactClick,
                        onSettingsShortcutClick = onSettingsShortcutClick,
                        onLauncherSettingsClick = onLauncherSettingsClick,
                        onFolderClick = onFolderClick,
                        onMapTargetClick = onMapTargetClick,
                        onWebsiteClick = onWebsiteClick,
                        onShortcutClick = onShortcutClick,
                        onWebCommand = onWebCommand,
                        onMapsCommand = onMapsCommand,
                        onAiCommand = onAiCommand,
                        onCalculatorResultClick = { result -> onQueryChange(result) },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        cardCornerRadiusPercent = resultsCornerRadiusPercent,
                        edgeToEdge = resultsEdgeToEdge
                    )
                    // Sits directly on top of the keyboard (this Column is IME-padded), so the
                    // results' bottom edge and fade end at the strip instead of the keyboard itself.
                    // It fades out as soon as the keyboard *starts* closing (the animation target,
                    // not the current inset), riding down with it, so both leave in one motion.
                    if (keyboardCapColor != null && imeVisible) {
                        val closing = WindowInsets.imeAnimationTarget.getBottom(LocalDensity.current) == 0
                        val capAlpha by animateFloatAsState(if (closing) 0f else 1f, tween(150), label = "capAlpha")
                        val capRadius = KEYBOARD_CAP_HEIGHT * (keyboardCapCornerRadiusPercent.coerceIn(0, 100) / 100f)
                        val keyboardController = LocalSoftwareKeyboardController.current
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(KEYBOARD_CAP_HEIGHT)
                                .graphicsLayer { alpha = capAlpha }
                                .clip(RoundedCornerShape(topStart = capRadius, topEnd = capRadius))
                                .background(Color(keyboardCapColor))
                                .then(if (keyboardCapArrow) Modifier.noRippleClickable { keyboardController?.hide() } else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            if (keyboardCapArrow) {
                                Icon(
                                    Icons.Filled.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.search_keyboard_close),
                                    tint = if (Color(keyboardCapColor).luminance() > 0.5f) Color.Black.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val KEYBOARD_CAP_HEIGHT = 20.dp

/** The space the search bar keeps at the top so that below the front camera there is as much room
 *  as above it: the camera's distance from the top edge, twice, plus the camera itself. Null when the
 *  display reports no camera cutout at the top (or before Android 9). */
@Composable
private fun rememberCameraGap(): androidx.compose.ui.unit.Dp? {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) return null
    val view = androidx.compose.ui.platform.LocalView.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val cutout = view.rootWindowInsets?.displayCutout ?: return null
    val camera = cutout.boundingRects.filter { it.top < view.height / 4 && !it.isEmpty }.minByOrNull { it.top } ?: return null
    // A notch reaching the top edge has no gap above it to mirror - keep a small one below then.
    return with(density) { if (camera.top > 0) (camera.bottom + camera.top).toDp() else camera.bottom.toDp() + 8.dp }
}
