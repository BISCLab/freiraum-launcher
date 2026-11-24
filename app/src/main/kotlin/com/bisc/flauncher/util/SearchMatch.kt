package com.bisc.flauncher.util

import java.text.Normalizer

private val COMBINING_MARKS = Regex("\\p{Mn}+")
private val WORD_SEPARATORS = Regex("[\\s,]+")

/** Lowercase, without accents and with ß as ss - so "uber" finds "Über" and "strasse" finds "Straße". */
fun foldForSearch(text: String): String =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(COMBINING_MARKS, "").replace("ß", "ss")

/** How well one word of a query matches a text, best first. */
private const val AT_WORD_START = 3
private const val INSIDE_WORD = 2
private const val WITH_TYPO = 1
private const val NONE = 0

/** Shortest query word that is also found inside a word ("ram" in "Instagram"), and that may hold a
 *  typo. Shorter ones would match nearly everything. */
private const val MIN_INSIDE = 3
private const val MIN_TYPO = 4

private fun words(folded: String): List<String> = folded.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }

/** Edits between [a] and [b] (insert, delete, change, swap of two neighbours), counted up to [limit]. */
private fun editDistance(a: String, b: String, limit: Int): Int {
    if (kotlin.math.abs(a.length - b.length) > limit) return limit + 1
    val rows = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) rows[i][0] = i
    for (j in 0..b.length) rows[0][j] = j
    for (i in 1..a.length) {
        var best = Int.MAX_VALUE
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            var d = minOf(rows[i - 1][j] + 1, rows[i][j - 1] + 1, rows[i - 1][j - 1] + cost)
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) d = minOf(d, rows[i - 2][j - 2] + 1)
            rows[i][j] = d
            if (d < best) best = d
        }
        if (best > limit) return limit + 1
    }
    return rows[a.length][b.length]
}

private fun wordQuality(foldedText: String, textWords: List<String>, q: String): Int {
    for (i in foldedText.indices) {
        if ((i == 0 || !foldedText[i - 1].isLetterOrDigit()) && foldedText.startsWith(q, i)) return AT_WORD_START
    }
    if (q.length >= MIN_INSIDE && foldedText.contains(q)) return INSIDE_WORD
    // A slip of the finger: the start of a word that differs by one edit (two for long words).
    // Numbers stay exact - a house number or a code with a typo is another one.
    if (q.length >= MIN_TYPO && q.none { it.isDigit() }) {
        val limit = if (q.length >= 7) 2 else 1
        for (w in textWords) {
            for (len in (q.length - limit)..(q.length + limit)) {
                if (len < 1 || len > w.length) continue
                if (editDistance(q, w.substring(0, len), limit) <= limit) return WITH_TYPO
            }
        }
    }
    return NONE
}

/**
 * How well [query] (one or several words, in any order) matches [text]: 0 for no match, higher is
 * better. Every word of the query has to be found: best at the start of a word, else inside a word
 * (from 3 letters on), else at the start of a word with a small typo (from 4 letters on). The
 * weakest word decides.
 */
fun searchScore(text: String, query: String): Int {
    val queryWords = query.split(WORD_SEPARATORS).map { foldForSearch(it) }.filter { it.isNotEmpty() }
    if (queryWords.isEmpty()) return NONE
    val folded = foldForSearch(text)
    val textWords = words(folded)
    var weakest = AT_WORD_START
    for (q in queryWords) {
        val quality = wordQuality(folded, textWords, q)
        if (quality == NONE) return NONE
        if (quality < weakest) weakest = quality
    }
    return weakest
}

/** Search matching shared by every text-search category: [query] is found in [text] - at the start
 *  of a word, inside one, or with a small typo (see [searchScore]). */
fun matchesWordStart(text: String, query: String): Boolean = searchScore(text, query) > NONE

/** Like [matchesWordStart] for queries of several words, in any order: every word of [query] has to
 *  be found somewhere in [text] - so "klee 23" finds "Kleeberger Straße 23, 35510 Butzbach". */
fun matchesAllWordStarts(text: String, query: String): Boolean = searchScore(text, query) > NONE

/** Whether [query] (one or several words) matches [label] or any of [keywords]: every word has to
 *  be found in the label, or in one single keyword. */
fun matchesLabelOrKeywords(label: String, keywords: List<String>, query: String): Boolean =
    matchesAllWordStarts(label, query) || keywords.any { matchesAllWordStarts(it, query) }

/** The best [searchScore] of [label] and [keywords], for putting the closest matches first. */
fun labelOrKeywordsScore(label: String, keywords: List<String>, query: String): Int =
    maxOf(searchScore(label, query), keywords.maxOfOrNull { searchScore(it, query) } ?: NONE)
