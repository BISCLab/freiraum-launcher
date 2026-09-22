package com.bisc.flauncher.data.model

import android.provider.Settings
import com.bisc.flauncher.R

data class SettingsShortcut(val labelRes: Int, val action: String)

/** A small curated set of commonly-searched system settings screens. */
object SettingsShortcuts {
    val all = listOf(
        SettingsShortcut(R.string.settings_shortcut_wifi, Settings.ACTION_WIFI_SETTINGS),
        SettingsShortcut(R.string.settings_shortcut_bluetooth, Settings.ACTION_BLUETOOTH_SETTINGS),
        SettingsShortcut(R.string.settings_shortcut_display, Settings.ACTION_DISPLAY_SETTINGS),
        SettingsShortcut(R.string.settings_shortcut_sound, Settings.ACTION_SOUND_SETTINGS),
        SettingsShortcut(R.string.settings_shortcut_apps, Settings.ACTION_APPLICATION_SETTINGS),
        SettingsShortcut(R.string.settings_shortcut_storage, Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
        SettingsShortcut(R.string.settings_shortcut_location, Settings.ACTION_LOCATION_SOURCE_SETTINGS),
        SettingsShortcut(R.string.settings_shortcut_security, Settings.ACTION_SECURITY_SETTINGS),
        SettingsShortcut(R.string.settings_shortcut_accessibility, Settings.ACTION_ACCESSIBILITY_SETTINGS),
        SettingsShortcut(R.string.settings_shortcut_date_time, Settings.ACTION_DATE_SETTINGS),
        SettingsShortcut(R.string.settings_shortcut_about, Settings.ACTION_DEVICE_INFO_SETTINGS)
    )
}
