package com.bisc.flauncher.ui.home.search

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputMethodRequest

/** Whether the keyboard may learn from what is typed into the launcher's search fields. Set from
 *  the settings in MainActivity. */
object KeyboardPrivacy {
    var noLearning by mutableStateOf(false)
}

/** Around a search field: while [KeyboardPrivacy.noLearning] is on, the keyboard is asked not to
 *  remember what is typed there (no learned words, no suggestions from it later). */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NoLearningKeyboard(content: @Composable () -> Unit) {
    if (!KeyboardPrivacy.noLearning) {
        content()
        return
    }
    InterceptPlatformTextInput(
        interceptor = { request, nextHandler ->
            val private = object : PlatformTextInputMethodRequest {
                override fun createInputConnection(outAttributes: EditorInfo): InputConnection {
                    val connection = request.createInputConnection(outAttributes)
                    outAttributes.imeOptions = outAttributes.imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
                    return connection
                }
            }
            nextHandler.startInputMethod(private)
        },
        content = content
    )
}
