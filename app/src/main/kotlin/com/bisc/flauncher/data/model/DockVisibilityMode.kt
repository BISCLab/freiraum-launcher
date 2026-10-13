package com.bisc.flauncher.data.model

/** How a pinned dock row (see [PageRow.type] == [PageRowType.SEARCH_BAR]) shows/hides itself.
 *  [ALWAYS_VISIBLE] is the original, unconditional behavior. [MANUAL] and [AUTO_EXPAND_ON_ARRIVAL]
 *  start collapsed, showing only the edge notch (see EdgeNotch) — see HomeScreen's dock panel Box
 *  for the actual expand/collapse state machine, which is intentionally not persisted (ephemeral UI
 *  state only, same as the Favorites popup it mirrors visually). Was once GESTURE_TOGGLE (bound to
 *  one of the 4 rebindable HomeGestures) — replaced by a dedicated, always-visible drag notch so
 *  opening/closing these two panels no longer competes with the general gesture system at all. */
enum class DockVisibilityMode { ALWAYS_VISIBLE, MANUAL, AUTO_EXPAND_ON_ARRIVAL }
