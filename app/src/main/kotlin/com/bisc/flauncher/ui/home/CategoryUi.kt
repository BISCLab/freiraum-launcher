package com.bisc.flauncher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.data.model.Category
import com.bisc.flauncher.data.model.ResolvedCategory
import com.bisc.flauncher.data.repository.CategoryRepository
import com.bisc.flauncher.ui.forAppName
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** The categories as they are right now, for every place that shows one (cells in the Dock, Panel,
 *  element boxes and folders), and the one way to open the Categories page from anywhere. */
object LauncherCategories {
    var resolved by mutableStateOf<List<ResolvedCategory>>(emptyList())

    /** A design's colors for categories without their own, see [categoryColor]. */
    var palette by mutableStateOf<List<Int>>(emptyList())

    /** Whether the places that show categories by name put the symbol in front (off by default). */
    var symbolsOnPage by mutableStateOf(false)
    var symbolsInSearch by mutableStateOf(false)
    var symbolsInAppList by mutableStateOf(false)

    /** Loads an app's icon, for tiles that show what a category holds. */
    var loadAppIcon: (android.content.ComponentName) -> androidx.compose.ui.graphics.ImageBitmap? = { null }

    /** Loads a picture from an icon pack, for symbols taken from one. */
    var loadPackIcon: (String, String) -> androidx.compose.ui.graphics.ImageBitmap? = { _, _ -> null }

    private val requests = MutableSharedFlow<Long>(extraBufferCapacity = 4)
    val openRequests: SharedFlow<Long> = requests.asSharedFlow()

    /** Opens the Categories page with [categoryId] chosen; [ALL] shows them all. */
    fun open(categoryId: Long) { requests.tryEmit(categoryId) }

    fun find(id: Long?): ResolvedCategory? = resolved.find { it.category.id == id }

    const val ALL = 0L
}

/** Stored as a category's color: no fill, an outline instead. */
const val CATEGORY_NO_COLOR = 0

/** Stored as a category's symbol: no symbol, the tile shows the first letter. */
const val CATEGORY_NO_ICON = "none"

/** How a category looks as a tile (Dock, Panel, element boxes, folders), like a folder's icon:
 *  what it holds in small (no symbol stored), its symbol, or its first letter. */
enum class CategoryFace { CONTENT, SYMBOL, LETTER }

val Category?.face: CategoryFace get() = when (this?.iconKey) {
    null -> CategoryFace.CONTENT
    CATEGORY_NO_ICON -> CategoryFace.LETTER
    else -> CategoryFace.SYMBOL
}

val Category?.hasFill: Boolean get() = this?.color != CATEGORY_NO_COLOR

/** Has a symbol of its own; where a category is shown by name, it is put in front only where that
 *  place asks for it. */
val Category?.hasSymbol: Boolean get() = face == CategoryFace.SYMBOL

/** A symbol from an icon pack is stored as its key with this in front: "pack:<package>|<drawable>". */
const val CATEGORY_PACK_PREFIX = "pack:"

fun categoryPackIconKey(pack: String, drawable: String): String = "$CATEGORY_PACK_PREFIX$pack|$drawable"

/** The icon pack and drawable of a symbol taken from one, else null. */
fun Category?.packIcon(): Pair<String, String>? =
    this?.iconKey?.takeIf { it.startsWith(CATEGORY_PACK_PREFIX) }?.removePrefix(CATEGORY_PACK_PREFIX)
        ?.split('|', limit = 2)?.takeIf { it.size == 2 }?.let { it[0] to it[1] }

/** A category's symbol in [size]: its pictogram in [tint], or the icon pack's picture as it is. */
@Composable
fun CategorySymbol(category: Category?, size: Dp, tint: Color, contentDescription: String? = null) {
    val pack = category.packIcon()
    if (pack != null) {
        val bitmap by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, pack) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { runCatching { LauncherCategories.loadPackIcon(pack.first, pack.second) }.getOrNull() }
        }
        val image = bitmap
        if (image != null) androidx.compose.foundation.Image(image, contentDescription = contentDescription, modifier = Modifier.size(size))
        else Spacer(Modifier.size(size))
    } else {
        Icon(categoryIcon(category), contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size))
    }
}

/** A category's own symbol: its pictogram, the smart rule's, or a plain label. */
fun categoryIcon(category: Category?): ImageVector =
    PictogramCatalog.iconFor(category?.iconKey)
        ?: category?.smartRule?.let { PictogramCatalog.iconFor(CategoryRepository.ruleIconKey(it)) }
        ?: Icons.AutoMirrored.Filled.Label

/** A category's color; without one of its own, the design's color for its place, else the accent.
 *  A category set to no color takes the color of the text around it. */
@Composable
fun categoryColor(category: Category?): Color {
    if (!category.hasFill) return LocalContentColor.current
    category?.color?.let { return Color(it) }
    val palette = LauncherCategories.palette
    if (category != null && palette.isNotEmpty()) return Color(palette[category.orderIndex.mod(palette.size)])
    return MaterialTheme.colorScheme.primary
}

