package com.bisc.flauncher.ui.settings

import androidx.compose.material.icons.filled.Settings
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
private class QuickTarget(val key: String, val icon: ImageVector, val labelRes: Int, val pattern: String, val route: String, val path: String)

private const val HOME = LauncherSettingsTarget.HOME

/** In the order they show before anything was opened: the furthest away first. */
private val QUICK_TARGETS = listOf(
    QuickTarget("home", Icons.Filled.Home, R.string.settings_pages_home, "settings/pages/{pageId}/home", "settings/pages/$HOME/home", "pages/home"),
    QuickTarget("dock", Icons.Filled.ViewDay, R.string.settings_home_dock_label, "settings/pages/{pageId}/home/layout/dock", "settings/pages/$HOME/home/layout/dock", "pages/home/layout/dock"),
    QuickTarget("panel", Icons.Filled.Dashboard, R.string.settings_home_panel_label, "settings/pages/{pageId}/home/panel", "settings/pages/$HOME/home/panel", "pages/home/layout/panel"),
    QuickTarget("folders", Icons.Filled.Folder, R.string.settings_section_folder, "settings/folder", "settings/folder", "elements/folders"),
    QuickTarget("gestures", Icons.Filled.Gesture, R.string.settings_section_gestures, "settings/gestures", "settings/gestures", "pages/gestures"),
    QuickTarget("icons", Icons.Filled.Interests, R.string.settings_icons_label, "settings/appearance/icons", "settings/appearance/icons", "appearance/icons"),
    QuickTarget("search", Icons.Filled.Search, R.string.settings_section_search_full, "settings/search", "settings/search", "pages/search"),
    QuickTarget("app_list", Icons.AutoMirrored.Filled.List, R.string.welcome_screen_app_list, "settings/app_overview", "settings/app_overview", "pages/app_list"),
    QuickTarget("app_grid", Icons.Filled.GridView, R.string.settings_pages_app_grid, "settings/app_grid", "settings/app_grid", "pages/app_grid"),
    QuickTarget("background", Icons.Filled.Image, R.string.settings_section_global_background, "settings/appearance/background", "settings/appearance/background", "appearance/background"),
    QuickTarget("designs", Icons.Filled.AutoAwesome, R.string.designs_title, "settings/appearance/designs", "settings/appearance/designs", "designs"),
    QuickTarget("wallpapers", Icons.Filled.Wallpaper, R.string.packs_title, "settings/wallpaper_packs", "settings/wallpaper_packs", "wallpapers"),
    QuickTarget("widgets", Icons.Filled.Widgets, R.string.extensions_widgets_title, "settings/extensions/widgets", "settings/extensions/widgets", "widgets"),
    QuickTarget("colors", Icons.Filled.FormatColorFill, R.string.settings_colors_all, "settings/appearance/colors", "settings/appearance/colors", "appearance/colors"),
    QuickTarget("corners", Icons.Filled.RoundedCorner, R.string.settings_corners_all, "settings/appearance/corners", "settings/appearance/corners", "appearance/corners"),
    QuickTarget("apps", Icons.Filled.Apps, R.string.settings_section_manage_apps, "settings/apps", "settings/apps", "elements/apps"),
    QuickTarget("permissions", Icons.Filled.Security, R.string.settings_section_permissions, "settings/permissions", "settings/permissions", "system/permissions")
)

