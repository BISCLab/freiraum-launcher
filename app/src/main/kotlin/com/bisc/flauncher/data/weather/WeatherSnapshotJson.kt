package com.bisc.flauncher.data.weather

import com.bisc.flauncher.data.model.DailyWeather
import com.bisc.flauncher.data.model.HourlyWeather
import com.bisc.flauncher.data.model.WeatherSnapshot
import org.json.JSONArray
import org.json.JSONObject

/** JSON parsing and serialization of weather snapshots (broadcast and stored format). */
object WeatherSnapshotJson {
    fun parseBroadcast(json: JSONObject, sourceApp: String, nowMillis: Long = System.currentTimeMillis()): WeatherSnapshot? = runCatching {
        val current = kelvinToCelsius(json.getDouble("currentTemp"))
        val timestamp = json.optLongOrNull("timestamp")?.let { it * 1000L } ?: nowMillis
        WeatherSnapshot(
            location = json.optString("location"),
            currentTempC = current,
            condition = json.optString("currentCondition"),
            todayMinC = kelvinToCelsius(json.getDouble("todayMinTemp")),
            todayMaxC = kelvinToCelsius(json.getDouble("todayMaxTemp")),
            updatedAtMillis = timestamp,
            sourceApp = sourceApp,
            conditionCode = json.optIntOrNull("currentConditionCode"),
            feelsLikeC = json.optDoubleOrNull("feelsLikeTemp")?.let(::kelvinToCelsius),
            humidity = json.optIntOrNull("currentHumidity"),
            windSpeedKmh = json.optDoubleOrNull("windSpeed")?.toFloat(),
            windDirection = json.optIntOrNull("windDirection"),
            uvIndex = json.optDoubleOrNull("uvIndex")?.toFloat(),
            precipProbability = json.optIntOrNull("precipProbability"),
            pressureHpa = json.optDoubleOrNull("pressure")?.toFloat(),
            cloudCover = json.optIntOrNull("cloudCover"),
            sunRiseMillis = json.optLongOrNull("sunRise")?.let { it * 1000L },
            sunSetMillis = json.optLongOrNull("sunSet")?.let { it * 1000L },
            hourly = json.optJSONArray("hourly").objects().mapNotNull { hour ->
                HourlyWeather(
                    timestampMillis = (hour.optLongOrNull("timestamp") ?: return@mapNotNull null) * 1000L,
                    tempC = kelvinToCelsius(hour.optDoubleOrNull("temp") ?: return@mapNotNull null),
                    conditionCode = hour.optIntOrNull("conditionCode"),
                    humidity = hour.optIntOrNull("humidity"),
                    windSpeedKmh = hour.optDoubleOrNull("windSpeed")?.toFloat(),
                    windDirection = hour.optIntOrNull("windDirection"),
                    uvIndex = hour.optDoubleOrNull("uvIndex")?.toFloat(),
                    precipProbability = hour.optIntOrNull("precipProbability")
                )
            }.sortedBy { it.timestampMillis },
            daily = json.optJSONArray("forecasts").objects().mapNotNull { day ->
                DailyWeather(
                    minC = kelvinToCelsius(day.optDoubleOrNull("minTemp") ?: return@mapNotNull null),
                    maxC = kelvinToCelsius(day.optDoubleOrNull("maxTemp") ?: return@mapNotNull null),
                    conditionCode = day.optIntOrNull("conditionCode"),
                    humidity = day.optIntOrNull("humidity"),
                    windSpeedKmh = day.optDoubleOrNull("windSpeed")?.toFloat(),
                    windDirection = day.optIntOrNull("windDirection"),
                    uvIndex = day.optDoubleOrNull("uvIndex")?.toFloat(),
                    precipProbability = day.optIntOrNull("precipProbability"),
                    sunRiseMillis = day.optLongOrNull("sunRise")?.let { it * 1000L },
                    sunSetMillis = day.optLongOrNull("sunSet")?.let { it * 1000L }
                )
            }
        )
    }.getOrNull()

    private fun kelvinToCelsius(value: Double): Double = if (value > 150.0) value - 273.15 else value

