package com.bisc.flauncher.util

import android.app.StatusBarManager
import android.content.Context

/** Expands the notification shade and quick settings through StatusBarManager (reflection). */
object SystemActionsUtil {
    fun expandNotifications(context: Context): Boolean = runCatching {
        val manager = context.getSystemService(Context.STATUS_BAR_SERVICE) ?: return false
        val method = StatusBarManager::class.java.getMethod("expandNotificationsPanel")
        method.invoke(manager)
        true
    }.getOrDefault(false)

    fun expandQuickSettings(context: Context): Boolean = runCatching {
        val manager = context.getSystemService(Context.STATUS_BAR_SERVICE) ?: return false
        val method = StatusBarManager::class.java.getMethod("expandSettingsPanel")
        method.invoke(manager)
        true
    }.getOrDefault(false)
}
