package com.bisc.flauncher.data.weather

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bisc.flauncher.data.model.WeatherSnapshot
import com.bisc.flauncher.data.repository.WeatherRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import javax.inject.Inject

/** Receives weather broadcasts in the Gadgetbridge format and stores the snapshot. */
@AndroidEntryPoint
class GadgetbridgeWeatherReceiver : BroadcastReceiver() {
    @Inject lateinit var weatherRepository: WeatherRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_GENERIC_WEATHER) return
        val json = extractWeatherJson(intent) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = parseWeatherSpec(json, sourceApp = "")
                if (snapshot != null) {
                    weatherRepository.update(snapshot)
                    Log.d(TAG, "Weather snapshot updated from broadcast.")
                } else {
                    Log.w(TAG, "Received weather broadcast but couldn't parse its payload.")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun extractWeatherJson(intent: Intent): JSONObject? {
        intent.getByteArrayExtra(EXTRA_WEATHER_GZ)?.let { bytes ->
            runCatching {
                val text = readCapped(GZIPInputStream(ByteArrayInputStream(bytes))) ?: return@runCatching null
                JSONArray(text).optJSONObject(0)
            }.getOrNull()?.let { return it }
        }
        intent.getStringExtra(EXTRA_WEATHER_JSON)?.takeIf { it.length <= MAX_WEATHER_BYTES }?.let { raw ->
            runCatching { JSONObject(raw) }.getOrNull()?.let { return it }
        }
        return null
    }

    /** Unpacks at most [MAX_WEATHER_BYTES]; any app may send this broadcast, so a packed payload
     *  that blows up beyond that is dropped instead of filling the memory. */
    private fun readCapped(input: java.io.InputStream): String? = input.use {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val n = it.read(buffer)
            if (n < 0) break
            if (out.size() + n > MAX_WEATHER_BYTES) return null
            out.write(buffer, 0, n)
        }
        out.toString(Charsets.UTF_8.name())
    }

    private fun parseWeatherSpec(json: JSONObject, sourceApp: String): WeatherSnapshot? =
        WeatherSnapshotJson.parseBroadcast(json, sourceApp)

    private companion object {
        const val TAG = "GbWeatherReceiver"
        const val ACTION_GENERIC_WEATHER = "nodomain.freeyourgadget.gadgetbridge.ACTION_GENERIC_WEATHER"
        const val EXTRA_WEATHER_JSON = "WeatherJson"
        const val EXTRA_WEATHER_GZ = "WeatherGz"
        const val MAX_WEATHER_BYTES = 1024 * 1024
    }
}
