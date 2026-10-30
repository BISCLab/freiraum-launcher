package com.bisc.flauncher.ui.home.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.SearchCategory
import com.bisc.flauncher.ui.home.dock.FreeEdge
import com.bisc.flauncher.ui.home.dock.freeEdgeBorder
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.theme.noRippleClickable

private val FIELD_HEIGHT = 52.dp
private val CATEGORY_ROW_HEIGHT = 48.dp
private val FILTER_CHIP_SIZE = 36.dp

/**
 * Search field + category filter row as one element. At the top it sits flush with the screen's
 * top edge (square top corners, rounded bottom ones, like the Panel); at the bottom ([atBottom])
 * it sits flush on the keyboard, mirrored: categories above the field, [topDecoration] above them.
 * The field itself has the same look as the App grid's search field.
 */
@Composable
fun SearchInputBar(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    appearance: ElementAppearance,
    autoOpen: Boolean,
    categories: List<SearchCategory>,
    categoryColors: Map<SearchCategory, Int?>,
    selectedCategory: SearchCategory?,
    onToggleCategory: (SearchCategory) -> Unit,
    cornerRadiusPercent: Int,
    modifier: Modifier = Modifier,
    /** Space above the content that the bar's own background still fills - lets the bar reach
     *  the screen's top edge (like the Panel) while its content stays below the status bar. */
    topInset: Dp = 0.dp,
    atBottom: Boolean = false,
    showCategories: Boolean = true,
    /** Inside the bar's background below its content, e.g. the navigation bar when at the bottom. */
    bottomInsets: WindowInsets? = null,
    topDecoration: @Composable () -> Unit = {}
) {
    val colors = resolveElementColors(appearance)
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var value by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }

    LaunchedEffect(query) {
        if (query != value.text) value = TextFieldValue(query, TextRange(query.length))
    }
    LaunchedEffect(Unit) {
        if (autoOpen) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    val percent = cornerRadiusPercent.coerceIn(0, 100) / 100f
    val radius = SearchDimens.cornerRadius * percent
    val shape = if (atBottom) RoundedCornerShape(topStart = radius, topEnd = radius)
        else RoundedCornerShape(bottomStart = radius, bottomEnd = radius)

    @Composable
    fun Field(modifier: Modifier) {
        // Same look as the App grid's search field: its automatic box against the bar's own color.
        val fieldColors = resolveElementColors(automaticRailAppearance(LocalContentColor.current.luminance() > 0.5f))
        val content = fieldColors.contentColor
        val fieldShape = RoundedCornerShape(FIELD_HEIGHT / 2 * percent)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(FIELD_HEIGHT)
                .clip(fieldShape)
                .background(fieldColors.background, alpha = fieldColors.backgroundAlpha)
                .border(fieldColors.borderWidth, fieldColors.borderColor, fieldShape)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = content)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.text.isEmpty()) {
                    Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = content.copy(alpha = 0.6f))
                }
                BasicTextField(
                    value = value,
                    onValueChange = {
                        value = it
                        if (it.text != query) onQueryChange(it.text)
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = content),
                    cursorBrush = SolidColor(content),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
            }
            if (value.text.isNotEmpty()) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(20.dp).noRippleClickable { onQueryChange("") }
                )
            }
        }
    }

    @Composable
    fun Categories() {
        Row(
            modifier = Modifier.fillMaxWidth().height(CATEGORY_ROW_HEIGHT).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryFilterRow(
                categories = categories,
                categoryColors = categoryColors,
                selected = selectedCategory,
                onSelect = onToggleCategory
            )
        }
    }

    CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                // Flush with the screen's edges, so the border only traces the free edge - drawn
                // just outside the bar (hence before the clip), so around the rounded corners it
                // runs on into the screen's side edges instead of ending visibly.
                .then(
                    if (appearance.borderEnabled) {
                        Modifier.freeEdgeBorder(colors.borderWidth, colors.borderColor, radius, if (atBottom) FreeEdge.TOP else FreeEdge.BOTTOM)
                    } else Modifier
                )
                .clip(shape)
                .background(colors.background, alpha = colors.backgroundAlpha)
                .padding(top = topInset)
                .then(if (bottomInsets != null) Modifier.windowInsetsPadding(bottomInsets) else Modifier)
        ) {
            if (atBottom) {
                topDecoration()
                if (showCategories) Categories() else Spacer(Modifier.height(12.dp))
                Field(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp))
            } else {
                Field(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = if (showCategories) 0.dp else 12.dp))
                if (showCategories) Categories()
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<SearchCategory>,
    categoryColors: Map<SearchCategory, Int?>,
    selected: SearchCategory?,
    onSelect: (SearchCategory) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // All symbols always fit side by side across the whole bar: the gaps shrink first, the
        // symbols themselves only once there's no gap left.
        val chipSize = if (categories.isEmpty()) FILTER_CHIP_SIZE else minOf(FILTER_CHIP_SIZE, maxWidth / categories.size)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { category ->
                CategoryFilterChip(
                    icon = category.icon,
                    color = categoryColors[category]?.let { Color(it) } ?: category.defaultColor,
                    contentDescription = stringResource(category.filterLabelRes),
                    selected = category == selected,
                    anySelected = selected != null,
                    size = chipSize,
                    onClick = { onSelect(category) }
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterChip(
    icon: ImageVector,
    color: Color,
    contentDescription: String,
    selected: Boolean,
    anySelected: Boolean,
    size: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = if (selected) 0.22f else 0f))
            .noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = color.copy(alpha = if (selected) 1f else if (anySelected) 0.35f else 0.85f),
            modifier = Modifier.size(20.dp)
        )
    }
}
