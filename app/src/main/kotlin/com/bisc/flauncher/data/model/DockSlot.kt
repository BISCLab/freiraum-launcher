package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One of the dock's 4 fixed quick-launch icon positions (0..3, in the pill's own left-to-right order). */
@Entity(tableName = "dock_slots")
data class DockSlot(
    @PrimaryKey val slotIndex: Int,
    val componentName: String
)
