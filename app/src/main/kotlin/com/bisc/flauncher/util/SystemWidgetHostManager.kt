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
        reusableViews.remove(appWidgetId)
        latestViews.remove(appWidgetId)
        runCatching { appWidgetHost.deleteAppWidgetId(appWidgetId) }
    }

    // The home screen's own host views, one per widget id and light/dark mode: a page leaving and
    // re-entering the pager's composed range (neighbors are pre-composed while swiping) reuses its
    // view instead of inflating the whole widget again - that inflation, mid-swipe, is what stutters.
    private val reusableViews = HashMap<Int, Pair<Boolean, AppWidgetHostView>>()

    // The host only delivers a widget's updates to the view created last for its id, so a kept home
    // view is only reusable while no other view (e.g. a settings preview) has been created since.
    private val latestViews = HashMap<Int, AppWidgetHostView>()

    /** Like [createHostView], but hands back the home screen's existing view for [appWidgetId] (detached
     *  from wherever it was shown before) when one exists for the same [dark] mode. */
    fun obtainHomeHostView(appWidgetId: Int, info: AppWidgetProviderInfo, dark: Boolean): AppWidgetHostView {
        reusableViews[appWidgetId]?.let { (cachedDark, view) ->
            if (cachedDark == dark && latestViews[appWidgetId] === view) {
                (view.parent as? android.view.ViewGroup)?.removeView(view)
                return view
            }
        }
        return createHostView(appWidgetId, info, dark).also { reusableViews[appWidgetId] = dark to it }
    }

    fun tryBindWithoutConfirmation(appWidgetId: Int, provider: AppWidgetProviderInfo): Boolean =
        appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider.provider)

    fun getInfo(appWidgetId: Int): AppWidgetProviderInfo? = appWidgetManager.getAppWidgetInfo(appWidgetId)

    fun installedProviders(): List<AppWidgetProviderInfo> = appWidgetManager.installedProviders

    /**
     * [dark] forces the hosted widget's own dark/light resources to follow the launcher's chosen mode
     * for the surface it sits on, instead of the system's separate day/night setting. A themed
     * [Configuration] context is what actually makes this work broadly - [AppWidgetHostView]'s own
     * [AppWidgetHostView.setOnLightBackground] (also set below, API 29+) sounds like it should be
     * enough and doesn't require recreating the view on a change, but in practice most third-party
     * widgets simply don't check it and keep rendering with whatever their default theme is; only
     * widgets built specifically for Material You dynamic color tend to honor it. Recreating on every
     * [dark] change (via `key(dark)` at the call site) is the reliable option, even though it costs a
     * brief blank/white flash right when it happens.
     */
    fun createHostView(appWidgetId: Int, info: AppWidgetProviderInfo, dark: Boolean): AppWidgetHostView =
        newHostView(appWidgetId, info, dark).also { latestViews[appWidgetId] = it }

    private fun newHostView(appWidgetId: Int, info: AppWidgetProviderInfo, dark: Boolean): AppWidgetHostView {
        val nightMode = if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        val configuration = Configuration(context.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
        }
        val themedContext = context.createConfigurationContext(configuration)
        return appWidgetHost.createView(themedContext, appWidgetId, info).apply {
            setAppWidget(appWidgetId, info)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) setOnLightBackground(!dark)
        }
    }

    /**
     * Tells both the widget (via options) and the host view its real allotted size. Without this the
     * widget never learns how much room it actually has and can fall back to a default/collapsed
     * layout with nothing to scroll, even though our card is sized generously.
     */
    // Last size told to each host view: telling a widget its unchanged size again makes many of them
    // rebuild and re-send their whole content, which is costly every time a page comes back into view.
    private val lastSizes = java.util.WeakHashMap<AppWidgetHostView, Pair<Int, Int>>()

    fun updateHostViewSize(appWidgetId: Int, hostView: AppWidgetHostView, widthDp: Int, heightDp: Int) {
        if (widthDp <= 0 || heightDp <= 0) return
        if (lastSizes[hostView] == (widthDp to heightDp)) return
        lastSizes[hostView] = widthDp to heightDp
        val options = android.os.Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
        }
        runCatching { appWidgetManager.updateAppWidgetOptions(appWidgetId, options) }
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                hostView.updateAppWidgetSize(options, listOf(android.util.SizeF(widthDp.toFloat(), heightDp.toFloat())))
            } else {
                @Suppress("DEPRECATION")
                hostView.updateAppWidgetSize(options, widthDp, heightDp, widthDp, heightDp)
            }
        }
    }

    private companion object {
        const val HOST_ID = 1024
    }
}
