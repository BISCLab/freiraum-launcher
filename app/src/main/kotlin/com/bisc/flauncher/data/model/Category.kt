package com.bisc.flauncher.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A smart category fills itself; it stores only its rule, the members are worked out live. */
enum class CategoryRule { NEW, UNUSED, FREQUENT, UNCATEGORIZED }

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
    val rule: String? = null
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
