package com.bisc.flauncher.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

/**
 * A design or wallpaper pack handed to the launcher from outside: a downloaded file opened with
 * Freiraum, or an "open in Freiraum" link on the extensions page (a small design travels inside the
 * link itself, since the launcher has no internet access to fetch anything). The settings page for
 * that kind picks it up, installs it and shows its preview.
 */
object IncomingExtension {
    enum class Kind { DESIGN, PACK }
    data class Pending(val uri: Uri, val kind: Kind)

    val pending = MutableStateFlow<Pending?>(null)

    private const val LINK_SCHEME = "freiraum"
    private const val LINK_HOST_DESIGN = "design"
    // A design is a few kilobytes; nothing bigger travels in a link.
    private const val MAX_LINK_BYTES = 300 * 1024

    /** Whether [intent] hands over a file or link at all (cheap, before any reading). */
    fun isCandidate(intent: Intent?): Boolean = intent?.action == Intent.ACTION_VIEW && intent.data != null

    /** Reads what [intent] hands over; null if it is neither a design nor a pack. */
    suspend fun read(context: Context, intent: Intent): Pending? = withContext(Dispatchers.IO) {
        val data = intent.data ?: return@withContext null
        runCatching {
            val uri = if (data.scheme == LINK_SCHEME) {
                if (data.host != LINK_HOST_DESIGN) return@runCatching null
                val encoded = data.encodedFragment ?: return@runCatching null
                if (encoded.length > MAX_LINK_BYTES * 2) return@runCatching null
                val bytes = Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP)
                if (bytes.size > MAX_LINK_BYTES) return@runCatching null
                val file = File(File(context.cacheDir, "incoming").apply { mkdirs() }, "link.freiraum-design")
                file.writeBytes(bytes)
                Uri.fromFile(file)
            } else data
            kindOf(context, uri)?.let { Pending(uri, it) }
        }.getOrNull()
    }

    /** By what is inside, not by the file's name: downloads often arrive without one. */
    private fun kindOf(context: Context, uri: Uri): Kind? =
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var checked = 0
                while (checked++ < 64) {
                    val entry = zip.nextEntry ?: break
                    when (entry.name) {
                        "design.json" -> return@use Kind.DESIGN
                        "pack.json" -> return@use Kind.PACK
                    }
                }
                null
            }
        }
}
