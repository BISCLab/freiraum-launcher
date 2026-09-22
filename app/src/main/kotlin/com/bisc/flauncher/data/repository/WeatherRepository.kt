package com.bisc.flauncher.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bisc.flauncher.data.model.WeatherSnapshot
import com.bisc.flauncher.data.weather.WeatherSnapshotJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Persists the most recent weather snapshot. */
class WeatherRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val snapshot: Flow<WeatherSnapshot?> = dataStore.data.map { prefs ->
        prefs[SNAPSHOT_JSON]?.let { WeatherSnapshotJson.fromStorage(it) }?.let { return@map it }
        val location = prefs[LOCATION] ?: return@map null
        WeatherSnapshot(
            location = location,
            currentTempC = prefs[CURRENT_TEMP] ?: return@map null,
            condition = prefs[CONDITION].orEmpty(),
            todayMinC = prefs[MIN_TEMP] ?: return@map null,
            todayMaxC = prefs[MAX_TEMP] ?: return@map null,
            updatedAtMillis = prefs[UPDATED_AT] ?: 0L,
            sourceApp = prefs[SOURCE_APP].orEmpty()
        )
    }

    suspend fun update(snapshot: WeatherSnapshot) {
        dataStore.edit { prefs ->
            prefs[SNAPSHOT_JSON] = WeatherSnapshotJson.toStorage(snapshot)
        }
    }

    private companion object {
        val SNAPSHOT_JSON = stringPreferencesKey("weather_snapshot_json")
        val LOCATION = stringPreferencesKey("weather_location")
        val CURRENT_TEMP = doublePreferencesKey("weather_current_temp")
        val CONDITION = stringPreferencesKey("weather_condition")
        val MIN_TEMP = doublePreferencesKey("weather_min_temp")
        val MAX_TEMP = doublePreferencesKey("weather_max_temp")
        val UPDATED_AT = longPreferencesKey("weather_updated_at")
        val SOURCE_APP = stringPreferencesKey("weather_source_app")
    }
}
