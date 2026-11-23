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

    private val requests = MutableSharedFlow<Long>(extraBufferCapacity = 4)
    val openRequests: SharedFlow<Long> = requests.asSharedFlow()

    /** Opens the Categories page with [categoryId] chosen; [ALL] shows them all. */
    fun open(categoryId: Long) { requests.tryEmit(categoryId) }

    fun find(id: Long?): ResolvedCategory? = resolved.find { it.category.id == id }

    const val ALL = 0L
}

/** Stored as a category's color: no fill, an outline instead. */
const val CATEGORY_NO_COLOR = 0

/** Stored as a category's symbol: none, the name alone. */
const val CATEGORY_NO_ICON = "none"

val Category?.hasFill: Boolean get() = this?.color != CATEGORY_NO_COLOR
val Category?.hasSymbol: Boolean get() = this?.iconKey != CATEGORY_NO_ICON

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

/** A category's sign: its symbol (or first letter) on its color, or in an outline without one. */
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
        if (category.hasSymbol) {
            Icon(categoryIcon(category), contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
        } else {
            Text(
                category?.name?.trim()?.take(1)?.uppercase().orEmpty(),
                color = tint,
                fontSize = (iconSize.value * 0.8f).sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/** A category's name in a pill of its color, its symbol in front; outlined without a color. */
@Composable
fun CategoryNamePill(
    category: Category,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    ink: Color = LocalContentColor.current,
    iconSize: Dp = 18.dp
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
        if (category.hasSymbol) {
            Icon(categoryIcon(category), contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
            Spacer(Modifier.size(8.dp))
        }
        Text(category.name, style = textStyle, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Inner start padding of [CategoryNamePill]; pull it back by this to line its content up. */
val PILL_PADDING = 10.dp
