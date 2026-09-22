package com.bisc.flauncher.ui.home.dock

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Pure layout math for [SidebarOverviewPanel]'s content height. */
object SidebarLayout {
    data class Metrics(
        val iconSize: Dp,
        val itemGap: Dp,
        val containerVerticalPadding: Dp
    )

    data class Fit(val height: Dp, val needsScroll: Boolean)

    private val DIVIDER_HEIGHT = 5.dp

    fun fit(itemCount: Int, availableHeight: Dp, metrics: Metrics): Fit {
        val slots = buildList {
            repeat(itemCount) { add(metrics.iconSize) }
            add(DIVIDER_HEIGHT)
            add(metrics.iconSize)
        }
        val gapCount = (slots.size - 1).coerceAtLeast(0)
        val naturalHeight = metrics.containerVerticalPadding * 2 +
            slots.fold(0.dp) { acc, slot -> acc + slot } +
            metrics.itemGap * gapCount
        if (naturalHeight <= availableHeight) {
            return Fit(naturalHeight, needsScroll = false)
        }
        var cumulative = metrics.containerVerticalPadding
        var count = 0
        for (i in slots.indices.reversed()) {
            val extra = slots[i] + (if (count > 0) metrics.itemGap else 0.dp)
            if (cumulative + extra > availableHeight && count > 0) break
            cumulative += extra
            count++
        }
        return Fit(cumulative.coerceAtMost(availableHeight), needsScroll = true)
    }
}
