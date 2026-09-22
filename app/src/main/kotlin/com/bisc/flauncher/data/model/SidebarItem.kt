package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** What [SidebarItem.payload] refers to. */
enum class SidebarItemType {
    CATEGORY,
    APP,
    SHORTCUT,
    /** One sidebar entry; the order is user-defined. */
    MAP_TARGET
}

/** One sidebar entry; the order is user-defined. */
@Entity(tableName = "sidebar_items")
data class SidebarItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderIndex: Int,
    val type: SidebarItemType,
    val payload: String,
    val customIconKey: String? = null
)
