package com.bisc.flauncher.data.model

/** A text-search hit among the app shortcuts: [shortcut] to launch, and the name of the app it belongs to. */
data class ShortcutResult(val shortcut: PinnedShortcut, val appLabel: String)
