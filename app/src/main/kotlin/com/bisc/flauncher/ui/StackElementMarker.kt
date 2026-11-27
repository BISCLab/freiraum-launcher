package com.bisc.flauncher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

/** Where the parts of a page sit in a settings replica, so their numbers can be put next to them. */
class StackMarkers {
    val bounds = mutableStateMapOf<String, Rect>()
    private val parts = HashMap<String, Pair<Int, LayoutCoordinates>>()
    /** Parts that are switched off: still drawn, but faint. */
    var ghosted by mutableStateOf<Set<String>>(emptySet())

    /** The replica's frame; the bounds are measured from it. Parts may report before it is known. */
    var container: LayoutCoordinates? = null
        set(value) {
            field = value
            parts.keys.toList().forEach(::measure)
        }

    fun report(id: String, rank: Int, coordinates: LayoutCoordinates) {
        val old = parts[id]
        // Of several parts with one id the lowest rank counts, unless it is gone.
        if (old != null && rank > old.first && old.second.isAttached) return
        parts[id] = rank to coordinates
        measure(id)
    }

    /** A part that left the screen (e.g. the button while searching) loses its number. */
    fun forget(id: String, coordinates: LayoutCoordinates) {
        if (parts[id]?.second === coordinates) {
            parts.remove(id)
            bounds.remove(id)
        }
    }

    private fun measure(id: String) {
        val box = container?.takeIf { it.isAttached } ?: return
        val part = parts[id]?.second?.takeIf { it.isAttached } ?: return
        val rect = box.localBoundingBoxOf(part, clipBounds = false)
        if (bounds[id] != rect) bounds[id] = rect
    }
}

val LocalStackMarkers = staticCompositionLocalOf<StackMarkers?> { null }

/** Marks a part of a page for the settings replica. Does nothing on the real screen. Of several
 *  parts with one [id] (the letters), the lowest [rank] is the one that is marked. */
fun Modifier.stackElement(id: String, rank: Int = 0): Modifier = composed {
    val markers = LocalStackMarkers.current
    if (markers == null) Modifier
    else {
        val last = androidx.compose.runtime.remember { arrayOfNulls<LayoutCoordinates>(1) }
        androidx.compose.runtime.DisposableEffect(markers, id) {
            onDispose { last[0]?.let { markers.forget(id, it) } }
        }
        Modifier
            .graphicsLayer { alpha = if (id in markers.ghosted) 0.3f else 1f }
            .onGloballyPositioned { last[0] = it; markers.report(id, rank, it) }
    }
}
