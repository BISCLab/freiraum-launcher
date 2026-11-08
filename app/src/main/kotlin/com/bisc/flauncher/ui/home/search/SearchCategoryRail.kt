package com.bisc.flauncher.ui.home.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.SearchCategory
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.theme.designGlow
import kotlin.math.roundToInt

/** The rail is as slim as the App list's A-Z rail. */
val SEARCH_RAIL_WIDTH = 28.dp
/** Between the rail and the result cards. */
val SEARCH_RAIL_GAP = 10.dp
private val RAIL_SLOT = 40.dp
private val BUBBLE_SIZE = 56.dp

/**
 * The categories of the Text search as a vertical rail beside the results, like the App list's
 * A-Z rail. A tap jumps to that category; dragging along the rail runs through the categories and
 * shows the one under the finger in a bubble. Categories without results right now are faint.
 * [focus] is the one tapped last; the others are greyed out until the results are scrolled.
 */
@Composable
fun SearchCategoryRail(
    categories: List<SearchCategory>,
    categoryColors: Map<SearchCategory, Int?>,
    present: Set<SearchCategory>,
    focus: SearchCategory?,
    left: Boolean,
    appearance: ElementAppearance,
    cornerRadiusPercent: Int,
    onTap: (SearchCategory) -> Unit,
    onScrub: (SearchCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) return
    val colors = resolveElementColors(appearance)
    val radius = SEARCH_RAIL_WIDTH / 2 * (cornerRadiusPercent.coerceIn(0, 100) / 100f)
    val shape = RoundedCornerShape(radius)
    fun colorOf(category: SearchCategory) = categoryColors[category]?.let { Color(it) } ?: category.defaultColor
    var scrubbing by remember { mutableStateOf<SearchCategory?>(null) }
    var scrubY by remember { mutableStateOf(0f) }

    BoxWithConstraints(modifier = modifier.fillMaxHeight().width(SEARCH_RAIL_WIDTH), contentAlignment = Alignment.Center) {
        val railHeight = minOf(maxHeight, RAIL_SLOT * categories.size)
        val railTopPx = with(LocalDensity.current) { ((maxHeight - railHeight) / 2).toPx() }
        Column(
            modifier = Modifier
                .width(SEARCH_RAIL_WIDTH)
                .heightIn(max = railHeight)
                .fillMaxHeight()
                .designGlow(shape, "rail")
                .clip(shape)
                .background(colors.background, alpha = colors.backgroundAlpha)
                .then(if (appearance.borderEnabled) Modifier.border(colors.borderWidth, colors.borderColor, shape) else Modifier)
                .pointerInput(categories) {
                    fun at(y: Float) = categories[((y / size.height) * categories.size).toInt().coerceIn(0, categories.lastIndex)]
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        var moved = false
                        var last = at(down.position.y)
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            if (!moved && kotlin.math.abs(change.position.y - down.position.y) > viewConfiguration.touchSlop) moved = true
                            if (moved) {
                                val now = at(change.position.y)
                                scrubY = railTopPx + change.position.y.coerceIn(0f, size.height.toFloat())
                                if (now != last || scrubbing == null) {
                                    last = now
                                    scrubbing = now
                                    onScrub(now)
                                }
                            }
                        }
                        if (!moved) onTap(last)
                        scrubbing = null
                    }
                },
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            categories.forEach { category ->
                val color = colorOf(category)
                val chosen = category == focus
                val faded = (focus != null && !chosen) || category !in present
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = if (chosen) 0.22f else 0f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        category.icon,
                        contentDescription = stringResource(category.filterLabelRes),
                        tint = color,
                        modifier = Modifier.size(17.dp).alpha(if (faded) 0.3f else 0.9f)
                    )
                }
            }
        }
        // While dragging, the category under the finger sits in a bubble beside the rail.
        scrubbing?.let { category ->
            val bubblePx = with(LocalDensity.current) { BUBBLE_SIZE.toPx() }
            val gapPx = with(LocalDensity.current) { (SEARCH_RAIL_WIDTH + 12.dp).toPx() }
            Box(
                modifier = Modifier
                    .align(if (left) Alignment.TopStart else Alignment.TopEnd)
                    .offset { IntOffset(if (left) gapPx.roundToInt() else -gapPx.roundToInt(), (scrubY - bubblePx / 2).roundToInt()) }
                    .size(BUBBLE_SIZE)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(category.icon, contentDescription = null, tint = colorOf(category), modifier = Modifier.size(28.dp))
            }
        }
    }
}
