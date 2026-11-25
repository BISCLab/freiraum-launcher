package com.bisc.flauncher.data.model

import org.json.JSONObject

/** What a tile (a folder or a category lying in the Dock, the Panel, a box or a folder) shows. */
enum class TileKind { CONTENT, SYMBOL, LETTER }

/** The fill behind it: the automatic color, a chosen one, or none. OUTLINE is from before the
 *  border had its own switch: no fill, a border. */
enum class TileSurface { AUTO, COLOR, OUTLINE, NONE }

/** The tile's corners when it has its own: fully round, soft or nearly square. */
enum class TileCorners { ROUND, SOFT, SQUARE }

/**
 * How a folder or a category looks as a tile, the same for both: what it shows, the symbol (a
 * pictogram key, or "pack:<package>|<drawable>" for a picture from an icon pack), the surface
 * behind it and the symbol's color (null = black or white, whichever reads on the surface).
 */
data class TileFace(
    val kind: TileKind = TileKind.CONTENT,
    val symbol: String? = null,
    val surface: TileSurface = TileSurface.AUTO,
    val surfaceColor: Int? = null,
    val symbolColor: Int? = null,
    /** A border around it; null = as older tiles had it (only with OUTLINE). */
    val border: Boolean? = null,
    /** The border's color; null = the color of the text around it. */
    val borderColor: Int? = null,
    /** The border's width in dp; null = 1.5. */
    val borderWidth: Float? = null,
    /** Its own corners from before they were set in percent, see [ownCornerPercent]. */
    val corners: TileCorners? = null,
    /** Its own corners on the same scale as every other part (100 = fully round); null = like
     *  the app icons around it. */
    val cornerPercent: Int? = null,
    /** How solid the fill is, 0..1; null = fully. */
    val surfaceAlpha: Float? = null
) {
    /** Its own corners in percent, older ones read from [corners]; null = like the app icons. */
    val ownCornerPercent: Int?
        get() = cornerPercent ?: when (corners) {
            TileCorners.ROUND -> 100
            TileCorners.SOFT -> 56
            TileCorners.SQUARE -> 16
            null -> null
        }

    val hasFill: Boolean get() = surface == TileSurface.AUTO || surface == TileSurface.COLOR
    val hasBorder: Boolean get() = border ?: (surface == TileSurface.OUTLINE)

    /** The icon pack and drawable of a picture from an icon pack, else null. */
    val packIcon: Pair<String, String>?
        get() = symbol?.takeIf { it.startsWith(PACK_PREFIX) }?.removePrefix(PACK_PREFIX)
            ?.split('|', limit = 2)?.takeIf { it.size == 2 }?.let { it[0] to it[1] }

    fun toJson(): String = JSONObject().apply {
        put("kind", kind.name)
        symbol?.let { put("symbol", it) }
        put("surface", surface.name)
        surfaceColor?.let { put("surfaceColor", it) }
        symbolColor?.let { put("symbolColor", it) }
        border?.let { put("border", it) }
        borderColor?.let { put("borderColor", it) }
        borderWidth?.let { put("borderWidth", it.toDouble()) }
        corners?.let { put("corners", it.name) }
        cornerPercent?.let { put("cornerPercent", it) }
        surfaceAlpha?.let { put("surfaceAlpha", it.toDouble()) }
    }.toString()

    companion object {
        const val PACK_PREFIX = "pack:"

        fun packSymbol(pack: String, drawable: String): String = "$PACK_PREFIX$pack|$drawable"

        fun fromJson(text: String?): TileFace? = text?.let {
            runCatching {
                val json = JSONObject(it)
                TileFace(
                    kind = runCatching { TileKind.valueOf(json.optString("kind")) }.getOrDefault(TileKind.CONTENT),
                    symbol = json.optString("symbol").ifBlank { null },
                    surface = runCatching { TileSurface.valueOf(json.optString("surface")) }.getOrDefault(TileSurface.AUTO),
                    surfaceColor = if (json.has("surfaceColor")) json.getInt("surfaceColor") else null,
                    symbolColor = if (json.has("symbolColor")) json.getInt("symbolColor") else null,
                    border = if (json.has("border")) json.getBoolean("border") else null,
                    borderColor = if (json.has("borderColor")) json.getInt("borderColor") else null,
                    borderWidth = if (json.has("borderWidth")) json.getDouble("borderWidth").toFloat() else null,
                    corners = json.optString("corners").ifBlank { null }?.let { c -> runCatching { TileCorners.valueOf(c) }.getOrNull() },
                    cornerPercent = if (json.has("cornerPercent")) json.getInt("cornerPercent") else null,
                    surfaceAlpha = if (json.has("surfaceAlpha")) json.getDouble("surfaceAlpha").toFloat() else null
                )
            }.getOrNull()
        }
    }
}

/** How a category's label looked before it was stored as a [TileFace]: in a pill of its color, in
 *  an outline, as plain text, or both. */
enum class CategoryNameStyle {
    PILL, OUTLINE, TEXT, PILL_OUTLINE;

    val hasFill: Boolean get() = this == PILL || this == PILL_OUTLINE
    val hasBorder: Boolean get() = this == OUTLINE || this == PILL_OUTLINE
}
