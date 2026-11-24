package com.bisc.flauncher.util

import androidx.core.net.toUri
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Deliberately honest about what these platforms actually expose: neither WhatsApp nor Telegram
 *  publish an intent to directly start a voice/video call to a number - only a documented chat
 *  deep link. So each app gets one "open chat" action, not fake voice/video buttons. */
object ContactActions {
    fun isPackageInstalled(context: Context, packageName: String): Boolean =
        runCatching { context.packageManager.getPackageInfo(packageName, 0) }.isSuccess

    fun dialIntent(number: String): Intent =
        Intent(Intent.ACTION_DIAL, "tel:${Uri.encode(number)}".toUri())

    fun smsIntent(number: String): Intent =
        Intent(Intent.ACTION_SENDTO, "smsto:${Uri.encode(number)}".toUri())

    fun whatsAppChatIntent(number: String): Intent =
        Intent(Intent.ACTION_VIEW, "https://wa.me/${number.filter { it.isDigit() }}".toUri())

    fun telegramChatIntent(number: String): Intent =
        Intent(Intent.ACTION_VIEW, "https://t.me/+${number.filter { it.isDigit() }}".toUri())

    const val WHATSAPP_PACKAGE = "com.whatsapp"
    const val TELEGRAM_PACKAGE = "org.telegram.messenger"

    fun launch(context: Context, intent: Intent): Boolean =
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
}
