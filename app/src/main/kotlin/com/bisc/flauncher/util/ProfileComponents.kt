package com.bisc.flauncher.util

import android.content.ComponentName
import android.content.Context
import android.os.UserHandle
import android.os.UserManager

/**
 * Apps of the work profile and the private space carry their profile in their component: the
 * class name gets "#u<serial>" (the profile's serial number, which stays the same across reboots).
 * So they are stored, listed, searched and set up like every other app, and only starting them,
 * their icon and their menu ask the system for their profile. Apps of the own profile have no mark;
 * everything saved before stays as it is.
 */
object ProfileComponents {
    private const val MARK = "#u"

    fun mark(component: ComponentName, serial: Long): ComponentName =
        ComponentName(component.packageName, component.className + MARK + serial)

    fun serialOf(component: ComponentName): Long? {
        val at = component.className.lastIndexOf(MARK)
        return if (at < 0) null else component.className.substring(at + MARK.length).toLongOrNull()
    }

    fun isProfile(component: ComponentName): Boolean = serialOf(component) != null

    /** The component as the system knows it, without the mark. */
    fun real(component: ComponentName): ComponentName {
        val at = component.className.lastIndexOf(MARK)
        return if (at < 0 || serialOf(component) == null) component else ComponentName(component.packageName, component.className.substring(0, at))
    }

    fun userOf(context: Context, component: ComponentName): UserHandle? {
        val serial = serialOf(component) ?: return null
        return runCatching { context.getSystemService(UserManager::class.java).getUserForSerialNumber(serial) }.getOrNull()
    }

    fun serialOf(context: Context, user: UserHandle): Long =
        context.getSystemService(UserManager::class.java).getSerialNumberForUser(user)

    /** Only the apps of the own profile - where something of theirs (their shortcuts) is meant. */
    fun ownOnly(apps: List<com.bisc.flauncher.data.model.AppInfo>): List<com.bisc.flauncher.data.model.AppInfo> =
        apps.filter { !isProfile(it.componentName) }
}
