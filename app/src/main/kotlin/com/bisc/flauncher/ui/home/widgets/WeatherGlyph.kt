package com.bisc.flauncher.ui.home.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.bisc.flauncher.data.model.WeatherCondition

private val SunColor = Color(0xFFFFB300)
private val MoonColor = Color(0xFF9FB4D6)

/** One weather condition as a symbol, drawn from stock Material icons. */
@Composable
fun WeatherGlyph(
    condition: WeatherCondition,
    night: Boolean,
    size: Dp,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(size)) {
        when (condition) {
            WeatherCondition.CLEAR ->
                Icon(if (night) Icons.Filled.NightsStay else Icons.Filled.WbSunny, null, Modifier.fillMaxSize(), tint = if (night) MoonColor else SunColor)

            WeatherCondition.PARTLY_CLOUDY -> {
                Icon(
                    if (night) Icons.Filled.NightsStay else Icons.Filled.WbSunny, null,
                    Modifier.size(size * 0.62f).align(Alignment.TopStart),
                    tint = if (night) MoonColor else SunColor
                )
                Icon(Icons.Filled.Cloud, null, Modifier.size(size * 0.78f).align(Alignment.BottomEnd), tint = tint)
            }

            else -> Icon(condition.imageVector(), null, Modifier.fillMaxSize(), tint = tint)
        }
    }
}

private fun WeatherCondition.imageVector(): ImageVector = when (this) {
    WeatherCondition.RAIN -> Icons.Filled.Umbrella
    WeatherCondition.SNOW, WeatherCondition.SLEET -> Icons.Filled.AcUnit
    WeatherCondition.HAIL -> Icons.Filled.Grain
    WeatherCondition.THUNDERSTORM -> Icons.Filled.Thunderstorm
    WeatherCondition.WIND -> Icons.Filled.Air
    WeatherCondition.FOG, WeatherCondition.HAZE -> Icons.Filled.BlurOn
    else -> Icons.Filled.Cloud
}
