package com.bisc.flauncher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The accessibility mode: every element solid with a strong border, larger icons, no blur.
 *  Set from the settings in MainActivity; read wherever elements are drawn. */
object AccessibilityMode {
    var on by mutableStateOf(false)

    /** How much larger app icons are while it is on. */
    const val ICON_SCALE = 1.25f
}
