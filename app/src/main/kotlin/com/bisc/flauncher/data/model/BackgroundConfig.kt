package com.bisc.flauncher.data.model

/** What gets drawn as the base fill of a real page's own backdrop. */
enum class BackgroundSource {
    IMAGE,
    COLOR
}

/** One real page's backdrop configuration. [shade] tints the fill/image toward black (negative) or
 *  white (positive); 0 is neutral. Replaces a separate "opacity" field, which faded the fill toward
 *  nothing with no real backdrop to reveal underneath it. */
data class BackgroundConfig(
    val source: BackgroundSource = BackgroundSource.COLOR,
    val fill: ColorFill = ColorFill(),
    val imagePath: String? = null,
    val shade: Float = 0f
)
