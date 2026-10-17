package com.bisc.flauncher.ui.home.dock

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Contact photo, loaded lazily — falls back to a generic person icon while loading or if the
 *  contact has none. Shared by [ContactPickerSheet], the search overlay's contact result, and any
 *  CONTACT-type [com.bisc.flauncher.data.model.RowEntry] cell (Dock/Panel). */
@Composable
fun ContactPhotoIcon(contactId: Long, photoUri: String?, loadPhoto: suspend (Long, String?) -> ImageBitmap?, size: Dp = 28.dp) {
    var photo by remember(contactId) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(contactId, photoUri) { photo = loadPhoto(contactId, photoUri) }
    val loaded = photo
    if (loaded != null) {
        Image(bitmap = loaded, contentDescription = null, modifier = Modifier.size(size).clip(CircleShape))
    } else {
        Icon(Icons.Filled.Person, contentDescription = null, tint = LocalContentColor.current.copy(alpha = 0.7f), modifier = Modifier.size(size))
    }
}
