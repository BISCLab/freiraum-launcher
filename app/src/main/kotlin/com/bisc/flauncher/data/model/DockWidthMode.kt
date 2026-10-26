package com.bisc.flauncher.data.model

/** No longer read: a pill row now spans the full row width like every other element (see
 *  [PageRow.edgeToEdge]). Only kept because [PageRow.pillWidthMode] is still a column of the
 *  page_rows table, and dropping a column means rebuilding the whole table in a migration. */
enum class DockWidthMode { FIXED, GROW }
