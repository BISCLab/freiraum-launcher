package com.bisc.flauncher.data.model

/** Home screen gestures that can be assigned an action. */
enum class HomeGesture { LONG_PRESS, DOUBLE_TAP, SWIPE_UP, SWIPE_DOWN }

/** What one [HomeGesture] does. */
enum class GestureActionType {
    NONE, APP, SHORTCUT, PAGE, SEARCH, APP_LIST, SETTINGS, NOTIFICATIONS, QUICK_SETTINGS
}

data class GestureAction(val type: GestureActionType = GestureActionType.NONE, val payload: String? = null) {
    companion object {
        val NONE = GestureAction(GestureActionType.NONE)
    }
}
