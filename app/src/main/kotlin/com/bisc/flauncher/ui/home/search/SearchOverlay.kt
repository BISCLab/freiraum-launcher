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
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.first
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
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
 * Full-screen search, reachable from a dock action button or a gesture. Only the search field sits
 * at the top (or on the keyboard); the categories run down a rail beside the results, like the App
 * list's A-Z rail, always above the keyboard. Tapping a category jumps to its results and greys out
 * the others until the list is scrolled; dragging along the rail runs through them. Favorites show
 * while nothing is typed.
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
    /** Roundness of the field when it has none of its own; null = [cornerRadiusPercent]. */
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
    /** The search bar at the bottom, on the keyboard, instead of at the top. */
    barAtBottom: Boolean = false,
    /** The category rail beside the results. */
    showCategoryRail: Boolean = true,
    /** Categories that are searched but have no place in the rail. */
    railHidden: Set<SearchCategory> = emptySet(),
    fieldAppearance: ElementAppearance? = null,
    fieldCornerRadiusPercent: Int? = null,
    statusBarHidden: Boolean,
    categoryOrder: List<SearchCategory>,
    activeCategories: Set<SearchCategory>,
    categoryFilter: SearchCategory?,
    categoryColors: Map<SearchCategory, Int?>,
    webChoices: List<CommandChoice>,
    aiChoices: List<CommandChoice>,
    keyboardAutoOpen: Boolean,
    /** What the open keyboard took from the results area last time, for the rail (px). */
    keyboardOverlapPx: Int = 0,
    railBubbleAppearance: ElementAppearance? = null,
    onKeyboardOverlap: (Int) -> Unit = {},
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
    onWebCommand: (String?) -> Unit,
    onMapsCommand: () -> Unit,
    onAiCommand: (String?) -> Unit,
    /** Narrows the search to one category, or back to all (null). */
    onSetFilter: (SearchCategory?) -> Unit = {},
    favoriteApps: List<AppInfo> = emptyList(),
    favoriteIds: Set<String> = emptySet(),
    onToggleFavorite: (AppInfo) -> Unit = {},
    railLeft: Boolean = false,
    /** The rail's look and roundness; null = the automatic A-Z rail look and the general roundness. */
    railAppearance: ElementAppearance? = null,
    railCornerRadiusPercent: Int? = null
) {
    val context = LocalContext.current

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
    // only closes the keyboard; the next one leaves the search (HomeScreen's own BackHandler for it).

    // Arrival as one motion with the keyboard: it starts when the keyboard starts to rise and runs on
    // the keyboard's own curve; field, cards and rail all fade in and come up together, the backdrop
    // a little ahead. Without animations everything is there at once.
    val motionOn = com.bisc.flauncher.ui.LocalMotion.current != com.bisc.flauncher.ui.MotionLevel.OFF &&
        com.bisc.flauncher.ui.LocalMotionPrefs.current.overlayZoom
    val appear = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(if (motionOn) 0f else 1f) }
    val imeTargetInsets = WindowInsets.imeAnimationTarget
    val arrivalDensity = LocalDensity.current
    LaunchedEffect(Unit) {
        if (!motionOn) return@LaunchedEffect
        if (keyboardAutoOpen) kotlinx.coroutines.withTimeoutOrNull(KEYBOARD_WAIT_MILLIS) {
            androidx.compose.runtime.snapshotFlow { imeTargetInsets.getBottom(arrivalDensity) > 0 }.first { it }
        }
        appear.animateTo(1f, androidx.compose.animation.core.tween(ARRIVAL_MILLIS, easing = androidx.compose.animation.core.LinearEasing))
    }
    val stage: (Float, Float) -> Float = { start, end -> stageProgress(appear.value, start, end) }
    val density = LocalDensity.current

    val imeOpen = WindowInsets.isImeVisible
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var focus by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<SearchCategory?>(null) }
    var cards by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Map<SearchCategory, Int>>(emptyMap()) }
    var pendingJump by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<SearchCategory?>(null) }
    // Typing on ends the focus on one category, and with it any narrowing to it.
    LaunchedEffect(query) {
        focus = null
        if (categoryFilter != null) onSetFilter(null)
    }
    val clearFocus = {
        focus = null
        if (categoryFilter != null) onSetFilter(null)
    }
    // Jumps as soon as the card is there - right away, or once a narrowed search has found it.
    LaunchedEffect(cards, pendingJump) {
        val target = pendingJump ?: return@LaunchedEffect
        cards[target]?.let { index ->
            pendingJump = null
            listState.animateScrollToItem(index)
        }
    }
    val shownFavorites = when {
        SearchCategory.FAVORITES !in activeCategories -> emptyList()
        // Searched by default (the switch in the category list) = shown as soon as the search opens.
        query.isBlank() -> favoriteApps
        focus == SearchCategory.FAVORITES -> favoriteApps.filter { it.label.contains(query.trim(), ignoreCase = true) }
        else -> emptyList()
    }
    val railCategories = categoryOrder.filter {
        it !in railHidden && (it != SearchCategory.AI || aiChoices.isNotEmpty()) && (it != SearchCategory.FAVORITES || favoriteApps.isNotEmpty())
    }
    val onRailTap: (SearchCategory) -> Unit = { category ->
        if (focus == category) {
            clearFocus()
        } else if (query.isBlank() && category != SearchCategory.FAVORITES) {
            // Nothing typed yet: there is nothing to jump to.
        } else {
            focus = category
            // Not among the results right now (not searched by default, or narrowed to another):
            // search only this one, like the old filter.
            if (category !in cards || (categoryFilter != null && categoryFilter != category)) onSetFilter(category)
            pendingJump = category
        }
    }
    val onRailScrub: (SearchCategory) -> Unit = { category ->
        cards[category]?.let { index -> scope.launch { listState.scrollToItem(index) } }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // The screen's own backdrop (global, own image or own color), always fully opaque so the
        // home page and its dock never show through - same as the app list.
        if (background != null) {
            BackgroundFill(background, Modifier.matchParentSize().graphicsLayer { alpha = stage(0f, 0.45f) })
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
            val imeInsets = WindowInsets.ime
            Box(modifier = Modifier.fillMaxSize()) {
                @Composable
                fun Bar() {
                    SearchInputBar(
                        modifier = Modifier.graphicsLayer {
                            val p = stage(0f, 1f)
                            alpha = p
                            translationY = (1f - p) * with(density) { ARRIVAL_RISE.toPx() }
                        },
                        query = query,
                        placeholder = stringResource(R.string.search_field_hint),
                        onQueryChange = onQueryChange,
                        onSubmit = { appResults.firstOrNull()?.let(onAppClick) ?: onWebCommand(null) },
                        autoOpen = keyboardAutoOpen,
                        cornerRadiusPercent = barCornerRadiusPercent ?: 100,
                        topInset = if (barAtBottom) 0.dp else topInset,
                        // Flush with the cards on one side and the rail on the other.
                        sideMargin = com.bisc.flauncher.ui.home.dock.DockDimens.horizontalMargin,
                        atBottom = barAtBottom,
                        fieldAppearance = fieldAppearance,
                        fieldCornerRadiusPercent = fieldCornerRadiusPercent,
                        // At the bottom it keeps clear of the navigation bar; with the keyboard open
                        // it sits right on the keyboard.
                        bottomInsets = if (barAtBottom) WindowInsets.navigationBars.exclude(WindowInsets.ime) else null
                    )
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = com.bisc.flauncher.ui.MaxContentWidth)
                        .fillMaxSize()
                        // With the bar on top the area does not shrink with the keyboard: the results and the rail
                        // end where it will end (see KeyboardRoom), so nothing moves while it rises.
                        .windowInsetsPadding(if (barAtBottom) WindowInsets.ime else WindowInsets.navigationBars)
                ) {
                    if (!barAtBottom) Bar()
                    // Results with the category rail beside them; this area ends above the keyboard, so
                    // the rail never runs under it.
                    val showRail = showCategoryRail && railCategories.size > 1
                    androidx.compose.foundation.layout.BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .then(if (barAtBottom) Modifier.padding(top = topInset) else Modifier)
                    ) {
                    val room = if (barAtBottom) rememberKeyboardRoom(constraints.maxHeight.toFloat(), keyboardAutoOpen, keyboardOverlapPx, onKeyboardOverlap)
                    else rememberKeyboardRoomOverArea(
                        constraints.maxHeight.toFloat(), keyboardAutoOpen, keyboardOverlapPx, onKeyboardOverlap,
                        capPx = if (keyboardCapColor != null) with(density) { KEYBOARD_CAP_HEIGHT.roundToPx() } else 0,
                        rise = ::keyboardCapRise
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
                        webChoices = webChoices,
                        aiChoices = aiChoices,
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                bottom = if (barAtBottom) 0.dp else with(density) { (room.restPx - room.bottomPx).coerceAtLeast(0f).toDp() },
                                start = if (showRail && railLeft) SEARCH_RAIL_WIDTH + SEARCH_RAIL_GAP else 0.dp,
                                end = if (showRail && !railLeft) SEARCH_RAIL_WIDTH + SEARCH_RAIL_GAP else 0.dp
                            ),
                        cardCornerRadiusPercent = resultsCornerRadiusPercent,
                        edgeToEdge = resultsEdgeToEdge,
                        listState = listState,
                        favoriteApps = shownFavorites,
                        favoriteIds = favoriteIds,
                        onToggleFavorite = onToggleFavorite,
                        focus = focus,
                        onClearFocus = clearFocus,
                        onCardsChange = { cards = it },
                        arrival = { _ -> stage(0f, 1f) }
                    )
                    if (showRail) {
                        SearchCategoryRail(
                            categories = railCategories,
                            categoryColors = categoryColors,
                            present = cards.keys,
                            focus = focus,
                            left = railLeft,
                            // Its own look, or like the App list's A-Z rail; never tied to the field.
                            appearance = railAppearance ?: automaticRailAppearance(colors.contentColor.luminance() > 0.5f),
                            cornerRadiusPercent = railCornerRadiusPercent ?: cornerRadiusPercent,
                            onTap = onRailTap,
                            onScrub = onRailScrub,
                            // Same gap to the field as to the keyboard, top level with the first card.
                            edgeGap = SEARCH_CARD_GAP,
                            room = room,
                            // Until set on its own, like the result cards.
                            bubbleAppearance = railBubbleAppearance ?: resultsAppearance ?: defaultElement,
                            modifier = Modifier
                                .align(if (railLeft) Alignment.CenterStart else Alignment.CenterEnd)
                                .padding(horizontal = com.bisc.flauncher.ui.home.dock.DockDimens.horizontalMargin)
                                .graphicsLayer {
                                    val p = stage(0f, 1f)
                                    alpha = p
                                    translationY = (1f - p) * with(density) { ARRIVAL_RISE.toPx() }
                                }
                        )
                    }
                    }
                    // With the bar at the top, the strip sits directly on top of the keyboard (this
                    // Column is IME-padded), so the results' bottom edge and fade end at the strip.
                    // It fades out as soon as the keyboard *starts* closing (the animation target,
                    // not the current inset), riding down with it, so both leave in one motion.
                    // With the bar at the bottom it tops the bar instead.
                    if (barAtBottom) Bar()
                    if (keyboardCapColor != null) {
                        // With the bar on top, the strip is drawn over everything (below).
                        if (barAtBottom) KeyboardCap(keyboardCapColor, keyboardCapCornerRadiusPercent, keyboardCapArrow)
                    }
                }
                if (keyboardCapColor != null && !barAtBottom) {
                    // The strip is the keyboard's top edge: it rides up and down with it frame by
                    // frame, sliding out from the screen edge as the keyboard starts to rise. Its
                    // color reaches on down under the keyboard, so should the keyboard ever be a
                    // frame ahead, no gap opens between the two.
                    val capColor = Color(keyboardCapColor)
                    KeyboardCapStrip(
                        color = keyboardCapColor,
                        cornerRadiusPercent = keyboardCapCornerRadiusPercent,
                        arrow = keyboardCapArrow,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset {
                                val cap = KEYBOARD_CAP_HEIGHT.roundToPx()
                                val ime = imeInsets.getBottom(this)
                                androidx.compose.ui.unit.IntOffset(0, cap - ime - keyboardCapRise(ime, cap))
                            }
                            .drawBehind {
                                val ime = imeInsets.getBottom(this)
                                if (ime > 0) drawRect(capColor, topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - 1f), size = androidx.compose.ui.geometry.Size(size.width, ime + size.height))
                            }
                    )
                }
            }
        }
    }
}

