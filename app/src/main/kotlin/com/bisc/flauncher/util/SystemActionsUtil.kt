package com.bisc.flauncher.util

import android.content.Context

/** Expands the notification shade and quick settings through the status bar service (reflection
 *  on the service object's own class, so it works the same on every API level). */
object SystemActionsUtil {
    fun expandNotifications(context: Context): Boolean = invokeStatusBar(context, "expandNotificationsPanel")

    fun expandQuickSettings(context: Context): Boolean = invokeStatusBar(context, "expandSettingsPanel")

    private fun invokeStatusBar(context: Context, methodName: String): Boolean = runCatching {
        @Suppress("WrongConstant")
        val manager = context.getSystemService(STATUS_BAR_SERVICE) ?: return false
        manager.javaClass.getMethod(methodName).invoke(manager)
        true
    }.getOrDefault(false)

    private const val STATUS_BAR_SERVICE = "statusbar"
}
