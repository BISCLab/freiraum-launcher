package com.bisc.flauncher.data.repository

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** An app of another profile on the phone: the work profile or the private space. */
data class ProfileApp(val componentName: ComponentName, val label: String, val user: UserHandle)

enum class ProfileKind { WORK, PRIVATE }

/** One profile with its apps. [locked]: paused (work) or locked (private space); its apps then
 *  do not start until it is switched on again. */
data class ProfileSection(val kind: ProfileKind, val user: UserHandle, val locked: Boolean, val apps: List<ProfileApp>)

/**
 * The apps of the work profile and the private space, through [LauncherApps] like every launcher.
 * The launcher's own profile is not part of this; its apps come from [InstalledAppsRepository].
 */
@Singleton
class ProfileAppsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val icons = ConcurrentHashMap<Pair<ComponentName, UserHandle>, ImageBitmap>()

    /** Every other profile with its apps, again whenever apps or a profile's state change. */
    fun sections(): Flow<List<ProfileSection>> = changes().onStart { emit(Unit) }.map { load() }.flowOn(Dispatchers.Default)

    private fun changes(): Flow<Unit> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) { trySend(Unit) }
            override fun onPackageAdded(packageName: String, user: UserHandle) { trySend(Unit) }
            override fun onPackageChanged(packageName: String, user: UserHandle) { trySend(Unit) }
            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) { trySend(Unit) }
            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) { trySend(Unit) }
        }
        launcherApps.registerCallback(callback, android.os.Handler(android.os.Looper.getMainLooper()))
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) { icons.clear(); trySend(Unit) }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
            if (Build.VERSION.SDK_INT >= 35) {
                addAction(Intent.ACTION_PROFILE_AVAILABLE)
                addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
                addAction(Intent.ACTION_PROFILE_ADDED)
                addAction(Intent.ACTION_PROFILE_REMOVED)
            }
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        awaitClose {
            launcherApps.unregisterCallback(callback)
            context.unregisterReceiver(receiver)
        }
    }

    private fun kindOf(user: UserHandle): ProfileKind {
        if (Build.VERSION.SDK_INT >= 35) {
            val type = runCatching { launcherApps.getLauncherUserInfo(user)?.userType }.getOrNull()
            if (type == UserManager.USER_TYPE_PROFILE_PRIVATE) return ProfileKind.PRIVATE
        }
        return ProfileKind.WORK
    }

    private fun load(): List<ProfileSection> {
        val own = Process.myUserHandle()
        val profiles = runCatching { launcherApps.profiles }.getOrDefault(emptyList()).filter { it != own }
        return profiles.mapNotNull { user ->
            val locked = runCatching { userManager.isQuietModeEnabled(user) }.getOrDefault(false)
            val apps = if (locked) emptyList() else runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList())
                .map { ProfileApp(it.componentName, it.label.toString(), user) }
                .sortedBy { it.label.lowercase() }
            ProfileSection(kindOf(user), user, locked, apps)
        }.sortedBy { it.kind.ordinal }
    }

    /** The app's icon with the profile's badge, as the system shows it. */
    fun icon(app: ProfileApp): ImageBitmap? = icons[app.componentName to app.user] ?: runCatching {
        val info = launcherApps.resolveActivity(Intent(Intent.ACTION_MAIN).setComponent(app.componentName), app.user) ?: return null
        info.getBadgedIcon(0).toBitmap(ICON_PX, ICON_PX).asImageBitmap().also { icons[app.componentName to app.user] = it }
    }.getOrNull()

    fun launch(app: ProfileApp, bounds: Rect? = null) {
        runCatching { launcherApps.startMainActivity(app.componentName, app.user, bounds, null) }
    }

    /** Pauses or locks the profile ([locked] = true), or switches it on again. The system may ask
     *  for the screen lock first. */
    fun setLocked(user: UserHandle, locked: Boolean) {
        runCatching { userManager.requestQuietModeEnabled(locked, user) }
    }

    private companion object {
        const val ICON_PX = 128
    }
}
