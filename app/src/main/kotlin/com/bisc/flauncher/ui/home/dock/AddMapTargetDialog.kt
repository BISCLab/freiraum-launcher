package com.bisc.flauncher.ui.home.dock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bisc.flauncher.R

/** Dialog to save a place as an address or as coordinates. */
@Composable
fun AddMapTargetDialog(
    onSave: (label: String, lat: Double, lon: Double, address: String) -> Unit,
    onDismiss: () -> Unit
) {
    var byAddress by remember { mutableStateOf(true) }
    var label by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var latText by remember { mutableStateOf("") }
    var lonText by remember { mutableStateOf("") }
    val lat = latText.toDoubleOrNull()
    val lon = lonText.toDoubleOrNull()
    val postalAndCity = listOf(postalCode, city).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")
    val address = listOf(street, postalAndCity, country).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ")
    val isValid = label.isNotBlank() && if (byAddress) address.isNotEmpty() else (lat != null && lon != null)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.sidebar_maps_label_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = byAddress,
                        onClick = { byAddress = true },
                        label = { Text(stringResource(R.string.settings_map_mode_address)) }
                    )
                    FilterChip(
                        selected = !byAddress,
                        onClick = { byAddress = false },
                        label = { Text(stringResource(R.string.settings_map_mode_coordinates)) }
                    )
                }
                if (byAddress) {
                    OutlinedTextField(
                        value = street,
                        onValueChange = { street = it },
                        label = { Text(stringResource(R.string.settings_map_street_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = postalCode,
                        onValueChange = { postalCode = it },
                        label = { Text(stringResource(R.string.settings_map_postal_code_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text(stringResource(R.string.settings_map_city_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text(stringResource(R.string.settings_map_country_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = latText,
                            onValueChange = { latText = it },
                            label = { Text(stringResource(R.string.sidebar_maps_lat_hint)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lonText,
                            onValueChange = { lonText = it },
                            label = { Text(stringResource(R.string.sidebar_maps_lon_hint)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.widget_notes_cancel))
                    }
                    TextButton(
                        enabled = isValid,
                        onClick = {
                            if (byAddress) onSave(label.trim(), 0.0, 0.0, address)
                            else onSave(label.trim(), lat!!, lon!!, "")
                        }
                    ) {
                        Text(stringResource(R.string.widget_notes_save))
                    }
                }
            }
        }
    }
}
