package com.bisc.flauncher.ui.home.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.ui.home.dock.resolveElementColors

/** Search field for the system keyboard. With [autoOpen], it takes focus and opens the keyboard right
 *  away; otherwise it waits for the user to tap it (the field's own default tap-to-focus handles that,
 *  no extra wiring needed). */
@Composable
fun SystemSearchField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onHome: () -> Unit,
    appearance: ElementAppearance,
    bottomMargin: Dp,
    autoOpen: Boolean = true,
    modifier: Modifier = Modifier
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = SearchDimens.horizontalMargin, end = SearchDimens.horizontalMargin, bottom = bottomMargin),
        contentAlignment = Alignment.Center
    ) {
        val d = searchDimens(maxWidth)
        val shape = RoundedCornerShape(d.cornerRadius)
        CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
            Row(
                modifier = Modifier
                    .width(d.pillWidth)
                    .clip(shape)
                    .background(colors.background, alpha = colors.backgroundAlpha)
                    .then(if (appearance.borderEnabled) Modifier.border(colors.borderWidth, colors.borderColor, shape) else Modifier)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
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
        }
    }
}
