package com.bisc.flauncher.ui.home.search

import com.bisc.flauncher.data.model.AppInfo

/** One row of the browse (blank-query) app list. */
sealed class BrowseItem {
    data class Header(val letter: Char) : BrowseItem()
    data class Row(val app: AppInfo) : BrowseItem()
    /** Several apps side by side, on wide screens; the first line of a letter carries it. */
    data class Line(val letter: Char?, val apps: List<AppInfo>) : BrowseItem()
}

class BrowseGrouping private constructor(
    val letters: List<Char>,
    val hasOther: Boolean,
    val items: List<BrowseItem>,
    private val headerIndex: Map<Char, Int>
) {
    fun indexOf(letter: Char): Int? = headerIndex[letter]

    fun nearestIndexOf(letter: Char): Int? {
        indexOf(letter)?.let { return it }
        val after = letters.filter { it > letter }.minOrNull()
        val before = letters.filter { it < letter }.maxOrNull()
        return (after ?: before)?.let { headerIndex[it] }
    }

    companion object {
        fun from(apps: List<AppInfo>, columns: Int = 1): BrowseGrouping {
            // Wide screens: the letter stands beside its first line instead of above it.
            fun MutableList<BrowseItem>.addSection(letter: Char, list: List<AppInfo>) {
                if (columns <= 1) {
                    add(BrowseItem.Header(letter))
                    list.forEach { add(BrowseItem.Row(it)) }
                } else list.chunked(columns).forEachIndexed { i, line -> add(BrowseItem.Line(if (i == 0) letter else null, line)) }
            }
            val byLetter = linkedMapOf<Char, MutableList<AppInfo>>()
            val other = mutableListOf<AppInfo>()
            for (app in apps) {
                val c = app.label.firstOrNull()?.uppercaseChar()
                if (c != null && c in 'A'..'Z') byLetter.getOrPut(c) { mutableListOf() }.add(app)
                else other.add(app)
            }
            val items = mutableListOf<BrowseItem>()
            val headerIndex = mutableMapOf<Char, Int>()
            for ((letter, letterApps) in byLetter) {
                headerIndex[letter] = items.size
                items.addSection(letter, letterApps)
            }
            if (other.isNotEmpty()) {
                headerIndex['#'] = items.size
                items.addSection('#', other)
            }
            return BrowseGrouping(byLetter.keys.toList(), other.isNotEmpty(), items, headerIndex)
        }
    }
}
