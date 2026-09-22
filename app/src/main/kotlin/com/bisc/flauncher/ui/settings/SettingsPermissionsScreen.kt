package com.bisc.flauncher.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.util.DefaultLauncherUtil
import com.bisc.flauncher.util.GlobalActionsAccessibilityService

/** Status and grant actions for all permissions and system settings the launcher uses. */
@Composable
fun SettingsPermissionsScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refreshTick++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isDefault = refreshTick.let { DefaultLauncherUtil.isDefaultLauncher(context) }
    val contacts = refreshTick.let { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED }
    val notifications = refreshTick.let { NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName) }
    val accessibility = refreshTick.let { GlobalActionsAccessibilityService.isEnabledInSystemSettings(context) }
    val shortcuts = refreshTick.let { viewModel.hasShortcutHostPermission() }

    val openAppDetails = {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { refreshTick++ }
    val requestDefault = {
        val intent = DefaultLauncherUtil.createRoleRequestIntent(context) ?: DefaultLauncherUtil.fallbackSettingsIntent()
        roleLauncher.launch(intent)
    }
    var pendingPermission by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refreshTick++
        val permission = pendingPermission
        val activity = context as? Activity
        // Once Android stops showing the dialog (denied twice), a request silently does nothing.
        if (!granted && permission != null && activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            openAppDetails()
        }
    }
    val requestPermission = { permission: String ->
        pendingPermission = permission
        permissionLauncher.launch(permission)
    }

    SettingsSubScreen(title = stringResource(R.string.settings_section_permissions), navController = navController, onBack = onBack) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_permissions_launcher_group)) {
            PermissionRow(
                title = stringResource(R.string.settings_section_default_launcher),
                purpose = stringResource(R.string.settings_default_launcher_description),
                granted = isDefault,
                onClick = { if (!isDefault) requestDefault() }
            )
            PermissionRow(
                title = stringResource(R.string.settings_permissions_shortcuts),
                purpose = stringResource(R.string.settings_permissions_shortcuts_purpose),
                granted = shortcuts,
                onClick = { if (!shortcuts) requestDefault() }
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_permissions_system_group)) {
            val touchFeedback = refreshTick.let { isTouchFeedbackOn(context) }
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_permissions_touch_feedback),
                summary = stringResource(if (touchFeedback) R.string.settings_permissions_on else R.string.settings_permissions_off),
                info = stringResource(R.string.settings_permissions_touch_feedback_purpose),
                onClick = { openSoundSettings(context) },
                leading = {
                    Icon(
                        imageVector = if (touchFeedback) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = if (touchFeedback) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                },
                trailing = null
            )
        }
        SettingsPreferenceGroup(title = stringResource(R.string.settings_permissions_access_group)) {
            PermissionRow(
                title = stringResource(R.string.settings_permissions_contacts),
                purpose = stringResource(R.string.settings_permissions_contacts_purpose),
                granted = contacts,
                onClick = { if (contacts) openAppDetails() else requestPermission(Manifest.permission.READ_CONTACTS) }
            )
            PermissionRow(
                title = stringResource(R.string.settings_permissions_notifications),
                purpose = stringResource(R.string.settings_permissions_notifications_purpose),
                granted = notifications,
                onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            )
            PermissionRow(
                title = stringResource(R.string.settings_permissions_accessibility),
                purpose = stringResource(R.string.settings_permissions_accessibility_purpose),
                granted = accessibility,
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            )
        }
    }
}

@Composable
private fun PermissionRow(title: String, purpose: String, granted: Boolean, onClick: () -> Unit) {
    val status = stringResource(if (granted) R.string.settings_permissions_granted else R.string.settings_permissions_not_granted)
    SettingsPreference(
        icon = null,
        title = title,
        summary = status,
        info = purpose,
        onClick = onClick,
        leading = {
            Icon(
                imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                contentDescription = status,
                tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        },
        trailing = null
    )
}
