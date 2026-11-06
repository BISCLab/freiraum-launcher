package com.bisc.flauncher.data.model

/** The symbol on the paging buttons. */
enum class WidgetArrowStyle { CHEVRON, TRIANGLE, ARROW }

/** The look of the paging buttons; whether a widget uses them is its own choice
 *  (PageRow.pagingButtons). [appearance] null = automatic, a light tint of the widget's own text
 *  color; [alphaPercent] fades the whole button, which stays tappable even at 0. */
data class WidgetPaging(
    val arrowStyle: WidgetArrowStyle = WidgetArrowStyle.CHEVRON,
    val alphaPercent: Int = 100,
    val appearance: ElementAppearance? = null,
    val cornerRadiusPercent: Int = 40
)
