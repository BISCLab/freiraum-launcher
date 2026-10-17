package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** What a [PowerButton] does when tapped. Unlike a plain [GridItemType.PAGE] link, it can also
 *  trigger the Panel/Dock directly instead of navigating anywhere. */
enum class PowerButtonActionType { PAGE, PANEL, DOCK }

/** A global "Powerbutton" Element (see Settings -> Elements -> Power buttons): a named action
 *  (open a page, or open the Panel/Dock) with its own icon colors, placeable anywhere a
 *  [GridItemType.POWER_BUTTON] entry is allowed. [actionPageId] is only meaningful when
 *  [actionType] is [PowerButtonActionType.PAGE]. */
@Entity(tableName = "power_buttons")
data class PowerButton(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val actionType: PowerButtonActionType = PowerButtonActionType.PAGE,
    val actionPageId: Long? = null,
    // Null means "adaptive black/white", the same default every other Element icon uses.
    val backgroundColor: Int? = null,
    val iconColor: Int? = null
)
