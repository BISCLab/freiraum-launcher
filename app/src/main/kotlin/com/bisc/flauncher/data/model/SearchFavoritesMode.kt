package com.bisc.flauncher.data.model

/** Where the search favorites come from: only picked by hand, picked ones topped up with the most
 *  opened apps, or only the most opened apps. */
enum class SearchFavoritesMode { MANUAL, BOTH, AUTO }

/** How many favorites the search shows at most (two rows of four). */
const val SEARCH_FAVORITES_MAX = 8
