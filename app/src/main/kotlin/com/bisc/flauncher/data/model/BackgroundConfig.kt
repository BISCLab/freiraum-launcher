package com.bisc.flauncher.data.model

/** What gets drawn as the base fill of a real page's own backdrop. */
enum class BackgroundSource {
    IMAGE,
    COLOR
}

/** One real page's backdrop configuration. */
data class BackgroundConfig(
    val source: BackgroundSource = BackgroundSource.COLOR,
    val fill: ColorFill = ColorFill(),
    val imagePath: String? = null,
    val opacity: Float = 1f,
    val darken: Float = 0f
)
