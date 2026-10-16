package com.bisc.flauncher.data.model

/** How a pinned dock row (see [PageRow.type] == [PageRowType.SEARCH_BAR]) shows/hides itself.
 *  [ALWAYS_VISIBLE] is the original, unconditional behavior. [MANUAL] starts collapsed and opens on
 *  a swipe-up from the home content (see HomeScreen's detectHomeGestures wiring) — no permanent
 *  on-screen affordance, matching the Panel's own gesture-only reveal. [AUTO_EXPAND_ON_ARRIVAL]
 *  starts collapsed and briefly expands itself on arriving at Home instead of needing any gesture.
 *  See HomeScreen's dock panel Box for the actual expand/collapse state machine, which is
 *  intentionally not persisted (ephemeral UI state only, same as the Favorites popup it mirrors
 *  visually). While MANUAL is active, the home screen's own SWIPE_UP gesture slot is reserved for
 *  the Dock and shown locked in SettingsGesturesScreen — it can't also be bound to something else. */
enum class DockVisibilityMode { ALWAYS_VISIBLE, MANUAL, AUTO_EXPAND_ON_ARRIVAL }
