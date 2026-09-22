package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A website pinned to the sidebar. */
@Entity(tableName = "pinned_websites")
data class PinnedWebsite(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val url: String
)
