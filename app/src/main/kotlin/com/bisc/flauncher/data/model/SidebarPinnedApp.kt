package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** An app added to the sidebar's app list. */
@Entity(tableName = "sidebar_pinned_apps")
data class SidebarPinnedApp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val componentName: String
)
