package com.bisc.flauncher.ui.settings

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import com.bisc.flauncher.ui.stackElement
import android.content.ComponentName
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwipeDown
import androidx.compose.material.icons.filled.Pinch
import androidx.compose.material.icons.filled.SwipeUp
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.Timer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.AppInfo
import com.bisc.flauncher.data.model.DockVisibilityMode
import com.bisc.flauncher.data.model.GestureAction
import com.bisc.flauncher.data.model.GestureActionType
import com.bisc.flauncher.data.model.HomeGesture
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.ui.pageLabel
import com.bisc.flauncher.ui.home.dock.AppShortcutPickerDialog
import com.bisc.flauncher.ui.home.dock.PagePickerSheet
import com.bisc.flauncher.ui.home.dock.ShortcutSourceAppSheet

/**
 * The gestures as a card stack: the start page with every gesture where it happens, numbered;
 * ⓪ the start page, then one part per gesture with what it does. A gesture the Dock or the Panel
 * takes is shown faint with the reason. Everything is a draft until Save.
 */
@Composable
fun SettingsGesturesScreen(navController: NavController, onBack: () -> Unit, startElement: String? = null, viewModel: SettingsViewModel = hiltViewModel()) {
    val home: com.bisc.flauncher.ui.home.HomeViewModel = hiltViewModel()
    val stack: StackDraft = androidx.lifecycle.viewmodel.compose.viewModel()
    val saved by viewModel.gestureActions.collectAsState()
    val labeledApps by viewModel.labeledApps.collectAsState()
    val allPages by viewModel.allPages.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val homeDockRow by viewModel.homeDockRow.collectAsState()
    val homePanelRow by viewModel.homePanelRow.collectAsState()
    val shortcutAppPickerOpen by viewModel.shortcutAppPickerOpen.collectAsState()
    val shortcutPickerForApp by viewModel.shortcutPickerForApp.collectAsState()
    val availableShortcuts by viewModel.availableShortcuts.collectAsState()
    var pickingAppFor by remember { mutableStateOf<HomeGesture?>(null) }
    var pickingPageFor by remember { mutableStateOf<HomeGesture?>(null) }
    var pickingCategoryFor by remember { mutableStateOf<HomeGesture?>(null) }
    var pendingShortcutGesture by remember { mutableStateOf<HomeGesture?>(null) }

    val actions = HomeGesture.entries.associateWith { g ->
        stack.field("g_${g.name}", saved[g] ?: GestureAction.NONE) { viewModel.setGestureAction(g, it) }
    }
    val dockReason = stringResource(R.string.settings_gesture_locked_by_dock)
    val panelReason = stringResource(R.string.settings_gesture_locked_by_panel)
    fun locked(g: HomeGesture): String? = when {
        g == HomeGesture.SWIPE_UP && homeDockRow?.enabled == true && homeDockRow?.dockVisibilityMode == DockVisibilityMode.MANUAL -> dockReason
        g == HomeGesture.SWIPE_DOWN && homePanelRow?.enabled == true -> panelReason
        else -> null
    }
    fun pick(g: HomeGesture, type: GestureActionType) {
        when (type) {
            GestureActionType.APP -> pickingAppFor = g
            GestureActionType.SHORTCUT -> { pendingShortcutGesture = g; viewModel.openShortcutAppPicker() }
            GestureActionType.PAGE -> pickingPageFor = g
            GestureActionType.CATEGORY -> pickingCategoryFor = g
            else -> actions.getValue(g).value = GestureAction(type)
        }
    }
    val summaries = HomeGesture.entries.associateWith { g -> gestureActionSummary(actions.getValue(g).value, allPages, labeledApps) }

    val elements = buildList {
        add(StackElement(
            id = "page", number = 0,
            title = stringResource(R.string.settings_pages_home),
            summary = stringResource(R.string.gestures_page_summary)
        ) {
            PartIntro(stringResource(R.string.gestures_intro))
            SettingsPreferenceGroup {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_section_pages),
                    summary = stringResource(R.string.gestures_pages_link),
                    onClick = { navController.navigate("settings/pages") },
                    trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
                )
            }
        })
        HomeGesture.entries.forEachIndexed { i, g ->
            val reason = locked(g)
            val field = actions.getValue(g)
            add(StackElement(
                id = "g_${g.name}", number = i + 1,
                title = gestureLabel(g),
                summary = reason ?: summaries.getValue(g),
                shown = reason == null,
                draftKeys = listOf("g_${g.name}"),
                anchor = Offset(0.5f, 0f)
            ) {
                if (reason != null) PartIntro(reason)
                StackDimmed(reason != null) {
                    SettingsPreferenceGroup(title = stringResource(R.string.settings_gesture_action_title)) {
                        GestureActionType.entries.forEach { type ->
                            RadioPreference(gestureActionTypeLabel(type), type, field.value.type, onSelect = { if (reason == null) pick(g, it) })
                        }
                    }
                    val type = field.value.type
                    if (type == GestureActionType.APP || type == GestureActionType.PAGE || type == GestureActionType.CATEGORY || type == GestureActionType.SHORTCUT) {
                        SettingsPreferenceGroup {
                            SettingsPreference(
                                icon = null,
                                title = summaries.getValue(g),
                                summary = stringResource(R.string.gestures_change_target),
                                onClick = { if (reason == null) pick(g, type) },
                                trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
                            )
                        }
                    }
                }
            })
        }
    }

    val pages by home.pages.collectAsState()
    val homeId = pages.find { it.isHome }?.id ?: -1L
    val global by home.globalBackground.collectAsState()
    val background by remember(homeId) { home.pageBackgroundConfig(homeId) }.collectAsState(initial = global)
    val dark = com.bisc.flauncher.ui.home.search.rememberSurfaceIsDark(com.bisc.flauncher.data.model.ElementAppearance(fillMode = com.bisc.flauncher.data.model.ElementFillMode.NONE), background)
    androidx.compose.runtime.CompositionLocalProvider(com.bisc.flauncher.ui.home.LocalSurfaceIsDark provides dark) {
        SettingsCardStackScreen(
            title = stringResource(R.string.settings_section_gestures),
            navController = navController,
            onBack = onBack,
            draft = stack,
            elements = elements,
            startElement = startElement
        ) { _, _ ->
            val ink = if (dark) Color.White else Color.Black
            val statusTop = replicaStatusHeight()
            Box(Modifier.fillMaxSize()) {
                com.bisc.flauncher.ui.home.dock.BackgroundFill(background, Modifier.fillMaxSize())
                // Where each gesture happens: down at the top, taps and pinch in the middle, up at the bottom;
                // one finger on the left, two on the right.
                Column(
                    modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = statusTop + 24.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    @Composable
                    fun Zone(g: HomeGesture, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
                        GestureZone(g, icon, summaries.getValue(g), ink, modifier.stackElement("g_${g.name}"))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Zone(HomeGesture.SWIPE_DOWN, Icons.Filled.ArrowDownward, Modifier.weight(1f))
                        Zone(HomeGesture.TWO_FINGER_SWIPE_DOWN, Icons.Filled.SwipeDown, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Zone(HomeGesture.DOUBLE_TAP, Icons.Filled.TouchApp, Modifier.fillMaxWidth())
                            Zone(HomeGesture.LONG_PRESS, Icons.Filled.Timer, Modifier.fillMaxWidth())
                        }
                        Zone(HomeGesture.PINCH_IN, Icons.Filled.Pinch, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Zone(HomeGesture.SWIPE_UP, Icons.Filled.ArrowUpward, Modifier.weight(1f))
                        Zone(HomeGesture.TWO_FINGER_SWIPE_UP, Icons.Filled.SwipeUp, Modifier.weight(1f))
                    }
                }
                StatusBarSketch(statusTop, ink)
            }
        }
    }

    pickingCategoryFor?.let { g ->
        CategoryPickerSheet(
            categories = categories,
            onSelect = { category -> actions.getValue(g).value = GestureAction(GestureActionType.CATEGORY, category.id.toString()); pickingCategoryFor = null },
            onDismiss = { pickingCategoryFor = null },
            onCreate = { name ->
                viewModel.addCategory(name) { id -> actions.getValue(g).value = GestureAction(GestureActionType.CATEGORY, id.toString()) }
                pickingCategoryFor = null
            }
        )
    }
    pickingAppFor?.let { g ->
        ShortcutSourceAppSheet(
            apps = labeledApps,
            loadIcon = viewModel::loadIcon,
            onAppSelected = { app -> actions.getValue(g).value = GestureAction(GestureActionType.APP, app.componentName.flattenToString()); pickingAppFor = null },
            onDismiss = { pickingAppFor = null }
        )
    }
    pickingPageFor?.let { g ->
        PagePickerSheet(
            pages = allPages,
            onPageSelected = { page -> actions.getValue(g).value = GestureAction(GestureActionType.PAGE, page.id.toString()); pickingPageFor = null },
            onDismiss = { pickingPageFor = null }
        )
    }
    if (shortcutAppPickerOpen) {
        ShortcutSourceAppSheet(
            apps = com.bisc.flauncher.util.ProfileComponents.ownOnly(labeledApps),
            loadIcon = viewModel::loadIcon,
            onAppSelected = viewModel::selectShortcutSourceApp,
            onDismiss = viewModel::dismissShortcutAppPicker
        )
    }
    shortcutPickerForApp?.let { app ->
        AppShortcutPickerDialog(
            appLabel = app.label,
            shortcuts = availableShortcuts,
            loadIcon = viewModel::loadShortcutIcon,
            onShortcutSelected = { info ->
                pendingShortcutGesture?.let { g -> actions.getValue(g).value = GestureAction(GestureActionType.SHORTCUT, "${info.`package`}|${info.id}") }
                pendingShortcutGesture = null
                viewModel.dismissShortcutPicker()
            },
            onDismiss = { pendingShortcutGesture = null; viewModel.dismissShortcutPicker() }
        )
    }
}

