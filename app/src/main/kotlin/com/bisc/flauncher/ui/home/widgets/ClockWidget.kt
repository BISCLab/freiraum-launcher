package com.bisc.flauncher.ui.home.widgets

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bisc.flauncher.data.model.ClockAlignment
import com.bisc.flauncher.data.model.ClockStyle
import com.bisc.flauncher.data.model.ClockTimeFormat
import com.bisc.flauncher.ui.home.LocalSurfaceIsDark
import kotlin.math.cos
import kotlin.math.sin
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val DATE_GAP = 10f

private const val CAP_HEIGHT = 0.72f

private const val EDGE_PADDING = 24f

private val ClockInkOnLight = Color(0xFF3C3C3E)

/** Built-in reference widget: current time + date. */
@Composable
fun ClockWidget(
    modifier: Modifier = Modifier,
    style: String = ClockStyle.DIGITAL,
    alignment: String = ClockAlignment.CENTER,
    timeFormat: String = ClockTimeFormat.SYSTEM,
    dateFormat: String = "",
    showNextAlarm: Boolean = false,
    customInk: Color? = null,
    onDateClick: (() -> Unit)? = null,
    onTimeClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    // Driven by ACTION_TIME_TICK, the broadcast the system clock relies on: delivered once a minute
    // while the screen is on and not deferred under Doze the way a delay() loop would be.
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                now = LocalDateTime.now()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        now = LocalDateTime.now()
        onDispose { context.unregisterReceiver(receiver) }
    }
    // The tick doesn't arrive while the screen is off and this widget stays composed through that,
    // so resync the moment the home screen is visible again.
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        now = LocalDateTime.now()
        onPauseOrDispose { }
    }
    // The analog face draws a second hand, which the once-a-minute tick above would leave frozen
    // for a whole minute - while that face is on screen, update every second as well.
    if (style == ClockStyle.ANALOG) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(1000L - System.currentTimeMillis() % 1000L)
                now = LocalDateTime.now()
            }
        }
    }

    val locale = LocalConfiguration.current.locales.get(0)
    val use24h = remember(timeFormat, context) {
        when (timeFormat) {
            ClockTimeFormat.H24 -> true
            ClockTimeFormat.H12 -> false
            else -> android.text.format.DateFormat.is24HourFormat(context)
        }
    }
    val timePattern = if (use24h) "HH:mm" else "h:mm a"
    val dateText = remember(now, locale, dateFormat) {
        // Default: weekday plus numeric date in the language's own order and separators
        // ("Montag, 26.10.2026", "Monday, 10/26/2026") rather than one hardcoded German pattern.
        val localDefault = android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEEddMMyyyy")
        val pattern = dateFormat.ifBlank { localDefault }
        runCatching { DateTimeFormatter.ofPattern(pattern, locale).format(now) }
            .getOrElse { DateTimeFormatter.ofPattern(localDefault, locale).format(now) }
            .replaceFirstChar { it.titlecase(locale) }
    }
    val longDateText = remember(now, locale, dateFormat) {
        if (dateFormat.isNotBlank()) dateText
        else DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(now)
    }
    val nextAlarmText = remember(now, use24h, timePattern) {
        if (!showNextAlarm) return@remember null
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerMillis = alarmManager?.nextAlarmClock?.triggerTime ?: return@remember null
        val alarmTime = Instant.ofEpochMilli(triggerMillis).atZone(ZoneId.systemDefault()).toLocalTime()
        DateTimeFormatter.ofPattern(timePattern, locale).format(alarmTime)
    }

    val surfaceDark = LocalSurfaceIsDark.current ?: (MaterialTheme.colorScheme.background.luminance() < 0.5f)
    val ink = customInk ?: (if (surfaceDark) Color.White else ClockInkOnLight)
    val shadow = if (surfaceDark) Shadow(Color.Black.copy(alpha = 0.6f), blurRadius = 8f)
    else Shadow(Color.White.copy(alpha = 0.35f), blurRadius = 6f)
    val dateModifier = if (onDateClick != null) Modifier.clickable(onClick = onDateClick) else Modifier
    val timeModifier = if (onTimeClick != null) Modifier.clickable(onClick = onTimeClick) else Modifier

    val hourNumber = if (use24h) now.hour else (now.hour % 12).let { if (it == 0) 12 else it }
    val hourText = if (use24h) hourNumber.toString().padStart(2, '0') else hourNumber.toString()
    val minuteText = now.minute.toString().padStart(2, '0')
    val density = LocalDensity.current
    fun sp(dp: Float): TextUnit = with(density) { dp.dp.toSp() }

    val horizontalAlignment = when (alignment) {
        ClockAlignment.START -> Alignment.Start
        ClockAlignment.END -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
    val textAlign = when (alignment) {
        ClockAlignment.START -> TextAlign.Start
        ClockAlignment.END -> TextAlign.End
        else -> TextAlign.Center
    }
    val edgePadding = if (alignment == ClockAlignment.CENTER) 0f else EDGE_PADDING

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val w = maxWidth.value - 2f * maxOf(edgePadding, 16f)
        val h = maxHeight.value
        val alarmReserve = if (nextAlarmText != null) 24f else 0f
        val dateReserve = DATE_GAP + CAP_HEIGHT * 14f + 8f

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = edgePadding.dp),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = Arrangement.Center
        ) {
            when (style) {
                ClockStyle.DIGITAL_LARGE -> {
                    val dateFs = 16f
                    val reserve = DATE_GAP + CAP_HEIGHT * dateFs + 8f
                    val fs = minOf((h - reserve - alarmReserve - 8f) / 1.2f, w / (if (use24h) 3.2f else 3.0f), 128f).coerceAtLeast(24f)
                    TimeText("$hourText:$minuteText", fs, FontWeight.Bold, FontFamily.Default, ink, shadow, textAlign, ::sp, timeModifier)
                    DateText(dateText, dateFs, FontFamily.Default, FontWeight.Normal, ink.copy(alpha = 0.8f), shadow, textAlign, ::sp, dateModifier)
                }

                ClockStyle.STACKED -> {
                    val fs = minOf((h - alarmReserve - 16f) / 2.15f, w * 0.44f, 280f).coerceAtLeast(28f)
                    val dateFs = (fs * 0.14f).coerceIn(13f, 30f)
                    TimeText(
                        text = "${hourNumber.toString().padStart(2, '0')}\n$minuteText",
                        fontSizeDp = fs,
                        weight = FontWeight.Bold,
                        family = FontFamily.Serif,
                        ink = ink,
                        shadow = shadow,
                        textAlign = textAlign,
                        sp = ::sp,
                        modifier = timeModifier,
                        lineHeightFactor = 0.8f,
                        maxLines = 2
                    )
                    DateText(longDateText, dateFs, FontFamily.Serif, FontWeight.Bold, ink.copy(alpha = 0.9f), shadow, textAlign, ::sp, dateModifier)
                }

                ClockStyle.ANALOG -> {
                    val face = minOf(w * 0.6f, h - dateReserve - alarmReserve - 8f, 240f).coerceAtLeast(56f)
                    AnalogClockFace(now = now, ink = ink, surfaceDark = surfaceDark, modifier = Modifier.size(face.dp).then(timeModifier))
                    LabelDate(dateText, locale, ink, shadow, textAlign, dateModifier)
                }

                ClockStyle.ORBIT -> {
                    val ring = minOf(w * 0.6f, h - dateReserve - alarmReserve - 8f, 220f).coerceAtLeast(72f)
                    OrbitClockFace(
                        now = now,
                        timeText = "$hourText:$minuteText",
                        ink = ink,
                        surfaceDark = surfaceDark,
                        diameterDp = ring,
                        sp = ::sp,
                        modifier = Modifier.size(ring.dp).then(timeModifier)
                    )
                    // The ring is drawn inset by 5 of its 54 units.
                    LabelDate(dateText, locale, ink, shadow, textAlign, dateModifier, pullUp = ring * 5f / 108f)
                }

                ClockStyle.BINARY -> {
                    val scale = ((h - dateReserve - alarmReserve - 16f) / 69f).coerceIn(0.55f, 1.5f)
                    BinaryClockFace(now = now, use24h = use24h, ink = ink, scale = scale, modifier = timeModifier)
                    LabelDate(dateText, locale, ink, shadow, textAlign, dateModifier)
                }

                else -> {
                    val fs = minOf((h - dateReserve - alarmReserve - 8f) / 1.25f, w / (if (use24h) 3.0f else 2.6f), 72f).coerceAtLeast(24f)
                    TimeText("$hourText:$minuteText", fs, FontWeight.Light, FontFamily.Default, ink, shadow, textAlign, ::sp, timeModifier)
                    LabelDate(dateText, locale, ink, shadow, textAlign, dateModifier)
                }
            }
            NextAlarmLine(nextAlarmText, ink)
        }
    }
}

