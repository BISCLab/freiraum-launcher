package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** An app shortcut pinned as an element (dock, panel, element box or folder). */
@Entity(tableName = "pinned_shortcuts")
data class PinnedShortcut(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val shortcutId: String,
    val label: String
)
