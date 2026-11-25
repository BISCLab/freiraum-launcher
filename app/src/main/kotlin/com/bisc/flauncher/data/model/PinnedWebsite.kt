package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A saved bookmark - one of the global "Internetadressen" Elements (see Settings -> Elements ->
 *  Links), placeable anywhere a [GridItemType.WEBSITE] entry is allowed. */
@Entity(tableName = "pinned_websites")
data class PinnedWebsite(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val url: String,
    // Package name of the browser to prefer for this link specifically - overrides the global
    // default browser (ExternalApps.browser) when set, see openWebsite.
    val browserApp: String? = null,
    // Key into PictogramCatalog; null falls back to the default Language glyph.
    val iconKey: String? = null,
    // Null means "adaptive black/white", the same default every other Element icon uses.
    val backgroundColor: Int? = null,
    val iconColor: Int? = null,
    // How it looks as a tile, see [TileFace]; null = from iconKey and the colors, see [face].
    val tileFace: String? = null
)

/** How it looks as a tile; from before [TileFace], read from its symbol and colors. */
fun PinnedWebsite.face(): TileFace = TileFace.fromJson(tileFace) ?: TileFace(
    kind = TileKind.SYMBOL,
    symbol = iconKey,
    surface = if (backgroundColor != null) TileSurface.COLOR else TileSurface.AUTO,
    surfaceColor = backgroundColor,
    symbolColor = iconColor
)