@Composable
private fun TimeText(
    text: String,
    fontSizeDp: Float,
    weight: FontWeight,
    family: FontFamily,
    ink: Color,
    shadow: Shadow,
    textAlign: TextAlign,
    sp: (Float) -> TextUnit,
    modifier: Modifier,
    lineHeightFactor: Float = 1.15f,
    maxLines: Int = 1
) {
    Text(
        text = text,
        modifier = Modifier
            .offset(x = (fontSizeDp * when (textAlign) { TextAlign.Start -> -0.075f; TextAlign.End -> 0.03f; else -> 0f }).dp)
            .paddingFromBaseline(bottom = 0.dp)
            .then(modifier),
        textAlign = textAlign,
        maxLines = maxLines,
        softWrap = false,
        style = MaterialTheme.typography.displayLarge.copy(
            color = ink,
            fontFamily = family,
            fontWeight = weight,
            fontSize = sp(fontSizeDp),
            lineHeight = sp(fontSizeDp * lineHeightFactor),
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
            shadow = shadow
        )
    )
}

@Composable
private fun DateText(
    text: String,
    fontSizeDp: Float,
    family: FontFamily,
    weight: FontWeight,
    color: Color,
    shadow: Shadow,
    textAlign: TextAlign,
    sp: (Float) -> TextUnit,
    modifier: Modifier
) {
    Text(
        text = text,
        modifier = Modifier.paddingFromBaseline(top = (DATE_GAP + CAP_HEIGHT * fontSizeDp).dp).then(modifier),
        textAlign = textAlign,
        maxLines = 1,
        style = MaterialTheme.typography.titleMedium.copy(
            color = color,
            fontFamily = family,
            fontWeight = weight,
            fontSize = sp(fontSizeDp),
            shadow = shadow
        )
    )
}

