package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ColorFill
import com.bisc.flauncher.data.model.FillMode
import com.bisc.flauncher.ui.theme.defaultBoxFillColorFor
import com.bisc.flauncher.data.model.GradientDirection
import com.bisc.flauncher.data.model.asBrush
import kotlin.math.max
import kotlin.math.min

/**
 * A hand-rolled HSV color picker. When [onEyedropperCapture] is given (e.g. for a widget's own
 * background), an eyedropper button captures that content and lets the user tap a point on it to
 * pick the exact color showing there, so a fill can be matched to it with no visible seam.
 */
@Composable
fun ColorPickerControls(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    onEyedropperCapture: (() -> ImageBitmap?)? = null
) {
    val initialHsv = remember { rgbToHsv(color) }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var hexText by remember { mutableStateOf(colorToHex(color)) }

    fun updateFromHsv() {
        val updated = hsvToColor(hue, saturation, value)
        hexText = colorToHex(updated)
        onColorChange(updated)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(hsvToColor(hue, saturation, value))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        )

        val hueColor = hsvToColor(hue, 1f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(hueColor)
                .pointerInput(Unit) {
                    // onColorChange only fires once the drag ends, not per pointer-move frame — each
                    // call flows into a settings write that rewrites the whole shared preferences
                    // file, so doing that continuously while dragging is what made this picker (and
                    // anything using it — every color in the app) feel severely laggy. The marker and
                    // swatch above still track the drag live since they read the local hue/saturation/
                    // value state directly, not onColorChange's result.
                    detectDragGestures(
                        onDragEnd = { updateFromHsv() },
                        onDrag = { change, _ ->
                            change.consume()
                            saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                            value = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        saturation = (offset.x / size.width).coerceIn(0f, 1f)
                        value = (1f - offset.y / size.height).coerceIn(0f, 1f)
                        updateFromHsv()
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                drawRect(Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                val markerX = saturation * size.width
                val markerY = (1f - value) * size.height
                drawCircle(color = Color.White, radius = 10.dp.toPx(), center = Offset(markerX, markerY), style = Stroke(width = 3.dp.toPx()))
                drawCircle(color = Color.Black.copy(alpha = 0.4f), radius = 10.dp.toPx() + 2.dp.toPx(), center = Offset(markerX, markerY), style = Stroke(width = 1.dp.toPx()))
            }
        }

        val hueBrush = remember { Brush.horizontalGradient((0..360 step 30).map { hsvToColor(it.toFloat(), 1f, 1f) }) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(hueBrush)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { updateFromHsv() },
                        onDrag = { change, _ ->
                            change.consume()
                            hue = ((change.position.x / size.width).coerceIn(0f, 1f)) * 360f
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        hue = ((offset.x / size.width).coerceIn(0f, 1f)) * 360f
                        updateFromHsv()
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
                val markerX = (hue / 360f) * size.width
                drawCircle(color = Color.White, radius = size.height / 2f, center = Offset(markerX, size.height / 2f), style = Stroke(width = 3.dp.toPx()))
            }
        }

        OutlinedTextField(
            value = hexText,
            onValueChange = { text ->
                hexText = text
                hexToColor(text)?.let { parsed ->
                    val hsv = rgbToHsv(parsed)
                    hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
                    onColorChange(parsed)
                }
            },
            label = { Text(stringResource(R.string.settings_sidebar_color_hex_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            shape = SettingsTextFieldShape,
            modifier = Modifier.fillMaxWidth()
        )

        fun applyPreset(c: Color) {
            val h = rgbToHsv(c)
            hue = h[0]; saturation = h[1]; value = h[2]
            updateFromHsv()
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ColorPresetSwatch(Color.Black, ::applyPreset)
            ColorPresetSwatch(Color.White, ::applyPreset)
            // The default surface color of the current light/dark mode, for when a color was picked by mistake.
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            val default = Color(defaultBoxFillColorFor(isDark))
            TextButton(onClick = { applyPreset(default) }) {
                Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.settings_color_reset_default), modifier = Modifier.padding(start = 6.dp))
            }
        }

        if (onEyedropperCapture != null) {
            var sample by remember { mutableStateOf<ImageBitmap?>(null) }
            TextButton(onClick = { sample = onEyedropperCapture() }) {
                Icon(Icons.Filled.Colorize, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.settings_color_eyedropper), modifier = Modifier.padding(start = 6.dp))
            }
            sample?.let { bitmap ->
                EyedropperDialog(
                    bitmap = bitmap,
                    onPick = { picked -> applyPreset(picked); sample = null },
                    onDismiss = { sample = null }
                )
            }
        }
    }
}

/** A captured snapshot with a tap-to-sample gesture; tapping a point picks the color shown there. */
@Composable
private fun EyedropperDialog(bitmap: ImageBitmap, onPick: (Color) -> Unit, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        val pixelMap = remember(bitmap) { bitmap.toPixelMap() }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.settings_color_eyedropper_hint), style = MaterialTheme.typography.bodyMedium)
            var displaySize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .onGloballyPositioned { displaySize = it.size.toSize() }
                    .pointerInput(bitmap) {
                        detectTapGestures { offset ->
                            if (displaySize.width <= 0f || displaySize.height <= 0f) return@detectTapGestures
                            val scaleX = bitmap.width / displaySize.width
                            val scaleY = bitmap.height / displaySize.height
                            val x = (offset.x * scaleX).toInt().coerceIn(0, bitmap.width - 1)
                            val y = (offset.y * scaleY).toInt().coerceIn(0, bitmap.height - 1)
                            onPick(pixelMap[x, y])
                        }
                    }
            )
            TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.widget_notes_cancel))
            }
        }
    }
}

