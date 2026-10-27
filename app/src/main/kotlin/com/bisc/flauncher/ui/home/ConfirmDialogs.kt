package com.bisc.flauncher.ui.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bisc.flauncher.R

/** Confirms before a long-press removal (element, folder, ...) takes effect. */
@Composable
fun ConfirmRemoveDialog(label: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sidebar_remove_confirm_title)) },
        text = { Text(stringResource(R.string.sidebar_remove_confirm_message, label)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.sidebar_remove_confirm_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.widget_notes_cancel)) }
        }
    )
}
