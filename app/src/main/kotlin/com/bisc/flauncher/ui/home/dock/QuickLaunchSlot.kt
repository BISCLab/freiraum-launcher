package com.bisc.flauncher.ui.home.dock

import android.content.ComponentName
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.bisc.flauncher.data.model.DockPowerButtonAction

/**
 * One rendered dock slot — resolved from [com.bisc.flauncher.data.model.DockSlot] (DB row: type +
 * componentName/action + optional color overrides) plus live app info. Every slot renders as the exact
 * same uniform squircle button regardless of kind (see [DockDimens]/[SearchBar]) — there's no longer a
 * special "Hub orb"; [PowerButton] slots are what replaced it, each a deliberate single-purpose button.
 */
sealed interface DockSlotUi {
    val index: Int
    val backgroundColor: Color
    val iconColor: Color

    /** [componentName] null means "not set yet" — shows a neutral add-placeholder, same invitation the old dock's "Not set" slots gave. */
    data class App(
        override val index: Int,
        val componentName: ComponentName?,
        val label: String?,
        override val backgroundColor: Color,
        override val iconColor: Color = Color.White
    ) : DockSlotUi

    data class PowerButton(
        override val index: Int,
        val action: DockPowerButtonAction,
        override val backgroundColor: Color,
        override val iconColor: Color = Color.White
    ) : DockSlotUi
}

val UNSET_APP_SLOT_COLOR = Color(0xFF6B7280)

/** This action's own built-in icon — see [defaultColor] for where the matching accent colors came from. */
fun DockPowerButtonAction.defaultIcon(): ImageVector = when (this) {
    DockPowerButtonAction.TEXT_SEARCH -> Icons.Filled.Search
    DockPowerButtonAction.CATEGORY_SEARCH -> Icons.Filled.GridView
    DockPowerButtonAction.APP_DRAWER -> Icons.Filled.Apps
    DockPowerButtonAction.APP_LIST -> Icons.AutoMirrored.Filled.ViewList
}

/** Default accent color per action — text-search/category-search match the user's Figma dock exactly; app-drawer/app-list are new, picked to stay visually distinct from every other dock color. */
fun DockPowerButtonAction.defaultColor(): Color = when (this) {
    DockPowerButtonAction.TEXT_SEARCH -> Color(0xFF1AF0FF)
    DockPowerButtonAction.CATEGORY_SEARCH -> Color(0xFFA14AFF)
    DockPowerButtonAction.APP_DRAWER -> Color(0xFF6366F1)
    DockPowerButtonAction.APP_LIST -> Color(0xFFF43F5E)
}

/** Placeholder glyph for an unset App slot — invites the user to tap and assign one. */
val unsetAppSlotIcon: ImageVector = Icons.Filled.Add
