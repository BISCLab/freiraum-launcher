package com.bisc.flauncher.data.model

/** Fill type of a layer 2/3 surface: no fill, or a color or gradient. */
enum class ElementFillMode { NONE, COLOR }

/** Everything needed to resolve and render one Layer-2/3 surface's own look. */
data class ElementAppearance(
    val fillMode: ElementFillMode = ElementFillMode.COLOR,
    val fill: ColorFill = ColorFill(),
    val opacity: Float = 1f,
    val borderEnabled: Boolean = false,
    val borderColor: Int? = null,
    val borderWidth: Float = 1f,
    val contentColor: Int? = null
)
