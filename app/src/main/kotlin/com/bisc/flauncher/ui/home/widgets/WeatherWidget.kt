package com.bisc.flauncher.ui.home.widgets

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.DailyWeather
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.HourlyWeather
import com.bisc.flauncher.data.model.WeatherCondition
import com.bisc.flauncher.data.model.WeatherSnapshot
import com.bisc.flauncher.ui.home.HomeViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private val HOURLY_MIN_HEIGHT = 130.dp
private val DAILY_MIN_HEIGHT = 172.dp

private val ROOMY_MIN_HEIGHT = 260.dp
private val COMPACT_MAX_HEIGHT = 120.dp

private const val MAX_DAYS = 7

/** Current conditions, hourly and daily forecast from the last received weather snapshot. */
@Composable
fun WeatherWidget(
    modifier: Modifier = Modifier,
    appearance: ElementAppearance = ElementAppearance(),
    cornerRadiusPercent: Int = 25,
    horizontalMargin: Dp = 0.dp,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val snapshot by viewModel.weather.collectAsState()

    WidgetCard(modifier = modifier, appearance = appearance, cornerRadiusPercent = cornerRadiusPercent, horizontalMargin = horizontalMargin) {
        val weather = snapshot
        if (weather == null) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Cloud, contentDescription = null, tint = LocalContentColor.current.copy(alpha = 0.6f))
                Text(
                    stringResource(R.string.widget_weather_no_data),
                    color = LocalContentColor.current.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        } else {
            WeatherContent(weather)
        }
    }
}

private data class Shown(
    val tempC: Double,
    val conditionCode: Int?,
    val conditionText: String,
    val night: Boolean,
    val precipProbability: Int?,
    val windSpeedKmh: Float?,
    val windDirection: Int?,
    val uvIndex: Float?,
    val humidity: Int?
)

@Composable
private fun WeatherContent(weather: WeatherSnapshot) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales.get(0)
    val zone = remember { ZoneId.systemDefault() }
    val nowMillis = remember(weather) { System.currentTimeMillis() }
    val today = remember(weather) { LocalDate.now(zone) }

    var selectedDay by remember(weather.updatedAtMillis) { mutableIntStateOf(0) }
    var selectedHour by remember(weather.updatedAtMillis) { mutableStateOf<Long?>(null) }

    val hoursByDate = remember(weather) {
        weather.hourly.groupBy { Instant.ofEpochMilli(it.timestampMillis).atZone(zone).toLocalDate() }
    }
    val dayCount = remember(weather) {
        val fromHours = hoursByDate.keys.maxOfOrNull { java.time.temporal.ChronoUnit.DAYS.between(today, it).toInt() + 1 } ?: 0
        max(weather.daily.size, fromHours).coerceIn(1, MAX_DAYS)
    }
    val day = selectedDay.coerceIn(0, dayCount - 1)
    val dayDate = today.plusDays(day.toLong())
    val dayHours = remember(weather, day) {
        val all = hoursByDate[dayDate].orEmpty()
        if (day == 0) all.filter { it.timestampMillis > nowMillis - 3_600_000L } else all
    }
    val dayForecast = weather.daily.getOrNull(day)

    fun isNight(timestamp: Long): Boolean {
        val daily = weather.daily.getOrNull(
            java.time.temporal.ChronoUnit.DAYS.between(today, Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()).toInt()
        )
        val rise = daily?.sunRiseMillis ?: weather.sunRiseMillis
        val set = daily?.sunSetMillis ?: weather.sunSetMillis
        if (rise != null && set != null) {
            val sameDay = Instant.ofEpochMilli(rise).atZone(zone).toLocalDate() == Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
            if (sameDay) return timestamp < rise || timestamp > set
        }
        val hour = Instant.ofEpochMilli(timestamp).atZone(zone).hour
        return hour < 6 || hour >= 21
    }

    val hourSelected = selectedHour?.let { ts -> dayHours.firstOrNull { it.timestampMillis == ts } }
    val shown: Shown = when {
        hourSelected != null -> hourSelected.toShown(context, isNight(hourSelected.timestampMillis))
        day == 0 -> Shown(
            tempC = weather.currentTempC,
            conditionCode = weather.conditionCode,
            conditionText = conditionLabel(context, weather.conditionCode, weather.condition),
            night = isNight(nowMillis),
            precipProbability = weather.precipProbability,
            windSpeedKmh = weather.windSpeedKmh,
            windDirection = weather.windDirection,
            uvIndex = weather.uvIndex,
            humidity = weather.humidity
        )
        dayForecast != null -> dayForecast.toShown(context)
        else -> dayHours.firstOrNull()?.toShown(context, false)
            ?: Shown(weather.currentTempC, weather.conditionCode, weather.condition, false, null, null, null, null, null)
    }
    val headerCondition = if (hourSelected == null && day > 0 && dayForecast != null) {
        "${shown.conditionText} · ${dayForecast.minC.roundToInt()}° / ${dayForecast.maxC.roundToInt()}°"
    } else shown.conditionText

    val onHeaderClick = remember(context) { openWeatherApp(context) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxHeight < COMPACT_MAX_HEIGHT
        val showHourly = maxHeight >= HOURLY_MIN_HEIGHT && dayHours.isNotEmpty()
        val showDays = maxHeight >= DAILY_MIN_HEIGHT && dayCount > 1
        val timeFormat24 = remember { DateFormat.is24HourFormat(context) }

        val roomy = maxHeight >= ROOMY_MIN_HEIGHT
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Header(
                weather = weather,
                shown = shown,
                conditionText = headerCondition,
                compact = compact,
                roomy = roomy,
                feelsLikeC = if (hourSelected == null && day == 0) weather.feelsLikeC else null,
                showUpdated = roomy || !showHourly,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onHeaderClick != null) Modifier.clickable(onClick = onHeaderClick) else Modifier)
            )

            if (showHourly) {
                HourSelector(
                    hours = dayHours,
                    selected = selectedHour,
                    onSelect = { selectedHour = if (selectedHour == it) null else it },
                    timeFormat24 = timeFormat24,
                    locale = locale,
                    isNight = ::isNight,
                    zone = zone,
                    roomy = roomy
                )
            }
            if (showDays) {
                DaySelector(
                    dayCount = dayCount,
                    selectedDay = day,
                    daily = weather.daily,
                    today = today,
                    locale = locale,
                    roomy = roomy,
                    onSelect = { selectedDay = it; selectedHour = null }
                )
            }
        }
    }
}