@Composable
private fun LabelDate(
    dateText: String,
    locale: java.util.Locale,
    ink: Color,
    shadow: Shadow,
    textAlign: TextAlign,
    modifier: Modifier,
    pullUp: Float = 0f
) {
    Text(
        text = dateText.uppercase(locale),
        modifier = Modifier.paddingFromBaseline(top = (DATE_GAP + CAP_HEIGHT * 14f - pullUp).dp).then(modifier),
        textAlign = textAlign,
        maxLines = 1,
        style = MaterialTheme.typography.labelLarge.copy(
            color = ink.copy(alpha = 0.75f),
            letterSpacing = 2.sp,
            shadow = shadow
        )
    )
}

@Composable
private fun NextAlarmLine(text: String?, ink: Color) {
    if (text == null) return
    val smallShadow = Shadow(ink.copy(alpha = 0f), blurRadius = 0f)
    Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Alarm,
            contentDescription = null,
            tint = ink.copy(alpha = 0.75f),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.labelMedium.copy(color = ink.copy(alpha = 0.75f), shadow = smallShadow)
        )
    }
}

@Composable
private fun AnalogClockFace(now: LocalDateTime, ink: Color, surfaceDark: Boolean, modifier: Modifier = Modifier) {
    val discShadow = if (surfaceDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.3f)
    Canvas(modifier = modifier) {
        val unit = (size.minDimension / 2f) / 48f
        val radius = size.minDimension / 2f - 1.5f * unit
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(color = discShadow, radius = radius, center = center + Offset(2f * unit, 3f * unit))
        drawCircle(color = ink.copy(alpha = 0.9f), radius = radius, center = center, style = Stroke(width = 3f * unit))

        fun hand(angleDegrees: Double, lengthFraction: Float, strokeWidth: Float, alpha: Float) {
            val angle = Math.toRadians(angleDegrees - 90.0)
            val end = Offset(
                x = center.x + (radius * lengthFraction * cos(angle)).toFloat(),
                y = center.y + (radius * lengthFraction * sin(angle)).toFloat()
            )
            drawLine(color = ink.copy(alpha = alpha), start = center, end = end, strokeWidth = strokeWidth * unit, cap = StrokeCap.Round)
        }

        val hourAngle = (now.hour % 12 + now.minute / 60f) * 30.0
        val minuteAngle = (now.minute + now.second / 60f) * 6.0
        val secondAngle = now.second * 6.0

        hand(hourAngle, 0.5f, 7f, 0.95f)
        hand(minuteAngle, 0.75f, 5f, 0.95f)
        hand(secondAngle, 0.8f, 2f, 0.7f)
        drawCircle(color = ink, radius = 5f * unit, center = center)
    }
}

