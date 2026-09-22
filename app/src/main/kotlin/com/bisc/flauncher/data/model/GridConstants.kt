package com.bisc.flauncher.data.model

/** Bounds for [PageRow.columns] on an [PageRowType.APPS] row. */
object GridConstants {
    const val DEFAULT_APPS_ROW_COLUMNS = 5
    const val MIN_APPS_ROW_COLUMNS = 2
    const val MAX_APPS_ROW_COLUMNS = 8
}

/** Identifiers for built-in (non-system-AppWidget) widgets rendered by the launcher itself. */
object WidgetKind {
    const val CLOCK = "clock"
    const val MUSIC = "music"
    const val WEATHER = "weather"
    const val NOTES = "notes"

    const val SPACER = "spacer"

    fun defaultSpan(kind: String): Int = when (kind) {
        CLOCK -> 2
        MUSIC -> 2
        WEATHER -> 3
        NOTES -> 2
        SPACER -> 1
        else -> 1
    }

    fun minSpan(kind: String): Int = when (kind) {
        CLOCK -> 1
        MUSIC -> 1
        WEATHER -> 1
        NOTES -> 1
        SPACER -> 1
        else -> 1
    }

    fun maxSpan(kind: String): Int = when (kind) {
        CLOCK -> 4
        MUSIC -> 3
        WEATHER -> 3
        NOTES -> 4
        SPACER -> 10
        else -> 5
    }
}

/** The clock widget's own visual layout. */
object ClockStyle {
    const val DIGITAL = "digital"

    const val DIGITAL_LARGE = "digital_large"

    const val STACKED = "stacked"

    const val ANALOG = "analog"

    const val ORBIT = "orbit"

    const val BINARY = "binary"

    val entries = listOf(DIGITAL, DIGITAL_LARGE, STACKED, ANALOG, ORBIT, BINARY)
}

/** Horizontal alignment of every clock style's content (time, date, alarm line, faces). */
object ClockAlignment {
    const val CENTER = "center"
    const val START = "start"
    const val END = "end"

    val entries = listOf(CENTER, START, END)
}

/** Which hour format [com.bisc.flauncher.ui.home.widgets.ClockWidget] renders. */
object ClockTimeFormat {
    const val SYSTEM = "system"

    const val H24 = "24h"
    const val H12 = "12h"

    val entries = listOf(SYSTEM, H24, H12)
}
