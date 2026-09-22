package com.bisc.flauncher.util

import android.accessibilityservice.AccessibilityService
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/** Opt-in accessibility service that performs the global actions lock screen, recent apps and power menu. */
class GlobalActionsAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = WeakReference(this)
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    companion object {
        private var instance: WeakReference<GlobalActionsAccessibilityService>? = null

        fun performGlobalActionOrFalse(action: Int): Boolean =
            runCatching { instance?.get()?.performGlobalAction(action) }.getOrNull() ?: false

        fun isEnabledInSystemSettings(context: android.content.Context): Boolean {
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?: return false
            val flatComponentName = "${context.packageName}/${GlobalActionsAccessibilityService::class.java.name}"
            return enabled.splitToSequence(':').any { it.equals(flatComponentName, ignoreCase = true) }
        }
    }
}