@Composable
private fun OrbitClockFace(
    now: LocalDateTime,
    timeText: String,
    ink: Color,
    surfaceDark: Boolean,
    diameterDp: Float,
    sp: (Float) -> TextUnit,
    modifier: Modifier = Modifier
) {
    val dotShadow = if (surfaceDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.3f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val half = size.minDimension / 2f
            val unit = half / 54f
            val outerRadius = half - 6f * unit
            val innerRadius = outerRadius * 0.68f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(color = ink.copy(alpha = 0.35f), radius = outerRadius, center = center, style = Stroke(width = 2f * unit))
            drawCircle(color = ink.copy(alpha = 0.35f), radius = innerRadius, center = center, style = Stroke(width = 2f * unit))

            fun dotAt(radius: Float, fraction: Float, dotRadius: Float, alpha: Float) {
                val angle = Math.toRadians(fraction * 360.0 - 90.0)
                val position = Offset(
                    x = center.x + (radius * cos(angle)).toFloat(),
                    y = center.y + (radius * sin(angle)).toFloat()
                )
                drawCircle(color = dotShadow, radius = dotRadius, center = position + Offset(1f * unit, 2f * unit))
                drawCircle(color = ink.copy(alpha = alpha), radius = dotRadius, center = position)
            }

            val minuteFraction = (now.minute + now.second / 60f) / 60f
            val hourFraction = (now.hour % 12 + now.minute / 60f) / 12f
            dotAt(outerRadius, minuteFraction, 6f * unit, 0.95f)
            dotAt(innerRadius, hourFraction, 6f * unit, 0.85f)
        }
        Text(
            text = timeText,
            maxLines = 1,
            softWrap = false,
            style = MaterialTheme.typography.titleMedium.copy(color = ink, fontWeight = FontWeight.Medium, fontSize = sp(diameterDp * 0.19f))
        )
    }
}

@Composable
private fun BinaryClockFace(now: LocalDateTime, use24h: Boolean, ink: Color, scale: Float, modifier: Modifier = Modifier) {
    val hour = if (use24h) now.hour else (now.hour % 12).let { if (it == 0) 12 else it }
    val digits = listOf(hour / 10, hour % 10, now.minute / 10, now.minute % 10)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy((14f * scale).dp)) {
        digits.forEachIndexed { index, digit ->
            Column(
                modifier = if (index == 2) Modifier.padding(start = (6f * scale).dp) else Modifier,
                verticalArrangement = Arrangement.spacedBy((7f * scale).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (bit in 3 downTo 0) {
                    val lit = (digit shr bit) and 1 == 1
                    Box(
                        modifier = Modifier
                            .size((12f * scale).dp)
                            .clip(CircleShape)
                            .background(ink.copy(alpha = if (lit) 0.95f else 0.18f))
                    )
                }
            }
        }
    }
}
