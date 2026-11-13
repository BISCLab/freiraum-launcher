package com.bisc.flauncher.ui.settings

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Interests
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.LauncherSettingsTarget

/** A page "Quick access" can lead to: one of the overview pages that are otherwise several taps
 *  away. [pattern] is its navigation route as the NavHost knows it; [route] the one to open, with
 *  [LauncherSettingsTarget.HOME] for the home page's id. */
private class QuickTarget(val key: String, val icon: ImageVector, val labelRes: Int, val pattern: String, val route: String)

private const val HOME = LauncherSettingsTarget.HOME

/** In the order they show before anything was opened: the furthest away first. */
private val QUICK_TARGETS = listOf(
    QuickTarget("home", Icons.Filled.Home, R.string.settings_pages_home, "settings/pages/{pageId}/home", "settings/pages/$HOME/home"),
    QuickTarget("dock", Icons.Filled.ViewDay, R.string.settings_home_dock_label, "settings/pages/{pageId}/home/layout/dock", "settings/pages/$HOME/home/layout/dock"),
    QuickTarget("panel", Icons.Filled.Dashboard, R.string.settings_home_panel_label, "settings/pages/{pageId}/home/panel", "settings/pages/$HOME/home/panel"),
    QuickTarget("folders", Icons.Filled.Folder, R.string.settings_section_folder, "settings/folder", "settings/folder"),
    QuickTarget("gestures", Icons.Filled.Gesture, R.string.settings_section_gestures, "settings/gestures", "settings/gestures"),
    QuickTarget("icons", Icons.Filled.Interests, R.string.settings_icons_label, "settings/appearance/icons", "settings/appearance/icons"),
    QuickTarget("search", Icons.Filled.Search, R.string.settings_section_search_full, "settings/search", "settings/search"),
    QuickTarget("app_list", Icons.AutoMirrored.Filled.List, R.string.welcome_screen_app_list, "settings/app_overview", "settings/app_overview"),
    QuickTarget("app_grid", Icons.Filled.GridView, R.string.settings_pages_app_grid, "settings/app_grid", "settings/app_grid"),
    QuickTarget("background", Icons.Filled.Image, R.string.settings_section_global_background, "settings/appearance/background", "settings/appearance/background"),
    QuickTarget("designs", Icons.Filled.AutoAwesome, R.string.designs_title, "settings/appearance/designs", "settings/appearance/designs"),
    QuickTarget("wallpapers", Icons.Filled.Wallpaper, R.string.packs_title, "settings/wallpaper_packs", "settings/wallpaper_packs"),
    QuickTarget("widgets", Icons.Filled.Widgets, R.string.extensions_widgets_title, "settings/extensions/widgets", "settings/extensions/widgets"),
    QuickTarget("colors", Icons.Filled.FormatColorFill, R.string.settings_colors_all, "settings/appearance/colors", "settings/appearance/colors"),
    QuickTarget("corners", Icons.Filled.RoundedCorner, R.string.settings_corners_all, "settings/appearance/corners", "settings/appearance/corners"),
    QuickTarget("apps", Icons.Filled.Apps, R.string.settings_section_manage_apps, "settings/apps", "settings/apps"),
    QuickTarget("permissions", Icons.Filled.Security, R.string.settings_section_permissions, "settings/permissions", "settings/permissions")
)

private const val QUICK_SHOWN = 6
private const val PREFS = "settings_quick_access"
/** How fast an old visit counts less: after this many days it weighs half. */
private const val HALF_LIFE_DAYS = 14.0

/**
 * Counts the visits of the pages above, wherever they were opened from, so the ones used most move
 * to the front. Recent visits weigh more than old ones, so a page used a lot once does not stay on
 * top for good. Kept on the phone only, in the settings' own small store.
 */
object SettingsQuickAccess {
    fun recordVisit(context: Context, routePattern: String?) {
        val target = QUICK_TARGETS.firstOrNull { it.pattern == routePattern } ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val score = decayed(prefs.getFloat("score_${target.key}", 0f), prefs.getLong("at_${target.key}", now), now) + 1f
        prefs.edit().putFloat("score_${target.key}", score).putLong("at_${target.key}", now).apply()
    }

    private fun decayed(score: Float, at: Long, now: Long): Float {
        val days = (now - at).coerceAtLeast(0) / 86_400_000.0
        return (score * Math.pow(0.5, days / HALF_LIFE_DAYS)).toFloat()
    }

    internal fun ranked(context: Context): List<Int> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        return QUICK_TARGETS.indices.sortedWith(
            compareByDescending<Int> { i ->
                val key = QUICK_TARGETS[i].key
                decayed(prefs.getFloat("score_$key", 0f), prefs.getLong("at_$key", now), now)
            }.thenBy { it }
        ).take(QUICK_SHOWN)
    }
}

/** The quick-access tiles on the settings search page, for when nothing is typed yet. */
@Composable
internal fun SettingsQuickAccessGrid(onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val shown = remember { SettingsQuickAccess.ranked(context) }
    SettingsTileGrid(
        title = stringResource(R.string.settings_quick_label),
        info = stringResource(R.string.settings_quick_info),
        columns = 3,
        tiles = shown.map { i ->
            val target = QUICK_TARGETS[i]
            SettingsTile(target.icon, stringResource(target.labelRes), area = com.bisc.flauncher.ui.theme.settingsAreaFor(target.pattern)) { onOpen(target.route) }
        }
    )
}
