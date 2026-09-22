package com.bisc.flauncher.ui.home.search

import com.bisc.flauncher.data.model.AppInfo

/** One row of the browse (blank-query) app list. */
sealed class BrowseItem {
    data class Header(val letter: Char) : BrowseItem()
    data class Row(val app: AppInfo) : BrowseItem()
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
        fun from(apps: List<AppInfo>): BrowseGrouping {
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
                items += BrowseItem.Header(letter)
                letterApps.forEach { items += BrowseItem.Row(it) }
            }
            if (other.isNotEmpty()) {
                headerIndex['#'] = items.size
                items += BrowseItem.Header('#')
                other.forEach { items += BrowseItem.Row(it) }
            }
            return BrowseGrouping(byLetter.keys.toList(), other.isNotEmpty(), items, headerIndex)
        }
    }
}
