package com.bisc.flauncher.data.model

import androidx.room.Entity

/** Cached appfilter.xml entry: the icon-pack drawable for an app activity. */
@Entity(tableName = "icon_pack_mappings", primaryKeys = ["iconPackPackage", "componentName"])
data class IconPackMapping(
    val iconPackPackage: String,
    val componentName: String,
    val drawableName: String
)
