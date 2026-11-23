package com.bisc.flauncher.ui.home

import androidx.compose.foundation.background
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

/** A category's own symbol: its pictogram, the smart rule's, or a plain label. */
fun categoryIcon(category: Category?): ImageVector =
    PictogramCatalog.iconFor(category?.iconKey)
        ?: category?.smartRule?.let { PictogramCatalog.iconFor(CategoryRepository.ruleIconKey(it)) }
        ?: Icons.AutoMirrored.Filled.Label

/** A category's color; without one of its own, the design's color for its place, else the accent. */
@Composable
fun categoryColor(category: Category?): Color {
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
    val background = categoryColor(category)
    Column(
        modifier = Modifier
            .clickable(enabled = category != null, onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(56.dp * iconSizeScale).clip(shape).background(background),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = categoryIcon(category), contentDescription = null, tint = inkOn(background), modifier = Modifier.size(30.dp * iconSizeScale))
        }
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
    val background = categoryColor(category)
    Box(
        modifier = modifier.background(background).clickable(enabled = category != null) { category?.let { LauncherCategories.open(it.id) } },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = categoryIcon(category), contentDescription = category?.name, tint = inkOn(background))
    }
}
