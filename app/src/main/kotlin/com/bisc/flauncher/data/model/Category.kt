package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A smart category fills itself; it stores only its rule (and [Category.ruleParam]), the members
 *  are worked out live. [NAME]: apps whose name holds a text. [ANDROID_KIND]: apps of one of
 *  Android's own kinds. [PROFILE]: apps of the work profile or the private space. */
enum class CategoryRule { NEW, UNUSED, FREQUENT, UNCATEGORIZED, NAME, ANDROID_KIND, PROFILE }

/** A category: a label that apps and other elements carry. Unlike a folder it has no place of its
 *  own, and one app can be in many. [rule] set means a smart category (see [CategoryRule]). */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    // Key into PictogramCatalog; null shows the default label glyph.
    val iconKey: String? = null,
    // Null is the accent color.
    val color: Int? = null,
    val orderIndex: Int = 0,
    val rule: String? = null,
    // The rule's own setting: days for NEW and UNUSED, how many for FREQUENT, the text for NAME,
    // the kind for ANDROID_KIND. Null = the rule's default.
    val ruleParam: String? = null,
    // True: the members keep the order they were put in; false: apps by name, then the rest.
    val manualOrder: Boolean = false,
    // How it looks as a tile, see [TileFace]; null = read from iconKey and color, see [face].
    val tileFace: String? = null,
    // How its name looks, a [CategoryNameStyle]; null = read from color, see [nameLook].
    val nameStyle: String? = null
) {
    val smartRule: CategoryRule? get() = rule?.let { runCatching { CategoryRule.valueOf(it) }.getOrNull() }
}

/** One element in a category: the same type and payload a folder entry uses. */
@Entity(
    tableName = "category_members",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId"), Index(value = ["categoryId", "type", "payload"], unique = true)]
)
data class CategoryMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val type: GridItemType,
    val payload: String,
    val orderIndex: Int = 0
)

/** What can be in a category. */
val CategoryMemberTypes = setOf(GridItemType.APP, GridItemType.SHORTCUT, GridItemType.MAP_TARGET, GridItemType.WEBSITE, GridItemType.CONTACT)

/** A category with the elements it holds right now, smart ones worked out. */
data class ResolvedCategory(val category: Category, val members: List<CategoryMember>)

/** How a Categories widget shows: one category ([SINGLE]), several with tabs ([TABS]), or the
 *  categories themselves as small tiles that open the Categories page ([TILES]). */
enum class CategoryWidgetMode { SINGLE, TABS, ROWS, TILES }

/** One Categories widget's own choice. [ids] empty means every category. */
data class CategoryWidgetConfig(
    val mode: CategoryWidgetMode = CategoryWidgetMode.TABS,
    val ids: List<Long> = emptyList(),
    val chips: Boolean = true,
    /** Symbols in front of the names in chips and heads; the tiles show each category's face anyway. */
    val symbols: Boolean = false
) {
    fun toJson(): org.json.JSONObject = org.json.JSONObject()
        .put("mode", mode.name)
        .put("ids", org.json.JSONArray(ids))
        .put("chips", chips)
        .put("symbols", symbols)

    companion object {
        fun fromJson(json: org.json.JSONObject?): CategoryWidgetConfig {
            if (json == null) return CategoryWidgetConfig()
            val ids = json.optJSONArray("ids")?.let { array -> (0 until array.length()).map { array.getLong(it) } }.orEmpty()
            return CategoryWidgetConfig(
                mode = runCatching { CategoryWidgetMode.valueOf(json.optString("mode")) }.getOrDefault(CategoryWidgetMode.TABS),
                ids = ids,
                chips = json.optBoolean("chips", true),
                symbols = json.optBoolean("symbols", false)
            )
        }

        /** The key a widget's choice is kept under: page rows and folder widgets number apart. */
        fun keyFor(row: PageRow): String = if (row.pageId == -1L) "f${row.id}" else "r${row.id}"
    }
}

/** How the category looks as a tile. Categories from before [TileFace] are read from their symbol
 *  ("none" = the first letter) and their color (0 = an outline only). */
fun Category.face(): TileFace = TileFace.fromJson(tileFace) ?: run {
    val outline = color == 0
    when (val key = iconKey) {
        null -> TileFace(surface = if (outline) TileSurface.OUTLINE else TileSurface.AUTO)
        "none" -> TileFace(kind = TileKind.LETTER, surface = if (outline) TileSurface.OUTLINE else TileSurface.AUTO)
        else -> TileFace(
            kind = TileKind.SYMBOL,
            symbol = key,
            surface = when {
                key.startsWith(TileFace.PACK_PREFIX) -> TileSurface.NONE
                outline -> TileSurface.OUTLINE
                else -> TileSurface.AUTO
            }
        )
    }
}

/** How the category's name looks; from before [nameStyle], an outline when it had no color. */
fun Category.nameLook(): CategoryNameStyle =
    nameStyle?.let { runCatching { CategoryNameStyle.valueOf(it) }.getOrNull() }
        ?: if (color == 0) CategoryNameStyle.OUTLINE else CategoryNameStyle.PILL