/** Like the keyboard's own rise, so the two arrive together. */
private const val ARRIVAL_MILLIS = 300
/** How long the arrival waits for an expected keyboard to start before it goes on its own. */
private const val KEYBOARD_WAIT_MILLIS = 220L
private val ARRIVAL_RISE = 20.dp

/** How far one part has arrived, 0 to 1, while the whole search goes from 0 to 1: it starts at
 *  [start] and is there at [end], easing out like the other motions. */
private fun stageProgress(overall: Float, start: Float, end: Float): Float {
    val t = ((overall - start) / (end - start)).coerceIn(0f, 1f)
    return KeyboardEasing.transform(t)
}

/** The colored strip on top of the keyboard (Text search settings), shown while the keyboard is up.
 *  Place it last in an IME-padded column. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun KeyboardCap(color: Int, cornerRadiusPercent: Int, arrow: Boolean) {
    if (!WindowInsets.isImeVisible) return
    val closing = WindowInsets.imeAnimationTarget.getBottom(LocalDensity.current) == 0
    val capAlpha by animateFloatAsState(if (closing) 0f else 1f, tween(150), label = "capAlpha")
    KeyboardCapStrip(color, cornerRadiusPercent, arrow, Modifier.graphicsLayer { alpha = capAlpha })
}

/** The strip itself, wherever it is placed. */
@Composable
internal fun KeyboardCapStrip(color: Int, cornerRadiusPercent: Int, arrow: Boolean, modifier: Modifier = Modifier) {
    val capRadius = KEYBOARD_CAP_HEIGHT * (cornerRadiusPercent.coerceIn(0, 100) / 100f)
    val keyboardController = LocalSoftwareKeyboardController.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(KEYBOARD_CAP_HEIGHT)
            .clip(RoundedCornerShape(topStart = capRadius, topEnd = capRadius))
            .background(Color(color))
            .then(if (arrow) Modifier.noRippleClickable { keyboardController?.hide() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (arrow) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.search_keyboard_close),
                tint = if (Color(color).luminance() > 0.5f) Color.Black.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

internal val KEYBOARD_CAP_HEIGHT = 20.dp

/** The curve Android moves the keyboard on. */
private val KeyboardEasing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** How much of the strip shows above a keyboard that is [imePx] high: it comes out over the first
 *  [capPx] of the keyboard's way, so at rest it is fully below the screen edge. */
internal fun keyboardCapRise(imePx: Int, capPx: Int): Int = imePx.coerceIn(0, capPx)

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
