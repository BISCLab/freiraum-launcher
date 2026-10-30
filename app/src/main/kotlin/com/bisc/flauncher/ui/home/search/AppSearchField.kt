package com.bisc.flauncher.ui.home.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.ui.home.dock.resolveElementColors

/** The app-only search field of the App grid and the app list. With [onOpenTextSearch] set it is
 *  just a button into Text search. */
@Composable
fun AppSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    appearance: ElementAppearance,
    cornerRadiusPercent: Int,
    autoOpen: Boolean,
    modifier: Modifier = Modifier,
    onOpenTextSearch: (() -> Unit)? = null
) {
    val colors = resolveElementColors(appearance)
    val shape = RoundedCornerShape(FIELD_HEIGHT / 2 * (cornerRadiusPercent.coerceIn(0, 100) / 100f))
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        if (autoOpen && onOpenTextSearch == null) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.contentColor)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FIELD_HEIGHT)
            .clip(shape)
            .background(colors.background, alpha = colors.backgroundAlpha)
            .then(if (appearance.borderEnabled) Modifier.border(colors.borderWidth, colors.borderColor, shape) else Modifier)
            .then(if (onOpenTextSearch != null) Modifier.clickable(onClick = onOpenTextSearch) else Modifier),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = colors.contentColor)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(stringResource(R.string.app_search_hint), style = textStyle, color = colors.contentColor.copy(alpha = 0.6f))
                }
                if (onOpenTextSearch == null) {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = textStyle,
                        cursorBrush = SolidColor(colors.contentColor),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                    )
                }
            }
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = null,
                    tint = colors.contentColor,
                    modifier = Modifier.size(20.dp).clickable { onQueryChange("") }
                )
            }
        }
    }
}

private val FIELD_HEIGHT = 52.dp
