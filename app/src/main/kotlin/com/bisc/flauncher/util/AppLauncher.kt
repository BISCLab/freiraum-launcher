package com.bisc.flauncher.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent

fun launchApp(context: Context, componentName: ComponentName) {
    // An app of the work profile or the private space starts in its profile; while that profile is
    // paused or locked, the system is asked to switch it on (it may ask for the screen lock).
    ProfileComponents.userOf(context, componentName)?.let { user ->
        val userManager = context.getSystemService(android.os.UserManager::class.java)
        runCatching {
            if (userManager.isQuietModeEnabled(user)) userManager.requestQuietModeEnabled(false, user)
            else context.getSystemService(android.content.pm.LauncherApps::class.java)
                .startMainActivity(ProfileComponents.real(componentName), user, null, launchZoomOptions(context))
        }
        return
    }
    // ACTION_MAIN + CATEGORY_LAUNCHER, not just a bare component, matches what
    // PackageManager.getLaunchIntentForPackage() (what "Open" in App info uses) sends. Some apps'
    // launch activities check for this and silently do nothing for a component-only Intent.
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = componentName
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            launchZoomOptions(context)
        )
    }
}

/** The opening app grows out of the spot that was just tapped (the icon), like on iOS. Null when
 *  animations are off or there's no screen to start from. */
private fun launchZoomOptions(context: Context): android.os.Bundle? {
    val origin = com.bisc.flauncher.ui.TouchOrigin
    if (!origin.launchZoom || origin.x < 0f) return null
    val view = (context as? android.app.Activity)?.window?.decorView ?: return null
    val size = (56 * context.resources.displayMetrics.density).toInt()
    return android.app.ActivityOptions.makeScaleUpAnimation(
        view, origin.x.toInt() - size / 2, origin.y.toInt() - size / 2, size, size
    ).toBundle()
}
