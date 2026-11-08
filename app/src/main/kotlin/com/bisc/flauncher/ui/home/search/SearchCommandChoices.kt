package com.bisc.flauncher.ui.home.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.bisc.flauncher.data.model.WebEngine
import com.bisc.flauncher.ui.theme.noRippleClickable
import com.bisc.flauncher.util.SearchIconStore

/** One search engine or AI app in a result card: its key (engine id or package), name and symbol. */
data class CommandChoice(val key: String, val label: String, val image: ImageBitmap?, val color: Color, val iconId: String, val ownIcon: Boolean)

@Composable
fun rememberEngineChoices(engines: List<WebEngine>): List<CommandChoice> {
    val context = LocalContext.current
    val stamps by SearchIconStore.stamps(context).collectAsState()
    return remember(engines, stamps) {
        engines.map { engine ->
            val id = SearchIconStore.engineId(engine.id)
            CommandChoice(engine.id, engine.name, SearchIconStore.load(context, id), Color(WebEngine.colorOf(engine)), id, id in stamps)
        }
    }
}

/** The AI apps still installed, with their own picture or the app icon as the launcher shows it
 *  (icon pack included) through [loadAppIcon]. */
@Composable
fun rememberAiChoices(packages: List<String>, loadAppIcon: ((android.content.ComponentName) -> ImageBitmap?)? = null): List<CommandChoice> {
    val context = LocalContext.current
    val stamps by SearchIconStore.stamps(context).collectAsState()
    return remember(packages, stamps) {
        val pm = context.packageManager
        packages.mapNotNull { pkg ->
            val info = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull() ?: return@mapNotNull null
            val id = SearchIconStore.aiId(pkg)
            val image = SearchIconStore.load(context, id)
                ?: loadAppIcon?.let { load -> pm.getLaunchIntentForPackage(pkg)?.component?.let { runCatching { load(it) }.getOrNull() } }
                ?: runCatching { pm.getApplicationIcon(info).toBitmap(144, 144).asImageBitmap() }.getOrNull()
            CommandChoice(pkg, pm.getApplicationLabel(info).toString(), image, Color(0xFF7E57C2), id, id in stamps)
        }
    }
}

/** The symbol of an engine or app: its picture, round, or its first letter on its color. */
@Composable
fun CommandBadge(choice: CommandChoice, size: Dp, modifier: Modifier = Modifier) {
    val image = choice.image
    if (image != null) {
        Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier.size(size).clip(CircleShape))
    } else {
        Box(modifier = modifier.size(size).clip(CircleShape).background(choice.color), contentAlignment = Alignment.Center) {
            Text(
                choice.label.trim().take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.46f).sp
            )
        }
    }
}

/**
 * A web or AI result: the first engine or app as the main line, the others below as chips to
 * pick instead, all in the same card.
 */
@Composable
internal fun CommandChoicesContent(
    choices: List<CommandChoice>,
    text: (CommandChoice) -> String,
    tint: Color,
    prominent: Boolean,
    onRun: (String) -> Unit
) {
    val primary = choices.firstOrNull() ?: return
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .noRippleClickable { onRun(primary.key) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CommandBadge(primary, 34.dp)
            Text(
                text = text(primary),
                style = if (prominent) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (choices.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                choices.drop(1).forEach { choice ->
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(tint.copy(alpha = 0.13f))
                            .noRippleClickable { onRun(choice.key) }
                            .padding(start = 5.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CommandBadge(choice, 24.dp)
                        Spacer(Modifier.width(7.dp))
                        Text(choice.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
        }
    }
}
