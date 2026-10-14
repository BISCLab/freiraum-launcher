package com.bisc.flauncher.data.model

/** What one [PageRow] renders. [PANEL] is the Home page's own pinned Favorites/Panel bookend —
 *  structurally pinned the same way [SEARCH_BAR] (the Dock) is, rendered outside the pager as a
 *  floating overlay rather than inline, opened via its own edge notch. */
enum class PageRowType { WIDGET, SYSTEM_WIDGET, APPS, SEARCH_BAR, PANEL }
