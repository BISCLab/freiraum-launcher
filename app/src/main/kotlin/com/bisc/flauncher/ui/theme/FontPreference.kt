package com.bisc.flauncher.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.bisc.flauncher.R

/** App-wide font choice. */
enum class FontPreference(val fontFamily: FontFamily?) {
    SYSTEM(null),
    SERIF(FontFamily(Font(R.font.pt_serif_regular))),
    MONOSPACE(FontFamily(Font(R.font.roboto_mono))),
    ROUNDED(FontFamily(Font(R.font.comfortaa)))
}