/** Black or white on [background], whichever reads better. */
fun inkOn(background: Color): Color = if (background.luminance() > 0.5f) Color.Black else Color.White

/** A category as an element: its symbol on its color, the name below. */
@Composable
internal fun CategoryCell(
    category: Category?,
    shape: Shape,
    onClick: () -> Unit,
    showLabel: Boolean = true,
    labelColor: Color = Color.White,
    iconSizeScale: Float = 1f
) {
    Column(
        modifier = Modifier
            .clickable(enabled = category != null, onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CategoryMark(category, iconSize = 30.dp * iconSizeScale, modifier = Modifier.size(56.dp * iconSizeScale), shape = shape, ink = labelColor)
        if (showLabel) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = category?.name.orEmpty(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(color = labelColor).forAppName()
            )
        }
    }
}

/** The same in the Dock's flush squares, without a caption of its own. */
@Composable
internal fun CategoryPillIcon(category: Category?, modifier: Modifier = Modifier) {
    CategoryMark(
        category,
        iconSize = 24.dp,
        modifier = modifier.clickable(enabled = category != null) { category?.let { LauncherCategories.open(it.id) } },
        shape = RectangleShape,
        contentDescription = category?.name
    )
}

/** A category as a tile: what it holds, its symbol or its first letter, on its color or in an
 *  outline without one. */
@Composable
fun CategoryMark(
    category: Category?,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    ink: Color = LocalContentColor.current,
    contentDescription: String? = null
) {
    val filled = category.hasFill
    val accent = categoryColor(category)
    val tint = if (filled) inkOn(accent) else ink
    Box(
        modifier = modifier.clip(shape).then(if (filled) Modifier.background(accent) else Modifier.border(1.5.dp, ink.copy(alpha = 0.6f), shape)),
        contentAlignment = Alignment.Center
    ) {
        val members = if (category.face == CategoryFace.CONTENT) LauncherCategories.find(category?.id)?.members.orEmpty().take(4) else emptyList()
        when {
            category.face == CategoryFace.SYMBOL ->
                // A picture from an icon pack fills more of the tile, like an app icon.
                CategorySymbol(category, if (category.packIcon() != null) iconSize * 1.4f else iconSize, tint, contentDescription)
            members.isNotEmpty() -> CategoryContentGrid(members, cell = iconSize * 0.7f, gap = iconSize * 0.1f, tint = tint)
            else -> Text(
                category?.name?.trim()?.take(1)?.uppercase().orEmpty(),
                color = tint,
                fontSize = (iconSize.value * 0.8f).sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/** What a category holds, up to four, in two by two: app icons, and symbols for the other kinds. */
@Composable
private fun CategoryContentGrid(members: List<com.bisc.flauncher.data.model.CategoryMember>, cell: Dp, gap: Dp, tint: Color) {
    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(gap)) {
        members.chunked(2).forEach { line ->
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(gap)) {
                line.forEach { member ->
                    Box(modifier = Modifier.size(cell), contentAlignment = Alignment.Center) {
                        if (member.type == com.bisc.flauncher.data.model.GridItemType.APP) {
                            val component = androidx.compose.runtime.remember(member.payload) { android.content.ComponentName.unflattenFromString(member.payload) }
                            rememberAppIcon(component, LauncherCategories.loadAppIcon)?.let {
                                androidx.compose.foundation.Image(it, contentDescription = null, modifier = Modifier.size(cell))
                            }
                        } else {
                            Icon(
                                when (member.type) {
                                    com.bisc.flauncher.data.model.GridItemType.WEBSITE -> Icons.Filled.Language
                                    com.bisc.flauncher.data.model.GridItemType.MAP_TARGET -> Icons.Filled.Place
                                    com.bisc.flauncher.data.model.GridItemType.CONTACT -> Icons.Filled.Person
                                    else -> Icons.AutoMirrored.Filled.Shortcut
                                },
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(cell * 0.8f)
                            )
                        }
                    }
                }
                if (line.size < 2) Spacer(Modifier.size(cell))
            }
        }
    }
}

/** A category's name in a pill of its color, outlined without a color; its symbol in front only
 *  where [showSymbol] asks for it and it has one. */
@Composable
fun CategoryNamePill(
    category: Category,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    ink: Color = LocalContentColor.current,
    iconSize: Dp = 18.dp,
    showSymbol: Boolean = false
) {
    val filled = category.hasFill
    val accent = categoryColor(category)
    val tint = if (filled) inkOn(accent) else ink
    Row(
        modifier = modifier
            .clip(CircleShape)
            .then(if (filled) Modifier.background(accent) else Modifier.border(1.5.dp, ink.copy(alpha = 0.6f), CircleShape))
            .padding(horizontal = PILL_PADDING, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showSymbol && category.hasSymbol) {
            CategorySymbol(category, iconSize, tint)
            Spacer(Modifier.size(8.dp))
        }
        Text(category.name, style = textStyle, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Inner start padding of [CategoryNamePill]; pull it back by this to line its content up. */
val PILL_PADDING = 10.dp
