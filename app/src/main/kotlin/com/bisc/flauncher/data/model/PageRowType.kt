package com.bisc.flauncher.data.model

/** What one [PageRow] renders. [PANEL] is the Home page's own pinned Favorites/Panel bookend —
 *  structurally pinned the same way [SEARCH_BAR] (the Dock) is, rendered outside the pager as a
 *  floating overlay rather than inline, opened via a swipe gesture. */
enum class PageRowType { WIDGET, SYSTEM_WIDGET, ELEMENT_BOX, SEARCH_BAR, PANEL }
