package com.bisc.flauncher.ui.settings

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R

/** The lock for the settings: while [enabled], they open only after the device's own unlock
 *  (fingerprint, face, PIN, pattern or password). Once unlocked they stay open until the launcher
 *  goes back home or has been away for a while. */
object SettingsLock {
    var enabled by mutableStateOf(false)
    var unlocked by mutableStateOf(false)

    private var stoppedAt = 0L

    /** The launcher left the screen (another app, the screen off). */
    fun onStop() { stoppedAt = System.currentTimeMillis() }

    /** Back on the screen: after more than a minute away the settings lock again. A short trip, like
     *  picking a picture in the system's picker, keeps them open. */
    fun onStart() {
        if (stoppedAt != 0L && System.currentTimeMillis() - stoppedAt > RELOCK_AFTER_MS) unlocked = false
    }

    /** Whether the device has a screen lock at all; without one there is nothing to ask for. */
    fun deviceSecure(context: Context): Boolean =
        context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

    private const val RELOCK_AFTER_MS = 60_000L
}

/** In front of every settings page while they are locked: asks for the unlock right away and offers
 *  to ask again. */
@Composable
internal fun SettingsLockGate() {
    val context = LocalContext.current
    val activity = context.findActivity()
    // Android 8 and 9: the system's own screen lock question.
    val confirm = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) SettingsLock.unlocked = true
    }
    val title = stringResource(R.string.settings_lock_prompt_title)
    val ask = {
        if (!SettingsLock.deviceSecure(context)) {
            // The screen lock was removed meanwhile: nothing to ask for.
            SettingsLock.unlocked = true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && activity != null) {
            askBiometric(activity, title)
        } else {
            context.getSystemService(KeyguardManager::class.java)
                ?.createConfirmDeviceCredentialIntent(title, null)
                ?.let { confirm.launch(it) }
                ?: run { SettingsLock.unlocked = true }
        }
    }
    LaunchedEffect(Unit) { ask() }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Text(stringResource(R.string.settings_lock_locked), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text(stringResource(R.string.settings_lock_locked_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Button(onClick = ask) { Text(stringResource(R.string.settings_lock_unlock)) }
            }
        }
    }
}

@androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
private fun askBiometric(activity: Activity, title: String) {
    val builder = BiometricPrompt.Builder(activity).setTitle(title)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
    } else {
        @Suppress("DEPRECATION")
        builder.setDeviceCredentialAllowed(true)
    }
    runCatching {
        builder.build().authenticate(CancellationSignal(), activity.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                SettingsLock.unlocked = true
            }
        })
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
