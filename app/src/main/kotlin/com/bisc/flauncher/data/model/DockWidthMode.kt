package com.bisc.flauncher.data.model

/** Not read: a pill row spans the full row width like every other element (see [PageRow.edgeToEdge]).
 *  Kept because [PageRow.pillWidthMode] is a column of the page_rows table, and dropping a column
 *  means rebuilding the whole table in a migration. */
enum class DockWidthMode { FIXED, GROW }