/** One gesture on the start page replica: its symbol, its name and what it does. */
@Composable
private fun GestureZone(gesture: HomeGesture, icon: androidx.compose.ui.graphics.vector.ImageVector, summary: String, ink: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(ink.copy(alpha = 0.12f))
            .border(1.5.dp, ink.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(36.dp))
        Text(gestureLabel(gesture), color = ink, style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        Text(summary, color = ink.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/** The gestures phone with every marker tappable, and the pickers that set what a gesture does.
 *  Shared by the gestures settings and the welcome tour. */
@Composable
internal fun EditableGesturePhone(viewModel: SettingsViewModel, widthFraction: Float = 0.92f) {
    val gestureActions by viewModel.gestureActions.collectAsState()
    // Unfiltered - a gesture already bound to an app keeps showing its.
    val labeledApps by viewModel.labeledApps.collectAsState()
    val browsableApps by viewModel.labeledApps.collectAsState()
    val allPages by viewModel.allPages.collectAsState()
    val homeDockRow by viewModel.homeDockRow.collectAsState()
    val homePanelRow by viewModel.homePanelRow.collectAsState()
    // Mirrors the reservation HomeScreen's detectHomeGestures actually enforces - a gesture claimed
    // by the Dock/Panel can't also be rebound here, shown locked with a reason instead of hidden.
    val lockedReason: @Composable (HomeGesture) -> String? = { gesture ->
        when {
            gesture == HomeGesture.SWIPE_UP && homeDockRow?.enabled == true && homeDockRow?.dockVisibilityMode == DockVisibilityMode.MANUAL ->
                stringResource(R.string.settings_gesture_locked_by_dock)
            gesture == HomeGesture.SWIPE_DOWN && homePanelRow?.enabled == true ->
                stringResource(R.string.settings_gesture_locked_by_panel)
            else -> null
        }
    }
    val shortcutAppPickerOpen by viewModel.shortcutAppPickerOpen.collectAsState()
    val shortcutPickerForApp by viewModel.shortcutPickerForApp.collectAsState()
    val availableShortcuts by viewModel.availableShortcuts.collectAsState()

    var editingGesture by remember { mutableStateOf<HomeGesture?>(null) }
    var pickingAppFor by remember { mutableStateOf<HomeGesture?>(null) }
    var pickingPageFor by remember { mutableStateOf<HomeGesture?>(null) }
    var pickingCategoryFor by remember { mutableStateOf<HomeGesture?>(null) }
    var pendingShortcutGesture by remember { mutableStateOf<HomeGesture?>(null) }

    // A phone with each gesture marked where it happens, instead of a list - tap a marker to change
    // what it does.
    val markers = HomeGesture.entries.associateWith { gesture ->
        val locked = lockedReason(gesture)
        GestureMarker(
            summary = locked ?: gestureActionSummary(gestureActions[gesture] ?: GestureAction.NONE, allPages, labeledApps),
            enabled = locked == null
        )
    }
    GesturePhone(markers = markers, onEdit = { editingGesture = it }, widthFraction = widthFraction)

    editingGesture?.let { gesture ->
        GestureActionTypeSheet(
            current = (gestureActions[gesture] ?: GestureAction.NONE).type,
            onSelected = { type ->
                editingGesture = null
                when (type) {
                    GestureActionType.APP -> pickingAppFor = gesture
                    GestureActionType.SHORTCUT -> {
                        pendingShortcutGesture = gesture
                        viewModel.openShortcutAppPicker()
                    }
                    GestureActionType.PAGE -> pickingPageFor = gesture
                    GestureActionType.CATEGORY -> pickingCategoryFor = gesture
                    else -> viewModel.setGestureAction(gesture, GestureAction(type))
                }
            },
            onDismiss = { editingGesture = null }
        )
    }

    pickingCategoryFor?.let { gesture ->
        val categories by viewModel.categories.collectAsState()
        CategoryPickerSheet(
            categories = categories,
            onSelect = { category ->
                viewModel.setGestureAction(gesture, GestureAction(GestureActionType.CATEGORY, category.id.toString()))
                pickingCategoryFor = null
            },
            onDismiss = { pickingCategoryFor = null },
            onCreate = { name ->
                viewModel.addCategory(name) { id -> viewModel.setGestureAction(gesture, GestureAction(GestureActionType.CATEGORY, id.toString())) }
                pickingCategoryFor = null
            }
        )
    }

    pickingAppFor?.let { gesture ->
        ShortcutSourceAppSheet(
            apps = browsableApps,
            loadIcon = viewModel::loadIcon,
            onAppSelected = { app ->
                viewModel.setGestureAction(gesture, GestureAction(GestureActionType.APP, app.componentName.flattenToString()))
                pickingAppFor = null
            },
            onDismiss = { pickingAppFor = null }
        )
    }

    pickingPageFor?.let { gesture ->
        PagePickerSheet(
            pages = allPages,
            onPageSelected = { page ->
                viewModel.setGestureAction(gesture, GestureAction(GestureActionType.PAGE, page.id.toString()))
                pickingPageFor = null
            },
            onDismiss = { pickingPageFor = null }
        )
    }

    if (shortcutAppPickerOpen) {
        ShortcutSourceAppSheet(
            apps = com.bisc.flauncher.util.ProfileComponents.ownOnly(browsableApps),
            loadIcon = viewModel::loadIcon,
            onAppSelected = viewModel::selectShortcutSourceApp,
            onDismiss = viewModel::dismissShortcutAppPicker
        )
    }

    shortcutPickerForApp?.let { app ->
        AppShortcutPickerDialog(
            appLabel = app.label,
            shortcuts = availableShortcuts,
            loadIcon = viewModel::loadShortcutIcon,
            onShortcutSelected = { info ->
                pendingShortcutGesture?.let { gesture ->
                    viewModel.setGestureAction(gesture, GestureAction(GestureActionType.SHORTCUT, "${info.`package`}|${info.id}"))
                }
                pendingShortcutGesture = null
                viewModel.dismissShortcutPicker()
            },
            onDismiss = viewModel::dismissShortcutPicker
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GestureActionTypeSheet(
    current: GestureActionType,
    types: List<GestureActionType> = GestureActionType.entries,
    onSelected: (GestureActionType) -> Unit,
    onDismiss: () -> Unit
) {
    ChoicePanel(
        title = stringResource(R.string.settings_gesture_action_title),
        options = types.map { type -> ChoiceOption(label = gestureActionTypeLabel(type), selected = type == current, onClick = { onSelected(type) }) },
        onDismiss = onDismiss
    )
}

internal fun gestureIcon(gesture: HomeGesture) = when (gesture) {
    HomeGesture.LONG_PRESS -> Icons.Filled.TouchApp
    HomeGesture.DOUBLE_TAP -> Icons.Filled.TouchApp
    HomeGesture.SWIPE_UP -> Icons.Filled.SwipeUp
    HomeGesture.SWIPE_DOWN -> Icons.Filled.SwipeDown
    HomeGesture.TWO_FINGER_SWIPE_UP -> Icons.Filled.SwipeUp
    HomeGesture.TWO_FINGER_SWIPE_DOWN -> Icons.Filled.SwipeDown
    HomeGesture.PINCH_IN -> Icons.Filled.Pinch
}

@Composable
internal fun gestureLabel(gesture: HomeGesture): String = when (gesture) {
    HomeGesture.LONG_PRESS -> stringResource(R.string.settings_gesture_long_press)
    HomeGesture.DOUBLE_TAP -> stringResource(R.string.settings_gesture_double_tap)
    HomeGesture.SWIPE_UP -> stringResource(R.string.settings_gesture_swipe_up)
    HomeGesture.SWIPE_DOWN -> stringResource(R.string.settings_gesture_swipe_down)
    HomeGesture.TWO_FINGER_SWIPE_UP -> stringResource(R.string.settings_gesture_two_up)
    HomeGesture.TWO_FINGER_SWIPE_DOWN -> stringResource(R.string.settings_gesture_two_down)
    HomeGesture.PINCH_IN -> stringResource(R.string.settings_gesture_pinch_in)
}

/** The gesture's name inside the phone, where the column already says one or two fingers. */
@Composable
private fun gestureShortLabel(gesture: HomeGesture): String = when (gesture) {
    HomeGesture.TWO_FINGER_SWIPE_UP -> stringResource(R.string.settings_gesture_swipe_up)
    HomeGesture.TWO_FINGER_SWIPE_DOWN -> stringResource(R.string.settings_gesture_swipe_down)
    else -> gestureLabel(gesture)
}

@Composable
internal fun gestureActionTypeLabel(type: GestureActionType): String = when (type) {
    GestureActionType.NONE -> stringResource(R.string.settings_gesture_action_none)
    GestureActionType.APP -> stringResource(R.string.settings_gesture_action_app)
    GestureActionType.SHORTCUT -> stringResource(R.string.settings_gesture_action_shortcut)
    GestureActionType.PAGE -> stringResource(R.string.settings_gesture_action_page)
    GestureActionType.CATEGORY -> stringResource(R.string.settings_gesture_action_category)
    GestureActionType.SETTINGS -> stringResource(R.string.settings_gesture_action_settings)
    GestureActionType.NOTIFICATIONS -> stringResource(R.string.settings_gesture_action_notifications)
    GestureActionType.QUICK_SETTINGS -> stringResource(R.string.settings_gesture_action_quick_settings)
}

@Composable
internal fun gestureActionSummary(action: GestureAction, pages: List<LauncherPage>, apps: List<AppInfo>): String = when (action.type) {
    GestureActionType.APP -> {
        val componentName = action.payload?.let { ComponentName.unflattenFromString(it) }
        apps.find { it.componentName == componentName }?.label ?: gestureActionTypeLabel(action.type)
    }
    GestureActionType.PAGE -> {
        val pageId = action.payload?.toLongOrNull()
        val page = pages.find { it.id == pageId }
        page?.let { pageLabel(it, pages) } ?: gestureActionTypeLabel(action.type)
    }
    GestureActionType.CATEGORY -> com.bisc.flauncher.ui.home.LauncherCategories.find(action.payload?.toLongOrNull())?.category?.name ?: gestureActionTypeLabel(action.type)
    else -> gestureActionTypeLabel(action.type)
}

/** What one gesture's marker on [GesturePhone] shows. */
internal data class GestureMarker(val summary: String, val enabled: Boolean)

/** A phone with every home gesture marked where it happens: one finger on the left, two fingers on
 *  the right, side by side by direction. Swiping down at the top, tapping and pinching in the
 *  middle, swiping up at the bottom. */
@Composable
internal fun GesturePhone(markers: Map<HomeGesture, GestureMarker>, onEdit: (HomeGesture) -> Unit, widthFraction: Float = 0.92f) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .clip(RoundedCornerShape(40.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(3.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(40.dp))
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // The camera hole, so it reads as a phone at a glance.
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outline))
            GesturePair(
                left = { GestureMarkerChip(HomeGesture.SWIPE_DOWN, Icons.Filled.ArrowDownward, markers, onEdit, it) },
                right = { GestureMarkerChip(HomeGesture.TWO_FINGER_SWIPE_DOWN, Icons.Filled.SwipeDown, markers, onEdit, it) },
                header = true
            )
            Spacer(Modifier.height(36.dp))
            GesturePair(
                left = { modifier ->
                    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GestureMarkerChip(HomeGesture.DOUBLE_TAP, Icons.Filled.TouchApp, markers, onEdit, Modifier.fillMaxWidth())
                        GestureMarkerChip(HomeGesture.LONG_PRESS, Icons.Filled.Timer, markers, onEdit, Modifier.fillMaxWidth())
                    }
                },
                right = { GestureMarkerChip(HomeGesture.PINCH_IN, Icons.Filled.Pinch, markers, onEdit, it) }
            )
            Spacer(Modifier.height(36.dp))
            GesturePair(
                left = { GestureMarkerChip(HomeGesture.SWIPE_UP, Icons.Filled.ArrowUpward, markers, onEdit, it) },
                right = { GestureMarkerChip(HomeGesture.TWO_FINGER_SWIPE_UP, Icons.Filled.SwipeUp, markers, onEdit, it) }
            )
            // The gesture bar.
            Box(modifier = Modifier.fillMaxWidth(0.35f).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outline))
        }
    }
}

/** One finger on the left, two fingers on the right, both as tall as the taller one. The first pair
 *  names the two sides. */
@Composable
private fun GesturePair(left: @Composable (Modifier) -> Unit, right: @Composable (Modifier) -> Unit, header: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (header) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.settings_gesture_one_finger, R.string.settings_gesture_two_fingers).forEach { res ->
                    Text(
                        stringResource(res),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            left(Modifier.weight(1f).fillMaxHeight())
            right(Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun GestureMarkerChip(
    gesture: HomeGesture,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    markers: Map<HomeGesture, GestureMarker>,
    onEdit: (HomeGesture) -> Unit,
    modifier: Modifier = Modifier
) {
    val marker = markers[gesture] ?: return
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .then(if (marker.enabled) Modifier.clickable { onEdit(gesture) } else Modifier)
            .alpha(if (marker.enabled) 1f else 0.5f)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Symbol above the name: half a phone wide is too narrow for both in a line.
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(
            gestureShortLabel(gesture),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            marker.summary,
            style = MaterialTheme.typography.titleSmall,
            color = if (marker.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
