package com.bisc.flauncher.ui.home.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ContactInfo
import com.bisc.flauncher.ui.home.dock.ContactPhotoIcon
import com.bisc.flauncher.util.ContactActions
import com.bisc.flauncher.util.openContact

/**
 * Contact result tap target: photo+name header, then a grid of real, working actions — call, SMS,
 * and (only when actually installed) a WhatsApp/Telegram chat deep link. No fake voice/video-call
 * buttons: neither app exposes a public intent for that, only a documented chat link (see
 * [ContactActions]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactActionsSheet(
    contact: ContactInfo,
    loadPhoto: suspend (Long, String?) -> ImageBitmap?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val number = contact.phoneNumbers.firstOrNull()?.number

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ContactPhotoIcon(contactId = contact.id, photoUri = contact.photoUri, loadPhoto = loadPhoto, size = 56.dp)
                Spacer(Modifier.width(16.dp))
                Text(contact.displayName, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(20.dp))

            if (number != null) {
                val actions = buildList {
                    add(ContactAction(Icons.Filled.Call, stringResource(R.string.contact_action_call)) {
                        ContactActions.launch(context, ContactActions.dialIntent(number)); onDismiss()
                    })
                    add(ContactAction(Icons.AutoMirrored.Filled.Send, stringResource(R.string.contact_action_message)) {
                        ContactActions.launch(context, ContactActions.smsIntent(number)); onDismiss()
                    })
                    if (ContactActions.isPackageInstalled(context, ContactActions.WHATSAPP_PACKAGE)) {
                        add(ContactAction(Icons.AutoMirrored.Filled.Chat, stringResource(R.string.contact_action_whatsapp)) {
                            ContactActions.launch(context, ContactActions.whatsAppChatIntent(number)); onDismiss()
                        })
                    }
                    if (ContactActions.isPackageInstalled(context, ContactActions.TELEGRAM_PACKAGE)) {
                        add(ContactAction(Icons.AutoMirrored.Filled.Send, stringResource(R.string.contact_action_telegram)) {
                            ContactActions.launch(context, ContactActions.telegramChatIntent(number)); onDismiss()
                        })
                    }
                }
                actions.chunked(3).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { action -> ActionTile(action, modifier = Modifier.weight(1f)) }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            } else {
                Text(stringResource(R.string.contact_action_no_number), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
            }

            contact.email?.let { email ->
                ActionTile(
                    ContactAction(Icons.Filled.Mail, stringResource(R.string.contact_action_email)) {
                        com.bisc.flauncher.util.launchEmailTo(context, email); onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    wide = true
                )
                Spacer(Modifier.height(10.dp))
            }
            ActionTile(
                ContactAction(Icons.Filled.ContactPage, stringResource(R.string.contact_action_open_in_contacts)) {
                    openContact(context, contact); onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                wide = true
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

private data class ContactAction(val icon: ImageVector, val label: String, val onClick: () -> Unit)

@Composable
private fun ActionTile(action: ContactAction, modifier: Modifier = Modifier, wide: Boolean = false) {
    Box(
        modifier = modifier
            .height(if (wide) 52.dp else 72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = action.onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (wide) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(action.icon, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(action.label)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(action.icon, contentDescription = null)
                Spacer(Modifier.height(4.dp))
                Text(action.label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            }
        }
    }
}
