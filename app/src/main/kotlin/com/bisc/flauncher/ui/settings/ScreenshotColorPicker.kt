package com.bisc.flauncher.ui.settings

import androidx.core.graphics.get
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bisc.flauncher.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Picks a color from a picture, in practice a screenshot of the open keyboard: the launcher can't
 *  see another app's pixels on the screen, but the system's own screenshot it can read when the
 *  person chooses it. Tap or drag on the picture; the ring shows the color under the finger. */
@Composable
internal fun ScreenshotColorPicker(uri: Uri, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var touch by remember { mutableStateOf<Offset?>(null) }
    LaunchedEffect(uri) {
        val loaded = withContext(Dispatchers.IO) { runCatching { decodeForPicking(context, uri) }.getOrNull() }
        if (loaded == null) failed = true else bitmap = loaded
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Text(
                    stringResource(if (failed) R.string.settings_screenshot_picker_failed else R.string.settings_screenshot_picker_hint),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val image = bitmap
                    if (image != null) {
                        val imageBitmap = remember(image) { image.asImageBitmap() }
                        // Where the picture sits in the box, fitted whole and centered.
                        var fit by remember { mutableStateOf(Fit(1f, Offset.Zero)) }
                        fun pickAt(position: Offset) {
                            val x = ((position.x - fit.offset.x) / fit.scale).toInt()
                            val y = ((position.y - fit.offset.y) / fit.scale).toInt()
                            if (x in 0 until image.width && y in 0 until image.height) {
                                touch = position
                                picked = image[x, y] or 0xFF000000.toInt()
                            }
                        }
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(image) { detectTapGestures { pickAt(it) } }
                                .pointerInput(image) { detectDragGestures(onDragStart = { pickAt(it) }) { change, _ -> pickAt(change.position) } }
                        ) {
                            val scale = minOf(size.width / image.width, size.height / image.height)
                            val drawn = androidx.compose.ui.geometry.Size(image.width * scale, image.height * scale)
                            val offset = Offset((size.width - drawn.width) / 2f, (size.height - drawn.height) / 2f)
                            fit = Fit(scale, offset)
                            drawImage(
                                imageBitmap,
                                dstOffset = IntOffset(offset.x.toInt(), offset.y.toInt()),
                                dstSize = IntSize(drawn.width.toInt(), drawn.height.toInt())
                            )
                            val at = touch
                            val color = picked
                            if (at != null && color != null) {
                                // The ring sits above the finger, so the finger doesn't hide it.
                                val center = Offset(at.x, (at.y - 72.dp.toPx()).coerceAtLeast(36.dp.toPx()))
                                drawCircle(Color(color), radius = 30.dp.toPx(), center = center)
                                drawCircle(Color.White, radius = 30.dp.toPx(), center = center, style = Stroke(3.dp.toPx()))
                                drawCircle(Color.Black, radius = 32.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
                                drawCircle(Color.White, radius = 4.dp.toPx(), center = at, style = Stroke(2.dp.toPx()))
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val color = picked
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (color != null) Color(color) else Color.Transparent)
                            .border(1.dp, Color.White, CircleShape)
                    )
                    Text(
                        if (color != null) "#%06X".format(color and 0xFFFFFF) else "",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
                    Button(onClick = { color?.let(onPick); onDismiss() }, enabled = color != null) {
                        Text(stringResource(R.string.settings_screenshot_picker_take))
                    }
                }
            }
        }
    }
}

private data class Fit(val scale: Float, val offset: Offset)

/** Loads the picture small enough to keep in memory, still sharp enough to hit a key's edge. */
private fun decodeForPicking(context: Context, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}
