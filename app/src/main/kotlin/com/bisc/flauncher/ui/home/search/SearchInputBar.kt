package com.bisc.flauncher.ui.home.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
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

private val ROW_HEIGHT = 56.dp
private val FILTER_CHIP_SIZE = 36.dp

/**
 * Search field + category filter row as one element: a single rounded frame, two internally
 * equal-height rows separated by a thin divider - the input field (with the Home button) on top,
 * the category filter chips below.
 *
 * Sits flush at the very top of the search screen (no outer margin), the same
 * "permanently-shown Panel" treatment as the home screen's own Panel: square top
 * corners (flush with the screen/cutout-safe edge above it), bottom corners scaled by
 * [cornerRadiusPercent] the same way Dock/Panel scale theirs.
 */
@Composable
fun SearchInputBar(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onHome: () -> Unit,
    appearance: ElementAppearance,
    autoOpen: Boolean,
    categories: List<SearchCategory>,
    categoryColors: Map<SearchCategory, Int?>,
    selectedCategory: SearchCategory?,
    onToggleCategory: (SearchCategory) -> Unit,
    cornerRadiusPercent: Int,
    modifier: Modifier = Modifier,
    /** Space above the input row that the bar's own background still fills - lets the bar reach
     *  the screen's top edge (like the Panel) while its content stays below the status bar. */
    topInset: Dp = 0.dp,
    /** The thin line above the input row. */
    showTopLine: Boolean = true,
    /** The thin line between the input row and the categories. */
    showMiddleLine: Boolean = true
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

    val radius = SearchDimens.cornerRadius * (cornerRadiusPercent.coerceIn(0, 100) / 100f)
    val shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = radius, bottomEnd = radius)
    CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                // Flush with the screen's top and side edges, so the border only traces the free
                // bottom edge - drawn just outside the bar (hence before the clip), so around the
                // rounded corners it runs on into the screen's side edges instead of ending visibly.
                .then(if (appearance.borderEnabled) Modifier.freeEdgeBorder(colors.borderWidth, colors.borderColor, radius, FreeEdge.BOTTOM) else Modifier)
                .clip(shape)
                .background(colors.background, alpha = colors.backgroundAlpha)
                .padding(top = topInset)
        ) {
                // Same divider as the one between the field and the categories, so the field row
                // sits framed between two lines.
                if (showTopLine) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.dividerColor.copy(alpha = 0.15f)))
                Row(
                    modifier = Modifier.fillMaxWidth().height(ROW_HEIGHT).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(LocalContentColor.current.copy(alpha = 0.08f))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.text.isEmpty()) {
                            Text(placeholder, color = LocalContentColor.current.copy(alpha = 0.6f))
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = {
                                value = it
                                if (it.text != query) onQueryChange(it.text)
                            },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current),
                            cursorBrush = SolidColor(LocalContentColor.current),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                        )
                    }
                    IconButton(onClick = onHome) {
                        Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.search_action_home))
                    }
                }
                if (showMiddleLine) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.dividerColor.copy(alpha = 0.15f)))
                Row(
                    modifier = Modifier.fillMaxWidth().height(ROW_HEIGHT).padding(horizontal = 8.dp),
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
