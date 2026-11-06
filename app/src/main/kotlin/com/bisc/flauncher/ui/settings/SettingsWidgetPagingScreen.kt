package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.WidgetArrowStyle
import com.bisc.flauncher.ui.home.widgets.PagingButton
import com.bisc.flauncher.ui.home.widgets.arrowIcon
import kotlin.math.roundToInt

/** The look of the paging buttons; whether a widget uses them is chosen in its own settings. */
@Composable
fun SettingsWidgetPagingScreen(navController: NavController, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val saved by viewModel.widgetPaging.collectAsState()
    var draft by rememberDraft(saved)
    SettingsSubScreen(
        title = stringResource(R.string.settings_widget_paging_title),
        navController = navController,
        onBack = onBack,
        onSave = { viewModel.setWidgetPaging(draft) },
        dirty = draft != saved
    ) {
        run {
            // The weather widget's own arrangement: upright pair for the hours, wide pair for the days.
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PagingButton(false, 30.dp, 40.dp, draft, true, {})
                        PagingButton(true, 30.dp, 40.dp, draft, true, {})
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PagingButton(false, 44.dp, 26.dp, draft, true, {})
                        PagingButton(true, 44.dp, 26.dp, draft, true, {})
                    }
                }
            }
            SettingsPreferenceGroup(title = stringResource(R.string.settings_widget_paging_symbol)) {
                WidgetArrowStyle.entries.forEach { style ->
                    SettingsPreference(
                        icon = null,
                        title = stringResource(
                            when (style) {
                                WidgetArrowStyle.CHEVRON -> R.string.settings_widget_arrow_chevron
                                WidgetArrowStyle.TRIANGLE -> R.string.settings_widget_arrow_triangle
                                WidgetArrowStyle.ARROW -> R.string.settings_widget_arrow_arrow
                            }
                        ),
                        onClick = { draft = draft.copy(arrowStyle = style) },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val (icon, turned) = arrowIcon(style, forward = true)
                                Icon(icon, contentDescription = null, modifier = if (turned) Modifier.rotate(180f) else Modifier)
                                RadioButton(selected = draft.arrowStyle == style, onClick = { draft = draft.copy(arrowStyle = style) })
                            }
                        }
                    )
                }
            }
            SettingsPreferenceGroup(
                title = stringResource(R.string.settings_widget_paging_opacity),
                info = stringResource(R.string.settings_widget_paging_opacity_info)
            ) {
                ExpandableSlider(
                    value = draft.alphaPercent.toFloat(),
                    onSettled = { draft = draft.copy(alphaPercent = it.roundToInt()) },
                    valueRange = 0f..100f,
                    valueLabel = { stringResource(R.string.settings_corner_radius_value, it.roundToInt()) }
                )
            }
            ElementAppearanceSection(
                appearance = draft.appearance ?: com.bisc.flauncher.data.model.ElementAppearance(),
                onAppearanceChange = { draft = draft.copy(appearance = it) },
                styleGroupTitle = null
            )
            SettingsPreferenceGroup(title = stringResource(R.string.settings_widgets_corner_shape_label)) {
                ExpandablePercentSlider(percent = draft.cornerRadiusPercent, onSettled = { draft = draft.copy(cornerRadiusPercent = it) })
            }
            if (draft.appearance != null) {
                SettingsPreferenceGroup {
                    SettingsPreference(
                        icon = null,
                        title = stringResource(R.string.settings_element_color_reset),
                        onClick = { draft = draft.copy(appearance = null) },
                        trailing = null
                    )
                }
            }
            Text(
                stringResource(R.string.settings_widget_paging_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
