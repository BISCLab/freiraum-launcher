package com.bisc.flauncher.data.model

/** Weather data received from a companion app through the Gadgetbridge broadcast format. */
data class WeatherSnapshot(
    val location: String,
    val currentTempC: Double,
    val condition: String,
    val todayMinC: Double,
    val todayMaxC: Double,
    val updatedAtMillis: Long,
    val sourceApp: String,
    val conditionCode: Int? = null,
    val feelsLikeC: Double? = null,
    val humidity: Int? = null,
    val windSpeedKmh: Float? = null,
    val windDirection: Int? = null,
    val uvIndex: Float? = null,
    val precipProbability: Int? = null,
    val pressureHpa: Float? = null,
    val cloudCover: Int? = null,
    val sunRiseMillis: Long? = null,
    val sunSetMillis: Long? = null,
    val hourly: List<HourlyWeather> = emptyList(),
    val daily: List<DailyWeather> = emptyList()
)

data class HourlyWeather(
    val timestampMillis: Long,
    val tempC: Double,
    val conditionCode: Int?,
    val humidity: Int? = null,
    val windSpeedKmh: Float? = null,
    val windDirection: Int? = null,
    val uvIndex: Float? = null,
    val precipProbability: Int? = null
)

/** One day of the forecast; index 0 in [WeatherSnapshot.daily] is today, then one day each. */
data class DailyWeather(
    val minC: Double,
    val maxC: Double,
    val conditionCode: Int?,
    val humidity: Int? = null,
    val windSpeedKmh: Float? = null,
    val windDirection: Int? = null,
    val uvIndex: Float? = null,
    val precipProbability: Int? = null,
    val sunRiseMillis: Long? = null,
    val sunSetMillis: Long? = null
)

/** Display category of a weather code. */
enum class WeatherCondition {
    CLEAR, PARTLY_CLOUDY, OVERCAST, RAIN, SNOW, SLEET, HAIL, THUNDERSTORM, WIND, FOG, HAZE, UNKNOWN;

    companion object {
        fun fromCode(code: Int?): WeatherCondition = when (code) {
            null -> UNKNOWN
            800 -> CLEAR
            801, 802, 803 -> PARTLY_CLOUDY
            804 -> OVERCAST
            511 -> HAIL
            611, 612, 613, 615, 616, 620, 621, 622 -> SLEET
            771, 781 -> WIND
            741 -> FOG
            701, 711, 721, 731, 751, 761, 762 -> HAZE
            in 200..232 -> THUNDERSTORM
            in 300..321, in 500..531 -> RAIN
            in 600..622 -> SNOW
            else -> UNKNOWN
        }
    }
}
