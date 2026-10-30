package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** What an [ActionButton] does when tapped. Unlike a plain [GridItemType.PAGE] link, it can also
 *  trigger the Panel/Dock directly, jump straight into a curated Android settings screen, or open
 *  FLauncher's own Settings - instead of navigating anywhere inside the app's pages. */
enum class ActionButtonActionType { PAGE, PANEL, DOCK, ANDROID_SETTING, LAUNCHER_SETTINGS }

/** A global "Action button" Element (see Settings -> Elements -> Action buttons): a named action
 *  (open a page, open the Panel/Dock, open an Android settings screen, or open FLauncher's own
 *  Settings) with its own icon colors, placeable anywhere a [GridItemType.ACTION_BUTTON] entry is
 *  allowed. [actionPageId] is only meaningful when [actionType] is [ActionButtonActionType.PAGE];
 *  [androidSettingAction] only when it's [ActionButtonActionType.ANDROID_SETTING] - it holds one
 *  of [com.bisc.flauncher.data.model.SettingsShortcut.action]'s intent-action strings, the same
 *  curated list the search feature already uses for its own settings shortcuts. */
@Entity(tableName = "action_buttons")
data class ActionButton(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val actionType: ActionButtonActionType = ActionButtonActionType.PAGE,
    val actionPageId: Long? = null,
    val androidSettingAction: String? = null,
    // Key into PictogramCatalog; null falls back to the action-type glyph (Panel/Dock/Page/Setting).
    val iconKey: String? = null,
    // Null means "adaptive black/white", the same default every other Element icon uses.
    val backgroundColor: Int? = null,
    val iconColor: Int? = null
)
