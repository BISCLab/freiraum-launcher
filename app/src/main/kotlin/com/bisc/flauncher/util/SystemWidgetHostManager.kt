package com.bisc.flauncher.util

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.res.Configuration
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Hosts third-party app widgets through AppWidgetHost. */
@Singleton
class SystemWidgetHostManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    private val appWidgetHost: AppWidgetHost = AppWidgetHost(context, HOST_ID)

    fun startListening() = appWidgetHost.startListening()
    fun stopListening() = appWidgetHost.stopListening()

    fun allocateId(): Int = appWidgetHost.allocateAppWidgetId()

    fun deleteId(appWidgetId: Int) {
        runCatching { appWidgetHost.deleteAppWidgetId(appWidgetId) }
    }

    fun tryBindWithoutConfirmation(appWidgetId: Int, provider: AppWidgetProviderInfo): Boolean =
        appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider.provider)

    fun getInfo(appWidgetId: Int): AppWidgetProviderInfo? = appWidgetManager.getAppWidgetInfo(appWidgetId)

    fun installedProviders(): List<AppWidgetProviderInfo> = appWidgetManager.installedProviders

    /**
     * [dark] forces the hosted widget's own dark/light resources to follow the launcher's chosen
     * mode for the surface it sits on, instead of the system's separate day/night setting.
     */
    fun createHostView(appWidgetId: Int, info: AppWidgetProviderInfo, dark: Boolean): AppWidgetHostView {
        val nightMode = if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        val configuration = Configuration(context.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
        }
        val themedContext = context.createConfigurationContext(configuration)
        return appWidgetHost.createView(themedContext, appWidgetId, info).apply { setAppWidget(appWidgetId, info) }
    }

    /**
     * Tells both the widget (via options) and the host view its real allotted size. Without this the
     * widget never learns how much room it actually has and can fall back to a default/collapsed
     * layout with nothing to scroll, even though our card is sized generously.
     */
    fun updateHostViewSize(appWidgetId: Int, hostView: AppWidgetHostView, widthDp: Int, heightDp: Int) {
        if (widthDp <= 0 || heightDp <= 0) return
        val options = android.os.Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
        }
        runCatching { appWidgetManager.updateAppWidgetOptions(appWidgetId, options) }
        runCatching { hostView.updateAppWidgetSize(options, widthDp, heightDp, widthDp, heightDp) }
    }

    private companion object {
        const val HOST_ID = 1024
    }
}
