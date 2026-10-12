package com.bisc.flauncher.data.model

/**
 * A dock power button's destination — opens a specific screen, same tap behavior the old single Hub orb
 * used to dispatch by gesture, now one deliberate button per destination instead. [APP_DRAWER] and
 * [APP_LIST] both open the full installed-apps browse, just forcing its layout (grid vs. list) open —
 * deliberately two separate actions, not one with a layout parameter, so a user can place both as
 * distinct buttons.
 */
enum class DockPowerButtonAction {
    TEXT_SEARCH,
    CATEGORY_SEARCH,
    APP_DRAWER,
    APP_LIST
}
