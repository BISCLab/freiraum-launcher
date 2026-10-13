package com.bisc.flauncher.data.model

/** Home screen gestures that can be assigned an action. */
enum class HomeGesture { LONG_PRESS, DOUBLE_TAP, SWIPE_UP, SWIPE_DOWN }

/** What one [HomeGesture] does. [PAGE] (payload = page id) is the sole way to reference a page —
 *  Search/App overview/Sidebar are all real pages now, so a dedicated action per singleton would
 *  just be a second way to say the same thing. */
enum class GestureActionType {
    NONE, APP, SHORTCUT, PAGE, SETTINGS, NOTIFICATIONS, QUICK_SETTINGS
}

data class GestureAction(val type: GestureActionType = GestureActionType.NONE, val payload: String? = null) {
    companion object {
        val NONE = GestureAction(GestureActionType.NONE)
    }
}