/** A titled row (with a live swatch) that expands [ColorPickerControls] inline beneath itself when tapped. */
@Composable
fun ColorPickerSection(title: String, summary: String? = null, info: String? = null, color: Int, onColorChange: (Int) -> Unit) {
    SettingsPreferenceGroup {
        ColorPickerRow(title = title, summary = summary, info = info, color = color, onColorChange = onColorChange)
    }
}

/** The row+expander content of [ColorPickerSection], without its own [SettingsPreferenceGroup] wrapper —
 *  for attaching directly inside another group's card (e.g. right under a related switch). */
@Composable
fun ColorPickerRow(title: String, summary: String? = null, info: String? = null, color: Int, onColorChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    SettingsPreference(
        icon = null,
        title = title,
        summary = summary,
        info = info,
        leading = null,
        onClick = { expanded = !expanded },
        trailing = {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
        }
    )
    if (expanded) {
        ColorPickerControls(
            color = Color(color),
            onColorChange = { onColorChange(it.toArgb()) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** The gradient-capable case. */
@Composable
fun ColorFillPickerContent(
    fill: ColorFill,
    onFillChange: (ColorFill) -> Unit,
    modifier: Modifier = Modifier,
    onEyedropperCapture: (() -> ImageBitmap?)? = null
) {
    var mode by remember { mutableStateOf(fill.mode) }
    var direction by remember { mutableStateOf(fill.direction) }
    var primaryColor by remember { mutableStateOf(Color(fill.color)) }
    var secondaryColor by remember { mutableStateOf(Color(fill.secondaryColor)) }
    var editingSecondary by remember { mutableStateOf(false) }

    fun emit(
        newMode: FillMode = mode,
        newDirection: GradientDirection = direction,
        newPrimary: Color = primaryColor,
        newSecondary: Color = secondaryColor
    ) {
        onFillChange(ColorFill(newMode, newPrimary.toArgb(), newSecondary.toArgb(), newDirection))
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = mode == FillMode.SOLID, onClick = { mode = FillMode.SOLID; emit(newMode = FillMode.SOLID) })
            Text(stringResource(R.string.settings_colorfill_mode_solid), modifier = Modifier.padding(end = 16.dp))
            RadioButton(selected = mode == FillMode.GRADIENT, onClick = { mode = FillMode.GRADIENT; emit(newMode = FillMode.GRADIENT) })
            Text(stringResource(R.string.settings_colorfill_mode_gradient))
        }

        if (mode == FillMode.GRADIENT) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ColorFill(mode, primaryColor.toArgb(), secondaryColor.toArgb(), direction).asBrush())
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ColorSlotSwatch(
                    label = stringResource(R.string.settings_colorfill_color_1),
                    color = primaryColor,
                    active = !editingSecondary,
                    onClick = { editingSecondary = false }
                )
                ColorSlotSwatch(
                    label = stringResource(R.string.settings_colorfill_color_2),
                    color = secondaryColor,
                    active = editingSecondary,
                    onClick = { editingSecondary = true }
                )
            }
            SettingsPreferenceGroup {
                RadioPreference(
                    stringResource(R.string.settings_colorfill_direction_top_to_bottom),
                    GradientDirection.TOP_TO_BOTTOM,
                    direction,
                    { direction = it; emit(newDirection = it) }
                )
                RadioPreference(
                    stringResource(R.string.settings_colorfill_direction_left_to_right),
                    GradientDirection.LEFT_TO_RIGHT,
                    direction,
                    { direction = it; emit(newDirection = it) }
                )
            }
        }

        key(editingSecondary) {
            ColorPickerControls(
                color = if (editingSecondary) secondaryColor else primaryColor,
                onColorChange = { newColor ->
                    if (editingSecondary) secondaryColor = newColor else primaryColor = newColor
                    emit(
                        newPrimary = if (editingSecondary) primaryColor else newColor,
                        newSecondary = if (editingSecondary) newColor else secondaryColor
                    )
                },
                onEyedropperCapture = onEyedropperCapture
            )
        }
    }
}

@Composable
private fun ColorSlotSwatch(label: String, color: Color, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color)
                .border(
                    width = if (active) 2.dp else 1.dp,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onClick)
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
internal fun ColorPresetSwatch(color: Color, onClick: (Color) -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable { onClick(color) }
    )
}

internal fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val c = v * s
    val x = c * (1 - kotlin.math.abs((h / 60f) % 2 - 1))
    val m = v - c
    val (r, g, b) = when {
        h < 60 -> Triple(c, x, 0f)
        h < 120 -> Triple(x, c, 0f)
        h < 180 -> Triple(0f, c, x)
        h < 240 -> Triple(0f, x, c)
        h < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}

internal fun rgbToHsv(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue
    val maxV = max(r, max(g, b))
    val minV = min(r, min(g, b))
    val delta = maxV - minV
    val h = when {
        delta == 0f -> 0f
        maxV == r -> 60f * (((g - b) / delta) % 6f)
        maxV == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }.let { if (it < 0) it + 360f else it }
    val s = if (maxV == 0f) 0f else delta / maxV
    return floatArrayOf(h, s, maxV)
}

internal fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return "#%06X".format(argb and 0xFFFFFF)
}

internal fun hexToColor(text: String): Color? {
    val cleaned = text.removePrefix("#")
    if (cleaned.length != 6) return null
    return runCatching {
        val rgb = cleaned.toLong(16).toInt()
        Color(0xFF000000.toInt() or rgb)
    }.getOrNull()
}