@Composable
private fun Header(
    weather: WeatherSnapshot,
    shown: Shown,
    conditionText: String,
    compact: Boolean,
    roomy: Boolean,
    feelsLikeC: Double?,
    showUpdated: Boolean,
    modifier: Modifier
) {
    val content = LocalContentColor.current
    val condition = WeatherCondition.fromCode(shown.conditionCode)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(weather.location, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.widget_weather_temp, shown.tempC.roundToInt()),
                style = if (roomy) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displaySmall
            )
            Text(
                conditionText,
                color = content.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (roomy && feelsLikeC != null) {
                Text(
                    stringResource(R.string.widget_weather_feels_like, feelsLikeC.roundToInt()),
                    color = content.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (showUpdated && weather.updatedAtMillis > 0L) {
                Text(
                    stringResource(R.string.widget_weather_updated, formatClock(LocalContext.current, weather.updatedAtMillis)),
                    color = content.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            WeatherGlyph(condition = condition, night = shown.night, size = if (compact) 32.dp else if (roomy) 44.dp else 34.dp, tint = content)
            if (compact) {
                Text(
                    stringResource(R.string.widget_weather_minmax, weather.todayMinC.roundToInt(), weather.todayMaxC.roundToInt()),
                    color = content.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Details(shown, maxLines = if (roomy) 4 else 2)
            }
        }
    }
}

@Composable
private fun Details(shown: Shown, maxLines: Int) {
    val items = buildList<DetailItem> {
        val precip = shown.precipProbability
        if (precip != null && precip >= 10) add(DetailItem({ DetailIcon(Icons.Filled.Umbrella) }, "$precip%"))
        val wind = shown.windSpeedKmh
        if (wind != null && wind >= 5f) {
            val direction = shown.windDirection
            add(
                DetailItem(
                    icon = {
                        if (direction != null) DetailIcon(Icons.Filled.Navigation, Modifier.rotate(direction + 180f))
                        else DetailIcon(Icons.Filled.Air)
                    },
                    text = stringResource(R.string.widget_weather_wind, wind.roundToInt())
                )
            )
        }
        val uv = shown.uvIndex
        if (uv != null && uv >= 3f) add(DetailItem({ DetailIcon(Icons.Filled.WbSunny) }, stringResource(R.string.widget_weather_uv, uv.roundToInt())))
        val humidity = shown.humidity
        if (humidity != null) add(DetailItem({ DetailIcon(Icons.Filled.WaterDrop) }, "$humidity%"))
    }
    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(top = 4.dp)) {
        items.take(maxLines).forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                item.icon()
                Text(item.text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 4.dp), maxLines = 1)
            }
        }
    }
}

private class DetailItem(val icon: @Composable () -> Unit, val text: String)

@Composable
private fun DetailIcon(icon: ImageVector, modifier: Modifier = Modifier) {
    Icon(icon, contentDescription = null, tint = LocalContentColor.current.copy(alpha = 0.75f), modifier = modifier.size(14.dp))
}

