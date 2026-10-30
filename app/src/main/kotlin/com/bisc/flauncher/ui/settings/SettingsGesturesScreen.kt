package com.bisc.flauncher.ui.settings

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
    // Unfiltered - a gesture already bound to an app keeps showing its.
    val labeledApps by viewModel.labeledApps.collectAsState()
    val browsableApps by viewModel.browsableApps.collectAsState()
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
    var pendingShortcutGesture by remember { mutableStateOf<HomeGesture?>(null) }

    SettingsSubScreen(title = stringResource(R.string.settings_section_gestures), navController = navController, onBack = onBack) {
        // A phone with each gesture marked where it happens, instead of a list - tap a marker to
        // change what it does.
        val markers = HomeGesture.entries.associateWith { gesture ->
            val locked = lockedReason(gesture)
            GestureMarker(
                summary = locked ?: gestureActionSummary(gestureActions[gesture] ?: GestureAction.NONE, allPages, labeledApps),
                enabled = locked == null
            )
        }
        GesturePhone(markers = markers, onEdit = { editingGesture = it })
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

/** What one gesture's marker on [GesturePhone] shows. */
internal data class GestureMarker(val summary: String, val enabled: Boolean)

/** A phone outline with the four home gestures marked where they happen: swipe down at the top,
 *  double tap and long press in the middle, swipe up at the bottom. */
@Composable
private fun GesturePhone(markers: Map<HomeGesture, GestureMarker>, onEdit: (HomeGesture) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .aspectRatio(0.52f)
                .clip(RoundedCornerShape(40.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(3.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(40.dp))
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // The camera hole, so it reads as a phone at a glance.
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outline))
            GestureMarkerChip(HomeGesture.SWIPE_DOWN, Icons.Filled.ArrowDownward, markers, onEdit)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GestureMarkerChip(HomeGesture.DOUBLE_TAP, Icons.Filled.TouchApp, markers, onEdit)
                GestureMarkerChip(HomeGesture.LONG_PRESS, Icons.Filled.TouchApp, markers, onEdit)
            }
            GestureMarkerChip(HomeGesture.SWIPE_UP, Icons.Filled.ArrowUpward, markers, onEdit)
            // The gesture bar.
            Box(modifier = Modifier.fillMaxWidth(0.35f).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outline))
        }
    }
}

@Composable
private fun GestureMarkerChip(
    gesture: HomeGesture,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    markers: Map<HomeGesture, GestureMarker>,
    onEdit: (HomeGesture) -> Unit
) {
    val marker = markers[gesture] ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .then(if (marker.enabled) Modifier.clickable { onEdit(gesture) } else Modifier)
            .alpha(if (marker.enabled) 1f else 0.5f)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Text(gestureLabel(gesture), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
