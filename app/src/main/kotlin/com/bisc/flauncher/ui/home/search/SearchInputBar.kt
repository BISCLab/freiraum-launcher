package com.bisc.flauncher.ui.home.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.ui.home.dock.resolveElementColors
import com.bisc.flauncher.ui.theme.designGlow
import com.bisc.flauncher.ui.theme.noRippleClickable

private val FIELD_HEIGHT = 52.dp

/**
 * The Text search's input field, sitting on the page itself near the top, or on the keyboard
 * ([atBottom]). The categories are not part of it any more; they run down the rail beside the
 * results (see [SearchCategoryRail]).
 */
@Composable
fun SearchInputBar(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    autoOpen: Boolean,
    /** Roundness when the field has none of its own. */
    cornerRadiusPercent: Int,
    modifier: Modifier = Modifier,
    /** Space kept above the field, e.g. below the status bar or the camera. */
    topInset: Dp = 0.dp,
    atBottom: Boolean = false,
    /** The field's own look; null = automatic against the page. */
    fieldAppearance: ElementAppearance? = null,
    fieldCornerRadiusPercent: Int? = null,
    /** Kept below the field, e.g. the navigation bar when it sits at the bottom. */
    bottomInsets: WindowInsets? = null,
    /** Distance to the screen's side edges. */
    sideMargin: Dp = 20.dp
) {
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

    // Until customized, white and round like the App grid's search field.
    val ownAppearance = fieldAppearance ?: automaticRailAppearance(false)
    val fieldColors = resolveElementColors(ownAppearance)
    val content = fieldColors.contentColor
    val fieldShape = com.bisc.flauncher.ui.theme.PartCornerShape(fieldCornerRadiusPercent ?: cornerRadiusPercent)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topInset)
            .then(if (bottomInsets != null) Modifier.windowInsetsPadding(bottomInsets) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .padding(start = sideMargin, end = sideMargin, top = if (atBottom) 0.dp else 8.dp, bottom = if (atBottom) 16.dp else 0.dp)
                .fillMaxWidth()
                .height(FIELD_HEIGHT)
                .designGlow(fieldShape, "search")
                .clip(fieldShape)
                .background(fieldColors.background, alpha = fieldColors.backgroundAlpha)
                .then(if (fieldColors.showBorder) Modifier.border(fieldColors.borderWidth, fieldColors.borderColor, fieldShape) else Modifier)
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
}
