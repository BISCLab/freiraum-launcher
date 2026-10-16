package com.bisc.flauncher.ui.settings

import android.content.ComponentName
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwipeDown
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

/** One row per [HomeGesture], each bindable to any [GestureActionType]. */
@Composable
fun SettingsGesturesScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val gestureActions by viewModel.gestureActions.collectAsState()
    // Unfiltered — a gesture already bound to an app keeps showing its.
    val labeledApps by viewModel.labeledApps.collectAsState()
    val browsableApps by viewModel.browsableApps.collectAsState()
    val allPages by viewModel.allPages.collectAsState()
    val homeDockRow by viewModel.homeDockRow.collectAsState()
    val homePanelRow by viewModel.homePanelRow.collectAsState()
    // Mirrors the reservation HomeScreen's detectHomeGestures actually enforces — a gesture claimed
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
    var pendingShortcutGesture by remember { mutableStateOf<HomeGesture?>(null) }

    SettingsSubScreen(title = stringResource(R.string.settings_section_gestures), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup {
            HomeGesture.entries.forEach { gesture ->
                val action = gestureActions[gesture] ?: GestureAction.NONE
                val locked = lockedReason(gesture)
                SettingsPreference(
                    icon = gestureIcon(gesture),
                    title = gestureLabel(gesture),
                    summary = locked ?: gestureActionSummary(action, allPages, labeledApps),
                    onClick = { editingGesture = gesture },
                    enabled = locked == null
                )
            }
        }
    }

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
                    else -> viewModel.setGestureAction(gesture, GestureAction(type))
                }
            },
            onDismiss = { editingGesture = null }
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
            apps = browsableApps,
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
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            types.forEach { type ->
                RadioPreference(gestureActionTypeLabel(type), type, current, onSelected)
            }
        }
    }
}

internal fun gestureIcon(gesture: HomeGesture) = when (gesture) {
    HomeGesture.LONG_PRESS -> Icons.Filled.TouchApp
    HomeGesture.DOUBLE_TAP -> Icons.Filled.TouchApp
    HomeGesture.SWIPE_UP -> Icons.Filled.SwipeUp
    HomeGesture.SWIPE_DOWN -> Icons.Filled.SwipeDown
}

@Composable
internal fun gestureLabel(gesture: HomeGesture): String = when (gesture) {
    HomeGesture.LONG_PRESS -> stringResource(R.string.settings_gesture_long_press)
    HomeGesture.DOUBLE_TAP -> stringResource(R.string.settings_gesture_double_tap)
    HomeGesture.SWIPE_UP -> stringResource(R.string.settings_gesture_swipe_up)
    HomeGesture.SWIPE_DOWN -> stringResource(R.string.settings_gesture_swipe_down)
}

@Composable
internal fun gestureActionTypeLabel(type: GestureActionType): String = when (type) {
    GestureActionType.NONE -> stringResource(R.string.settings_gesture_action_none)
    GestureActionType.APP -> stringResource(R.string.settings_gesture_action_app)
    GestureActionType.SHORTCUT -> stringResource(R.string.settings_gesture_action_shortcut)
    GestureActionType.PAGE -> stringResource(R.string.settings_gesture_action_page)
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
    else -> gestureActionTypeLabel(action.type)
}