    fun toStorage(s: WeatherSnapshot): String = JSONObject().apply {
        put("location", s.location)
        put("currentTempC", s.currentTempC)
        put("condition", s.condition)
        put("todayMinC", s.todayMinC)
        put("todayMaxC", s.todayMaxC)
        put("updatedAt", s.updatedAtMillis)
        put("sourceApp", s.sourceApp)
        s.conditionCode?.let { put("conditionCode", it) }
        s.feelsLikeC?.let { put("feelsLikeC", it) }
        s.humidity?.let { put("humidity", it) }
        s.windSpeedKmh?.let { put("windSpeedKmh", it.toDouble()) }
        s.windDirection?.let { put("windDirection", it) }
        s.uvIndex?.let { put("uvIndex", it.toDouble()) }
        s.precipProbability?.let { put("precipProbability", it) }
        s.pressureHpa?.let { put("pressureHpa", it.toDouble()) }
        s.cloudCover?.let { put("cloudCover", it) }
        s.sunRiseMillis?.let { put("sunRise", it) }
        s.sunSetMillis?.let { put("sunSet", it) }
        put("hourly", JSONArray().apply {
            s.hourly.forEach { h ->
                put(JSONObject().apply {
                    put("t", h.timestampMillis)
                    put("temp", h.tempC)
                    h.conditionCode?.let { put("code", it) }
                    h.humidity?.let { put("humidity", it) }
                    h.windSpeedKmh?.let { put("wind", it.toDouble()) }
                    h.windDirection?.let { put("windDir", it) }
                    h.uvIndex?.let { put("uv", it.toDouble()) }
                    h.precipProbability?.let { put("precip", it) }
                })
            }
        })
        put("daily", JSONArray().apply {
            s.daily.forEach { d ->
                put(JSONObject().apply {
                    put("min", d.minC)
                    put("max", d.maxC)
                    d.conditionCode?.let { put("code", it) }
                    d.humidity?.let { put("humidity", it) }
                    d.windSpeedKmh?.let { put("wind", it.toDouble()) }
                    d.windDirection?.let { put("windDir", it) }
                    d.uvIndex?.let { put("uv", it.toDouble()) }
                    d.precipProbability?.let { put("precip", it) }
                    d.sunRiseMillis?.let { put("sunRise", it) }
                    d.sunSetMillis?.let { put("sunSet", it) }
                })
            }
        })
    }.toString()

    fun fromStorage(text: String): WeatherSnapshot? = runCatching {
        val json = JSONObject(text)
        WeatherSnapshot(
            location = json.optString("location"),
            currentTempC = json.getDouble("currentTempC"),
            condition = json.optString("condition"),
            todayMinC = json.getDouble("todayMinC"),
            todayMaxC = json.getDouble("todayMaxC"),
            updatedAtMillis = json.optLong("updatedAt"),
            sourceApp = json.optString("sourceApp"),
            conditionCode = json.optIntOrNull("conditionCode"),
            feelsLikeC = json.optDoubleOrNull("feelsLikeC"),
            humidity = json.optIntOrNull("humidity"),
            windSpeedKmh = json.optDoubleOrNull("windSpeedKmh")?.toFloat(),
            windDirection = json.optIntOrNull("windDirection"),
            uvIndex = json.optDoubleOrNull("uvIndex")?.toFloat(),
            precipProbability = json.optIntOrNull("precipProbability"),
            pressureHpa = json.optDoubleOrNull("pressureHpa")?.toFloat(),
            cloudCover = json.optIntOrNull("cloudCover"),
            sunRiseMillis = json.optLongOrNull("sunRise"),
            sunSetMillis = json.optLongOrNull("sunSet"),
            hourly = json.optJSONArray("hourly").objects().map { h ->
                HourlyWeather(
                    timestampMillis = h.getLong("t"),
                    tempC = h.getDouble("temp"),
                    conditionCode = h.optIntOrNull("code"),
                    humidity = h.optIntOrNull("humidity"),
                    windSpeedKmh = h.optDoubleOrNull("wind")?.toFloat(),
                    windDirection = h.optIntOrNull("windDir"),
                    uvIndex = h.optDoubleOrNull("uv")?.toFloat(),
                    precipProbability = h.optIntOrNull("precip")
                )
            },
            daily = json.optJSONArray("daily").objects().map { d ->
                DailyWeather(
                    minC = d.getDouble("min"),
                    maxC = d.getDouble("max"),
                    conditionCode = d.optIntOrNull("code"),
                    humidity = d.optIntOrNull("humidity"),
                    windSpeedKmh = d.optDoubleOrNull("wind")?.toFloat(),
                    windDirection = d.optIntOrNull("windDir"),
                    uvIndex = d.optDoubleOrNull("uv")?.toFloat(),
                    precipProbability = d.optIntOrNull("precip"),
                    sunRiseMillis = d.optLongOrNull("sunRise"),
                    sunSetMillis = d.optLongOrNull("sunSet")
                )
            }
        )
    }.getOrNull()

    private fun JSONObject.optDoubleOrNull(name: String): Double? =
        if (has(name) && !isNull(name)) optDouble(name).takeIf { !it.isNaN() } else null

    private fun JSONObject.optIntOrNull(name: String): Int? = optDoubleOrNull(name)?.toInt()

    private fun JSONObject.optLongOrNull(name: String): Long? = optDoubleOrNull(name)?.toLong()

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
}
