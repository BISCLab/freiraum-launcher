package com.bisc.flauncher.ui.settings

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.ui.theme.SettingsThemed
import com.bisc.flauncher.ui.theme.current

/** Tablets held sideways (and other screens that wide) show the settings in two parts: the main
 *  sections stay on the left, the chosen page is on the right. */
@Composable
fun settingsTwoPane(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.smallestScreenWidthDp >= 600 && configuration.screenWidthDp >= 840
}

private val SIDE_RAIL_WIDTH = 280.dp

/** The room a settings page leaves on its left for the sections, 0 on phones. */
@Composable
fun settingsSideRailWidth(): Dp = if (settingsTwoPane()) SIDE_RAIL_WIDTH else 0.dp

/** Lets a page with unsaved changes ask before the sections on the left take the user away. */
internal object SettingsLeaveGuard {
    var current by mutableStateOf<(((() -> Unit)) -> Unit)?>(null)
}

private class SideSection(val route: String, val icon: ImageVector, val label: Int, val owns: List<String>, val area: com.bisc.flauncher.ui.theme.SettingsArea)

private val SECTIONS = listOf(
    SideSection("settings/appearance", Icons.Filled.Palette, R.string.settings_design_title, listOf("settings/appearance"), com.bisc.flauncher.ui.theme.SettingsArea.APPEARANCE),
    SideSection(
        "settings/pages", Icons.Filled.ViewCarousel, R.string.settings_section_pages,
        listOf("settings/pages", "settings/search", "settings/app_overview", "settings/app_grid", "settings/gestures"),
        com.bisc.flauncher.ui.theme.SettingsArea.PAGES
    ),
    SideSection("settings/elements", Icons.Filled.Apps, R.string.settings_section_elements, listOf("settings/elements", "settings/apps", "settings/folder"), com.bisc.flauncher.ui.theme.SettingsArea.ELEMENTS),
    SideSection("settings/system", Icons.Filled.PhoneAndroid, R.string.settings_system_title, listOf("settings/system", "settings/permissions"), com.bisc.flauncher.ui.theme.SettingsArea.SYSTEM),
    SideSection("settings/extensions", Icons.Filled.Extension, R.string.settings_group_extensions, listOf("settings/extensions", "settings/wallpaper_packs"), com.bisc.flauncher.ui.theme.SettingsArea.EXTENSIONS)
)

/** The sections on the left, drawn once over the settings pages so it stays put while they change. */
// currentBackStack is the only way to see which section the open page was reached through.
@SuppressLint("RestrictedApi")
@Composable
fun SettingsSideRail(navController: NavController) {
    if (!settingsTwoPane()) return
    val backStack by navController.currentBackStack.collectAsState()
    val settingsStack = backStack.mapNotNull { it.destination.route }.filter { it.startsWith("settings") }
    // The page right after the start page says which section this is; a page opened directly (from
    // the Text search) counts for the section it belongs to.
    val first = settingsStack.getOrNull(1)
    val selected = SECTIONS.firstOrNull { section -> first != null && section.owns.any { first == it || first.startsWith("$it/") } }
    val leave: (() -> Unit) -> Unit = { action -> SettingsLeaveGuard.current?.invoke(action) ?: action() }

    AnimatedVisibility(visible = settingsStack.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
        SettingsThemed {
            Column(
                modifier = Modifier
                    .width(SIDE_RAIL_WIDTH)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { leave { navController.popBackStack("settings", inclusive = false) } }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppLogo(size = 36.dp)
                    Text(
                        stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 14.dp)
                    )
                }
                SECTIONS.forEach { section ->
                    val isSelected = section == selected
                    val areaColor = section.area.current()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) areaColor.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable {
                                leave {
                                    navController.popBackStack("settings", inclusive = false)
                                    navController.navigate(section.route)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AreaSymbol(section.icon, section.area)
                        Text(
                            stringResource(section.label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.padding(start = 14.dp)
                        )
                    }
                }
            }
        }
    }
}
