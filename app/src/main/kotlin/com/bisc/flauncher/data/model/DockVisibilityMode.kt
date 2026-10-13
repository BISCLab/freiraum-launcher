package com.bisc.flauncher.data.model

/** How a pinned dock row (see [PageRow.type] == [PageRowType.SEARCH_BAR]) shows/hides itself.
 *  [ALWAYS_VISIBLE] is the original, unconditional behavior. [GESTURE_TOGGLE] and
 *  [AUTO_EXPAND_ON_ARRIVAL] start collapsed — see HomeScreen's floating dock Box for the actual
 *  expand/collapse state machine, which is intentionally not persisted (ephemeral UI state only,
 *  same as the Favorites popup it mirrors visually). */
enum class DockVisibilityMode { ALWAYS_VISIBLE, GESTURE_TOGGLE, AUTO_EXPAND_ON_ARRIVAL }
