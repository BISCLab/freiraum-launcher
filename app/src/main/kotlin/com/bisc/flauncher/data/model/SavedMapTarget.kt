package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A saved navigation destination for the sidebar's Maps entry. */
@Entity(tableName = "saved_map_targets")
data class SavedMapTarget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val address: String = ""
)
