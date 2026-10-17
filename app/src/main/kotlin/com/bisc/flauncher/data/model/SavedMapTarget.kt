package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A saved navigation destination — one of the global "Adressen" Elements (see Settings -> Elements
 *  -> Addresses), placeable anywhere a [GridItemType.MAP_TARGET] entry is allowed. */
@Entity(tableName = "saved_map_targets")
data class SavedMapTarget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val address: String = "",
    // Package name of the navigation app to prefer for this address specifically — overrides the
    // global default navigation app (ExternalApps.maps) when set, see launchMapTarget.
    val navigationApp: String? = null,
    // Key into PictogramCatalog; null falls back to the default Place glyph.
    val iconKey: String? = null,
    // Null means "adaptive black/white", the same default every other Element icon uses.
    val backgroundColor: Int? = null,
    val iconColor: Int? = null
)
