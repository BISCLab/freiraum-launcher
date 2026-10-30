package com.bisc.flauncher.util

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
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}"))

    fun smsIntent(number: String): Intent =
        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}"))

    fun whatsAppChatIntent(number: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${number.filter { it.isDigit() }}"))

    fun telegramChatIntent(number: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+${number.filter { it.isDigit() }}"))

    const val WHATSAPP_PACKAGE = "com.whatsapp"
    const val TELEGRAM_PACKAGE = "org.telegram.messenger"

    fun launch(context: Context, intent: Intent): Boolean =
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
}
