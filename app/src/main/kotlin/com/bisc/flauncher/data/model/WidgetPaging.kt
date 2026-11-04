package com.bisc.flauncher.data.model

/** How widgets with more than one page (weather hours and days, notes) are paged. */
enum class WidgetPagingMode { GESTURE, BUTTONS }

/** The symbol on the paging buttons. */
enum class WidgetArrowStyle { CHEVRON, TRIANGLE, ARROW }

/** Everything the paging buttons need. [appearance] null = automatic, a light tint of the widget's
 *  own text color; [alphaPercent] fades the whole button, which stays tappable even at 0. */
data class WidgetPaging(
    val mode: WidgetPagingMode = WidgetPagingMode.GESTURE,
    val arrowStyle: WidgetArrowStyle = WidgetArrowStyle.CHEVRON,
    val alphaPercent: Int = 100,
    val appearance: ElementAppearance? = null,
    val cornerRadiusPercent: Int = 40
)
