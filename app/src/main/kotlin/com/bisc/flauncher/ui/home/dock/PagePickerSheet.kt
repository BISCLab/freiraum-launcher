package com.bisc.flauncher.ui.home.dock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.data.model.LauncherPage
import com.bisc.flauncher.ui.pageLabel
import com.bisc.flauncher.ui.theme.noRippleClickable

/** Pure picker - pick which existing page a dock slot should link to. No icon/label editing here,
 *  that lives on the page's own settings (see its Icon editor). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagePickerSheet(pages: List<LauncherPage>, onPageSelected: (LauncherPage) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
            items(pages, key = { it.id }) { page ->
                val glyph = page.iconGlyph?.let { runCatching { PageIconGlyph.valueOf(it) }.getOrNull() } ?: PageIconGlyph.forPageKind(page.kind)
                val bg = page.iconBackgroundColor?.let { Color(it) } ?: pageDefaultColor(page, glyph)
                val tint = page.iconColor?.let { Color(it) } ?: Color.White
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .noRippleClickable { onPageSelected(page) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg),
                        contentAlignment = Alignment.Center
                    ) {
                        PageIconGlyphImage(glyph = glyph, tint = tint, modifier = Modifier.size(22.dp))
                    }
                    Text(pageLabel(page, pages), modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}
