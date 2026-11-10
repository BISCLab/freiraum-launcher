package com.bisc.flauncher.ui.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.AppInfo

/** What an app's long-press menu needs from the launcher; there only on the home screen, so apps
 *  shown elsewhere (settings previews) have no menu. */
class AppMenuHost(
    val favoriteIds: Set<String>,
    val toggleFavorite: (AppInfo) -> Unit,
    val shortcutsFor: suspend (String) -> List<ShortcutInfo>,
    val shortcutIcon: (ShortcutInfo) -> Drawable?,
    val launchShortcut: (String, String) -> Unit,
    /** Opens this app's page in the launcher's own settings: name, icon, where it shows. */
    val edit: (ComponentName) -> Unit
)

val LocalAppMenuHost = compositionLocalOf<AppMenuHost?> { null }

/** Gives everything inside the long-press menu for apps. */
@Composable
fun AppMenuProvider(onEdit: (ComponentName) -> Unit, viewModel: HomeViewModel = hiltViewModel(), content: @Composable () -> Unit) {
    val favoriteIds by viewModel.searchFavoriteIds.collectAsState()
    val currentOnEdit by androidx.compose.runtime.rememberUpdatedState(onEdit)
    val host = remember(favoriteIds) {
        AppMenuHost(
            favoriteIds = favoriteIds,
            toggleFavorite = viewModel::toggleSearchFavorite,
            shortcutsFor = viewModel::appShortcutsFor,
            shortcutIcon = viewModel::loadShortcutIcon,
            launchShortcut = viewModel::launchAppShortcut,
            edit = { currentOnEdit(it) }
        )
    }
    CompositionLocalProvider(LocalAppMenuHost provides host, content = content)
}

/** An app's long-press menu: [open] goes on the long press, [Menu] inside the app's own layout, so
 *  the menu opens right at it. */
class AppMenuHandle(val open: () -> Unit, val Menu: @Composable () -> Unit)

/** The long-press menu of [component], null where there is none. */
@Composable
fun rememberAppMenu(component: ComponentName?, label: String): AppMenuHandle? {
    val host = LocalAppMenuHost.current
    if (host == null || component == null) return null
    val haptics = LocalHapticFeedback.current
    val open = remember(component) { mutableStateOf(false) }
    return remember(component, label, host) {
        AppMenuHandle(
            open = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                open.value = true
            },
            Menu = {
                PieceMenu(expanded = open.value, onDismissRequest = { open.value = false }) {
                    AppMenuItems(AppInfo(component, label), host) { open.value = false }
                }
            }
        )
    }
}

private const val MAX_MENU_SHORTCUTS = 4

/** One entry of the app menu: a symbol and a line of text. */
@Composable
private fun MenuEntry(text: String, onClick: () -> Unit, leading: @Composable () -> Unit) =
    PieceMenuItem(text = { Text(text, maxLines = 1) }, onClick = onClick, leadingIcon = leading)

@Composable
private fun AppMenuItems(app: AppInfo, host: AppMenuHost, close: () -> Unit) {
    val context = LocalContext.current
    val packageName = app.componentName.packageName
    val shortcuts by produceState(emptyList<ShortcutInfo>(), packageName) {
        value = host.shortcutsFor(packageName).filter { it.isEnabled }.take(MAX_MENU_SHORTCUTS)
    }
    shortcuts.forEach { shortcut ->
        val icon = remember(shortcut.id) { runCatching { host.shortcutIcon(shortcut)?.toBitmap(72, 72)?.asImageBitmap() }.getOrNull() }
        MenuEntry(
            text = (shortcut.shortLabel ?: shortcut.longLabel ?: shortcut.id).toString(),
            onClick = { close(); host.launchShortcut(packageName, shortcut.id) }
        ) { if (icon != null) Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(24.dp)) }
    }
    // A wider gap sets the app's own shortcuts apart from what the launcher offers.
    if (shortcuts.isNotEmpty()) Spacer(Modifier.height(4.dp))

    val favorite = app.componentName.flattenToString() in host.favoriteIds
    MenuEntry(
        text = stringResource(if (favorite) R.string.search_favorite_remove else R.string.search_favorite_add),
        onClick = { close(); host.toggleFavorite(app) }
    ) { Icon(if (favorite) Icons.Filled.Star else Icons.Filled.StarBorder, contentDescription = null) }
    MenuEntry(text = stringResource(R.string.app_menu_edit), onClick = { close(); host.edit(app.componentName) }) {
        Icon(Icons.Filled.Edit, contentDescription = null)
    }
    MenuEntry(text = stringResource(R.string.app_menu_info), onClick = { close(); openAppInfo(context, packageName) }) {
        Icon(Icons.Outlined.Info, contentDescription = null)
    }
    if (remember(packageName) { canUninstall(context, packageName) }) {
        MenuEntry(text = stringResource(R.string.app_menu_uninstall), onClick = { close(); requestUninstall(context, packageName) }) {
            Icon(Icons.Filled.DeleteOutline, contentDescription = null)
        }
    }
}

private fun openAppInfo(context: Context, packageName: String) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/** Apps that came with the phone cannot be removed, only switched off in their App info. */
private fun canUninstall(context: Context, packageName: String): Boolean = runCatching {
    context.packageManager.getApplicationInfo(packageName, 0).flags and ApplicationInfo.FLAG_SYSTEM == 0
}.getOrDefault(false)

/** Hands over to Android, which asks before it removes anything. */
private fun requestUninstall(context: Context, packageName: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
