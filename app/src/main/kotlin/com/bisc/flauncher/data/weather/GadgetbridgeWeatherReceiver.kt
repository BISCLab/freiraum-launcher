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
                val text = GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader(Charsets.UTF_8).readText()
                JSONArray(text).optJSONObject(0)
            }.getOrNull()?.let { return it }
        }
        intent.getStringExtra(EXTRA_WEATHER_JSON)?.let { raw ->
            runCatching { JSONObject(raw) }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun parseWeatherSpec(json: JSONObject, sourceApp: String): WeatherSnapshot? =
        WeatherSnapshotJson.parseBroadcast(json, sourceApp)

    private companion object {
        const val TAG = "GbWeatherReceiver"
        const val ACTION_GENERIC_WEATHER = "nodomain.freeyourgadget.gadgetbridge.ACTION_GENERIC_WEATHER"
        const val EXTRA_WEATHER_JSON = "WeatherJson"
        const val EXTRA_WEATHER_GZ = "WeatherGz"
    }
}
