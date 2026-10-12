package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One of the dock's icon positions (0..N-1, in the bar's own left-to-right order) — a flexible-length
 * list now, not a fixed 4. Each slot is either a pinned app ([type] "APP", [componentName] set) or a
 * power button ([type] "POWER_BUTTON", [powerButtonAction] set to a [com.bisc.flauncher.data.model.DockPowerButtonAction]
 * name) that jumps to a specific screen (text search, category search, app drawer, app list, ...).
 * Stored as plain strings rather than a Room-converted enum so adding a new [DockPowerButtonAction] case
 * never needs a schema change. [backgroundColor]/[iconColor] are ARGB overrides; null means "use that
 * action's/app's own default colors".
 */
@Entity(tableName = "dock_slots")
data class DockSlot(
    @PrimaryKey val slotIndex: Int,
    val type: String = "APP",
    val componentName: String? = null,
    val powerButtonAction: String? = null,
    val backgroundColor: Int? = null,
    val iconColor: Int? = null
)