@Composable
private fun HourSelector(
    hours: List<HourlyWeather>,
    selected: Long?,
    onSelect: (Long) -> Unit,
    timeFormat24: Boolean,
    locale: Locale,
    isNight: (Long) -> Boolean,
    zone: ZoneId,
    roomy: Boolean,
    modifier: Modifier = Modifier
) {
    val listState: LazyListState = rememberLazyListState()
    LaunchedEffect(hours.firstOrNull()?.timestampMillis) { listState.scrollToItem(0) }
    val formatter = remember(timeFormat24, locale) { DateTimeFormatter.ofPattern(if (timeFormat24) "HH:mm" else "h a", locale) }
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        itemsIndexed(hours, key = { _, h -> h.timestampMillis }) { _, hour ->
            val isSelected = hour.timestampMillis == selected
            SelectorTile(selected = isSelected, onClick = { onSelect(hour.timestampMillis) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 44.dp).padding(vertical = if (roomy) 11.dp else 6.dp, horizontal = 4.dp)) {
                    WeatherGlyph(
                        condition = WeatherCondition.fromCode(hour.conditionCode),
                        night = isNight(hour.timestampMillis),
                        size = if (roomy) 30.dp else 22.dp,
                        tint = LocalContentColor.current
                    )
                    Text(
                        formatter.format(Instant.ofEpochMilli(hour.timestampMillis).atZone(zone)),
                        style = MaterialTheme.typography.labelSmall,
                        softWrap = false,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        stringResource(R.string.widget_weather_temp, hour.tempC.roundToInt()),
                        style = MaterialTheme.typography.labelSmall,
                        softWrap = false
                    )
                    if (roomy) {
                        val precip = hour.precipProbability
                        Text(
                            if (precip != null && precip >= 10) "$precip%" else "\u00A0",
                            style = MaterialTheme.typography.labelSmall,
                            color = LocalContentColor.current.copy(alpha = 0.6f),
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DaySelector(
    dayCount: Int,
    selectedDay: Int,
    daily: List<DailyWeather>,
    today: LocalDate,
    locale: Locale,
    roomy: Boolean,
    onSelect: (Int) -> Unit
) {
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("EEE", locale) }
    LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        itemsIndexed((0 until dayCount).toList()) { _, index ->
            val forecast = daily.getOrNull(index)
            SelectorTile(selected = index == selectedDay, onClick = { onSelect(index) }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = if (roomy) 10.dp else 6.dp)
                ) {
                    if (forecast != null) {
                        WeatherGlyph(WeatherCondition.fromCode(forecast.conditionCode), night = false, size = if (roomy) 22.dp else 18.dp, tint = LocalContentColor.current)
                    }
                    Text(
                        text = buildString {
                            append(formatter.format(today.plusDays(index.toLong())))
                            if (forecast != null) append(" ${forecast.minC.roundToInt()}° / ${forecast.maxC.roundToInt()}°")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        softWrap = false,
                        modifier = Modifier.padding(start = if (forecast != null) 6.dp else 0.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectorTile(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val tint = LocalContentColor.current
    Column(
        modifier = Modifier
            .background(tint.copy(alpha = if (selected) 0.22f else 0.08f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) { content() }
}

private fun HourlyWeather.toShown(context: Context, night: Boolean) = Shown(
    tempC = tempC,
    conditionCode = conditionCode,
    conditionText = conditionLabel(context, conditionCode, ""),
    night = night,
    precipProbability = precipProbability,
    windSpeedKmh = windSpeedKmh,
    windDirection = windDirection,
    uvIndex = uvIndex,
    humidity = humidity
)

private fun DailyWeather.toShown(context: Context) = Shown(
    tempC = maxC,
    conditionCode = conditionCode,
    conditionText = conditionLabel(context, conditionCode, ""),
    night = false,
    precipProbability = precipProbability,
    windSpeedKmh = windSpeedKmh,
    windDirection = windDirection,
    uvIndex = uvIndex,
    humidity = humidity
)

private fun conditionLabel(context: Context, code: Int?, fallback: String): String {
    val condition = WeatherCondition.fromCode(code)
    if (condition == WeatherCondition.UNKNOWN && fallback.isNotBlank()) return fallback
    return context.getString(
        when (condition) {
            WeatherCondition.CLEAR -> R.string.weather_condition_clear
            WeatherCondition.PARTLY_CLOUDY -> R.string.weather_condition_partly_cloudy
            WeatherCondition.OVERCAST -> R.string.weather_condition_overcast
            WeatherCondition.RAIN -> R.string.weather_condition_rain
            WeatherCondition.SNOW -> R.string.weather_condition_snow
            WeatherCondition.SLEET -> R.string.weather_condition_sleet
            WeatherCondition.HAIL -> R.string.weather_condition_hail
            WeatherCondition.THUNDERSTORM -> R.string.weather_condition_thunderstorm
            WeatherCondition.WIND -> R.string.weather_condition_wind
            WeatherCondition.FOG -> R.string.weather_condition_fog
            WeatherCondition.HAZE -> R.string.weather_condition_haze
            WeatherCondition.UNKNOWN -> R.string.weather_condition_unknown
        }
    )
}

private fun formatClock(context: Context, millis: Long): String =
    DateFormat.getTimeFormat(context).format(java.util.Date(millis))

private fun openWeatherApp(context: Context): (() -> Unit)? {
    val probe = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_WEATHER)
    val info = context.packageManager.resolveActivity(probe, 0)?.activityInfo ?: return null
    return {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_WEATHER)
                    .setComponent(ComponentName(info.packageName, info.name))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