private const val QUICK_SHOWN = 9
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

    /** What usually comes next after a page: where a new folder is placed, what goes with a design. */
    private val RELATED = mapOf(
        "folders" to listOf("dock", "panel", "home"),
        "apps" to listOf("folders", "dock", "home", "icons"),
        "icons" to listOf("apps", "designs", "corners"),
        "designs" to listOf("wallpapers", "colors", "corners", "icons"),
        "wallpapers" to listOf("background", "designs"),
        "background" to listOf("wallpapers", "designs", "colors"),
        "colors" to listOf("corners", "designs", "background"),
        "corners" to listOf("colors", "designs", "icons"),
        "home" to listOf("dock", "panel", "folders", "widgets"),
        "dock" to listOf("panel", "folders", "home"),
        "panel" to listOf("dock", "folders", "widgets"),
        "widgets" to listOf("home", "panel"),
        "search" to listOf("app_list", "gestures", "apps"),
        "app_list" to listOf("app_grid", "search", "apps"),
        "app_grid" to listOf("app_list", "folders", "apps"),
        "gestures" to listOf("home", "search", "app_list"),
        "permissions" to listOf("apps", "widgets")
    )

    /** A route's address, as its parts (ids like {pageId} left out), to find the page it lies under. */
    private fun steps(route: String?): List<String> =
        route?.removePrefix("settings")?.split('/')?.filter { it.isNotEmpty() && !it.startsWith("{") }.orEmpty()

    /** Where [route] lies as one taps through the settings: the way to the page above it, one step
     *  further when it is deeper still; the four areas by themselves; the start page as no way. */
    private fun wayTo(route: String?): List<String> {
        val parts = steps(route)
        if (parts.isEmpty()) return emptyList()
        val under = QUICK_TARGETS.filter { t -> steps(t.pattern).let { it.isNotEmpty() && parts.take(it.size) == it } }
            .maxByOrNull { steps(it.pattern).size }
        if (under != null) {
            val way = under.path.split('/')
            return if (parts.size > steps(under.pattern).size) way + "deeper" else way
        }
        return when (parts.first()) {
            "appearance" -> listOf("appearance")
            "pages" -> listOf("pages")
            "elements" -> listOf("elements")
            "system", "about" -> listOf("system")
            else -> listOf(parts.first())
        }
    }

    /** How many steps lie between two places: up to where their ways part, then down again. */
    private fun distance(a: List<String>, b: List<String>): Int {
        val common = a.zip(b).takeWhile { (x, y) -> x == y }.size
        return a.size + b.size - 2 * common
    }

    /**
     * The pages to offer from [fromRoute], the page the search was opened on. First what usually
     * comes next after it, then the pages furthest away from it (those near are a step away anyway),
     * then the ones opened often. The page itself is left out.
     */
    internal fun ranked(context: Context, fromRoute: String?, count: Int): List<Int> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val from = wayTo(fromRoute)
        // The page the search came from, or the one it lies under.
        val fromKey = QUICK_TARGETS.filter { t -> steps(t.pattern).let { it.isNotEmpty() && from.take(it.size) == it } }
            .maxByOrNull { steps(it.pattern).size }?.key
        val related = RELATED[fromKey].orEmpty()
        fun score(i: Int): Double {
            val t = QUICK_TARGETS[i]
            val use = decayed(prefs.getFloat("score_${t.key}", 0f), prefs.getLong("at_${t.key}", now), now).coerceAtMost(6f)
            val rel = related.indexOf(t.key).let { if (it < 0) 0.0 else 6.0 - it }
            return rel + distance(from, t.path.split('/')) * 0.6 + use * 0.8
        }
        return QUICK_TARGETS.indices
            .filter { QUICK_TARGETS[it].key != fromKey }
            .sortedWith(compareByDescending<Int> { score(it) }.thenBy { it })
            .take(count)
    }
}

/** The quick-access tiles on the settings search page, for when nothing is typed yet: nine, the
 *  settings' start page first when the search came from deeper in. Flat tiles, so all fit above
 *  the keyboard. */
@Composable
internal fun SettingsQuickAccessGrid(fromRoute: String?, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val fromStart = fromRoute == null || fromRoute == "settings"
    val shown = remember(fromRoute) { SettingsQuickAccess.ranked(context, fromRoute, if (fromStart) QUICK_SHOWN else QUICK_SHOWN - 1) }
    val start = if (fromStart) null else SettingsTile(androidx.compose.material.icons.Icons.Filled.Settings, stringResource(R.string.settings_title), area = com.bisc.flauncher.ui.theme.SettingsArea.NEUTRAL) { onOpen("settings") }
    SettingsTileGrid(
        title = stringResource(R.string.settings_quick_label),
        info = stringResource(R.string.settings_quick_info),
        columns = 3,
        compact = true,
        tiles = listOfNotNull(start) + shown.map { i ->
            val target = QUICK_TARGETS[i]
            SettingsTile(target.icon, stringResource(target.labelRes), area = com.bisc.flauncher.ui.theme.settingsAreaFor(target.pattern)) { onOpen(target.route) }
        }
    )
}
