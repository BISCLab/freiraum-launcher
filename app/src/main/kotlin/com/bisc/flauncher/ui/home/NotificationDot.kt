package com.bisc.flauncher.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** The packages that get a dot right now; empty while dots are off or there is no access. */
val LocalNotificationDots = staticCompositionLocalOf<Set<String>> { emptySet() }

/** A small dot in the accent color at the top end of an app icon while [packageName] has a
 *  notification. Nothing at all otherwise. */
@Composable
fun Modifier.notificationDot(packageName: String?): Modifier {
    if (packageName == null || packageName !in LocalNotificationDots.current) return this
    val color = MaterialTheme.colorScheme.primary
    return this.drawWithContent {
        drawContent()
        val radius = (size.minDimension * 0.11f).coerceAtLeast(4.dp.toPx())
        val center = Offset(size.width - radius * 0.9f, radius * 0.9f)
        drawCircle(Color.White, radius + 1.5.dp.toPx(), center)
        drawCircle(color, radius, center)
    }
}
