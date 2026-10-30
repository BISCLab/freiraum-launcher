package com.bisc.flauncher.data.model

import android.content.Intent
import android.provider.Settings
import com.bisc.flauncher.R

/** A system settings screen the text search can open. [keywords] (German and English, matched
 *  regardless of the app's language) find it by what people look for, not only by its name. */
data class SettingsShortcut(val labelRes: Int, val action: String, val keywords: List<String> = emptyList())

/** Commonly searched system settings screens. Some actions aren't public API or not offered by every
 *  manufacturer — the search only shows those this phone can actually open. */
object SettingsShortcuts {
    val all = listOf(
        SettingsShortcut(R.string.settings_shortcut_wifi, Settings.ACTION_WIFI_SETTINGS,
            listOf("wlan", "wifi", "wi-fi", "internet", "netzwerk", "network")),
        SettingsShortcut(R.string.settings_shortcut_mobile_data, "android.settings.DATA_USAGE_SETTINGS",
            listOf("mobile daten", "mobilfunk", "datenverbrauch", "sim", "roaming", "mobile data", "data usage", "cellular")),
        SettingsShortcut(R.string.settings_shortcut_airplane, Settings.ACTION_AIRPLANE_MODE_SETTINGS,
            listOf("flugmodus", "flugzeugmodus", "airplane", "flight mode")),
        SettingsShortcut(R.string.settings_shortcut_hotspot, "android.settings.TETHER_SETTINGS",
            listOf("hotspot", "tethering", "teilen", "share internet")),
        SettingsShortcut(R.string.settings_shortcut_bluetooth, Settings.ACTION_BLUETOOTH_SETTINGS,
            listOf("bluetooth", "kopfhörer", "kopplung", "headphones", "pairing")),
        SettingsShortcut(R.string.settings_shortcut_nfc, Settings.ACTION_NFC_SETTINGS,
            listOf("nfc", "kontaktlos", "bezahlen", "contactless", "payment")),
        SettingsShortcut(R.string.settings_shortcut_vpn, Settings.ACTION_VPN_SETTINGS, listOf("vpn")),
        SettingsShortcut(R.string.settings_shortcut_cast, Settings.ACTION_CAST_SETTINGS,
            listOf("streamen", "übertragen", "bildschirm übertragen", "cast", "chromecast", "screen mirroring")),
        SettingsShortcut(R.string.settings_shortcut_display, Settings.ACTION_DISPLAY_SETTINGS,
            listOf("bildschirm", "helligkeit", "schriftgröße", "display", "screen", "brightness", "font size")),
        SettingsShortcut(R.string.settings_shortcut_dark_mode, "android.settings.DARK_THEME_SETTINGS",
            listOf("dunkelmodus", "dunkles design", "nachtmodus", "dark mode", "dark theme", "night mode")),
        SettingsShortcut(R.string.settings_shortcut_wallpaper, Intent.ACTION_SET_WALLPAPER,
            listOf("hintergrundbild", "wallpaper", "sperrbildschirm bild", "lock screen wallpaper")),
        SettingsShortcut(R.string.settings_shortcut_sound, Settings.ACTION_SOUND_SETTINGS,
            listOf("ton", "lautstärke", "klingelton", "vibration", "sound", "volume", "ringtone")),
        SettingsShortcut(R.string.settings_shortcut_dnd, Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS,
            listOf("nicht stören", "ruhemodus", "do not disturb", "dnd")),
        SettingsShortcut(R.string.settings_shortcut_notifications, "android.settings.NOTIFICATION_SETTINGS",
            listOf("benachrichtigungen", "mitteilungen", "notifications")),
        SettingsShortcut(R.string.settings_shortcut_battery, Intent.ACTION_POWER_USAGE_SUMMARY,
            listOf("akku", "batterie", "energie", "laden", "battery", "power", "charging")),
        SettingsShortcut(R.string.settings_shortcut_battery_saver, Settings.ACTION_BATTERY_SAVER_SETTINGS,
            listOf("energiesparmodus", "stromsparen", "akku sparen", "battery saver", "power saving")),
        SettingsShortcut(R.string.settings_shortcut_apps, Settings.ACTION_APPLICATION_SETTINGS,
            listOf("apps", "anwendungen", "applications", "deinstallieren", "uninstall")),
        SettingsShortcut(R.string.settings_shortcut_default_apps, Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS,
            listOf("standard-apps", "standardapps", "standard browser", "default apps", "default browser")),
        SettingsShortcut(R.string.settings_shortcut_storage, Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
            listOf("speicher", "speicherplatz", "storage", "space")),
        SettingsShortcut(R.string.settings_shortcut_location, Settings.ACTION_LOCATION_SOURCE_SETTINGS,
            listOf("standort", "ortung", "gps", "location")),
        SettingsShortcut(R.string.settings_shortcut_security, Settings.ACTION_SECURITY_SETTINGS,
            listOf("sicherheit", "sperrbildschirm", "fingerabdruck", "gesichtserkennung", "pin", "passwort",
                "security", "lock screen", "fingerprint", "face unlock", "password")),
        SettingsShortcut(R.string.settings_shortcut_privacy, Settings.ACTION_PRIVACY_SETTINGS,
            listOf("datenschutz", "berechtigungen", "privatsphäre", "privacy", "permissions")),
        SettingsShortcut(R.string.settings_shortcut_accounts, Settings.ACTION_SYNC_SETTINGS,
            listOf("konten", "synchronisierung", "accounts", "sync")),
        SettingsShortcut(R.string.settings_shortcut_accessibility, Settings.ACTION_ACCESSIBILITY_SETTINGS,
            listOf("bedienungshilfen", "barrierefreiheit", "vorlesen", "accessibility", "talkback")),
        SettingsShortcut(R.string.settings_shortcut_language, Settings.ACTION_LOCALE_SETTINGS,
            listOf("sprache", "region", "language", "locale")),
        SettingsShortcut(R.string.settings_shortcut_keyboard, Settings.ACTION_INPUT_METHOD_SETTINGS,
            listOf("tastatur", "eingabe", "rechtschreibung", "keyboard", "input", "spell check")),
        SettingsShortcut(R.string.settings_shortcut_date_time, Settings.ACTION_DATE_SETTINGS,
            listOf("datum", "uhrzeit", "zeitzone", "date", "time", "time zone")),
        SettingsShortcut(R.string.settings_shortcut_system_update, "android.settings.SYSTEM_UPDATE_SETTINGS",
            listOf("systemupdate", "softwareupdate", "aktualisierung", "update", "system update")),
        SettingsShortcut(R.string.settings_shortcut_developer, Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
            listOf("entwickleroptionen", "entwickler", "developer options", "usb debugging")),
        SettingsShortcut(R.string.settings_shortcut_about, Settings.ACTION_DEVICE_INFO_SETTINGS,
            listOf("über das telefon", "geräteinfo", "android version", "modell", "about phone", "device info", "model"))
    )
}
