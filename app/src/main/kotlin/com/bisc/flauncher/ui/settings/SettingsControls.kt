package com.bisc.flauncher.ui.settings

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.bisc.flauncher.R
import com.bisc.flauncher.data.model.ElementAppearance
import com.bisc.flauncher.data.model.ElementFillMode
import com.bisc.flauncher.data.model.asBrush
import com.bisc.flauncher.data.settings.IconShape
import kotlin.math.roundToInt

/** Title of a layer background: layer number and surface name. */
@Composable
fun layerBackgroundLabel(layer: Int, surfaceNameRes: Int): String =
    stringResource(R.string.settings_layer_background_title, layer, stringResource(surfaceNameRes))

/** Corner shape of all text fields in the settings. */
val SettingsTextFieldShape = RoundedCornerShape(16.dp)

/**
 * A [Slider] whose value is only persisted once the drag ends, not on every intermediate frame.
 * Persisted settings live in a single shared DataStore Preferences file, and every write there
 * re-serializes and rewrites the *whole* file to disk — firing that on every drag frame (as plain
 * `onValueChange` does) can queue up dozens of full-file writes per second while dragging, which is
 * what made these screens feel massively laggy, and could even bury a just-made unrelated change
 * (like picking a background image) behind a long backlog of stale slider writes. The slider itself
 * still tracks the drag smoothly via local state — [label] is handed that live value so any text next
 * to the slider (e.g. a percentage) stays in sync while dragging too.
 */
@Composable
fun SettledSlider(
    value: Float,
    onSettled: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    label: @Composable (Float) -> Unit = {}
) {
    var local by remember(value) { mutableFloatStateOf(value) }
    Column(modifier = modifier) {
        label(local)
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onSettled(local) },
            valueRange = valueRange
        )
    }
}

/** Shared generic radio-list row, one [SettingsPreference] with a trailing [RadioButton]. */
@Composable
fun <T> RadioPreference(
    label: String,
    value: T,
    selected: T,
    onSelect: (T) -> Unit,
    font: FontFamily? = null
) {
    SettingsPreference(
        icon = null,
        title = label,
        titleFontFamily = font,
        onClick = { onSelect(value) },
        trailing = { RadioButton(selected = value == selected, onClick = { onSelect(value) }) }
    )
}

/** Localized label for one [ElementFillMode] value. */
@Composable
fun elementFillModeLabel(mode: ElementFillMode): String = when (mode) {
    ElementFillMode.NONE -> stringResource(R.string.element_fill_mode_none)
    ElementFillMode.COLOR -> stringResource(R.string.element_fill_mode_color)
}

/** Radio row for an [ElementFillMode] with a fill preview. */
@Composable
fun FillModePreference(
    value: ElementFillMode,
    selected: ElementFillMode,
    onSelect: (ElementFillMode) -> Unit,
    customFill: Brush? = null
) {
    SettingsPreference(
        icon = null,
        title = elementFillModeLabel(value),
        onClick = { onSelect(value) },
        leading = {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .let { base ->
                        when (value) {
                            ElementFillMode.NONE -> base.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            ElementFillMode.COLOR -> base
                                .background(customFill ?: Brush.linearGradient(listOf(Color.White, Color.Black)), CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        }
                    }
            )
        },
        trailing = { RadioButton(selected = value == selected, onClick = { onSelect(value) }) }
    )
}

/** Editor for a layer 2/3 appearance: fill, opacity, border, text color. */
@Composable
fun ElementAppearanceSection(
    appearance: ElementAppearance,
    onAppearanceChange: (ElementAppearance) -> Unit,
    styleGroupTitle: String,
    allowNone: Boolean = true,
    onEyedropperCapture: (() -> androidx.compose.ui.graphics.ImageBitmap?)? = null
) {
    var colorExpanded by remember { mutableStateOf(false) }
    var showLayerHelp by remember { mutableStateOf(false) }

    SettingsPreferenceGroup(title = styleGroupTitle, onInfoClick = { showLayerHelp = true }) {
        ElementFillMode.entries.filter { allowNone || it != ElementFillMode.NONE }.forEach { mode ->
            FillModePreference(
                value = mode,
                selected = appearance.fillMode,
                customFill = appearance.fill.asBrush(),
                onSelect = { selected ->
                    onAppearanceChange(appearance.copy(fillMode = selected))
                    if (selected != ElementFillMode.NONE && appearance.fillMode == ElementFillMode.NONE) colorExpanded = true
                }
            )
        }
    }

    if (appearance.fillMode != ElementFillMode.NONE) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_background_source_color),
                leading = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(appearance.fill.asBrush(), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                },
                onClick = { colorExpanded = !colorExpanded },
                trailing = null
            )
            if (colorExpanded) {
                ColorFillPickerContent(
                    fill = appearance.fill,
                    onFillChange = { fill -> onAppearanceChange(appearance.copy(fill = fill)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    onEyedropperCapture = onEyedropperCapture
                )
            }
        }

        SettingsPreferenceGroup(title = stringResource(R.string.settings_background_opacity_label)) {
            SettledSlider(
                value = appearance.opacity,
                onSettled = { onAppearanceChange(appearance.copy(opacity = it)) },
                valueRange = 0f..1f,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                label = { live ->
                    Text(
                        stringResource(R.string.settings_background_opacity_value, (live * 100).roundToInt()),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }
    }

    SettingsPreferenceGroup {
        SettingsPreference(
            icon = null,
            title = stringResource(R.string.settings_element_border_label),
            onClick = { onAppearanceChange(appearance.copy(borderEnabled = !appearance.borderEnabled)) },
            trailing = {
                Switch(
                    checked = appearance.borderEnabled,
                    onCheckedChange = { onAppearanceChange(appearance.copy(borderEnabled = it)) }
                )
            }
        )
    }

    if (appearance.borderEnabled) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_element_border_style_label)) {
            SettledSlider(
                value = appearance.borderWidth,
                onSettled = { onAppearanceChange(appearance.copy(borderWidth = (it * 2f).roundToInt() / 2f)) },
                valueRange = 0.5f..6f,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                label = { live ->
                    Text(
                        stringResource(R.string.settings_element_border_width_value, String.format(java.util.Locale.US, "%.1f", live)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }
        AutomaticColorPicker(
            title = stringResource(R.string.settings_element_border_color),
            custom = appearance.borderColor,
            automaticSeed = autoBorderSeed(appearance),
            onChange = { onAppearanceChange(appearance.copy(borderColor = it)) }
        )
    }

    AutomaticColorPicker(
        title = stringResource(R.string.settings_element_text_color_label),
        custom = appearance.contentColor,
        automaticSeed = autoTextSeed(appearance),
        onChange = { onAppearanceChange(appearance.copy(contentColor = it)) }
    )

    if (showLayerHelp) {
        LayerExplainerSheet(onDismiss = { showLayerHelp = false })
    }
}

private fun autoTextSeed(appearance: ElementAppearance): Int {
    val light = appearance.fillMode == ElementFillMode.COLOR && androidx.compose.ui.graphics.Color(appearance.fill.color).luminance() > 0.5f
    return if (light) android.graphics.Color.BLACK else android.graphics.Color.WHITE
}

@Composable
private fun AutomaticColorPicker(title: String, custom: Int?, automaticSeed: Int, onChange: (Int?) -> Unit) {
    ColorPickerSection(
        title = title,
        summary = if (custom == null) stringResource(R.string.settings_element_color_auto_hint) else null,
        color = custom ?: automaticSeed,
        onColorChange = { onChange(it) }
    )
    if (custom != null) {
        SettingsPreferenceGroup {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_element_color_reset),
                onClick = { onChange(null) },
                trailing = null
            )
        }
    }
}

private fun autoBorderSeed(appearance: ElementAppearance): Int = autoTextSeed(appearance)

/** Wraps [content] in a titled, scrollable [ModalBottomSheet]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsEditSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

/** Full-screen counterpart of [SettingsEditSheet]. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsEditPage(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val view = androidx.compose.ui.platform.LocalView.current
        val lightBackground = MaterialTheme.colorScheme.background.luminance() > 0.5f
        androidx.compose.runtime.SideEffect {
            (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.let { window ->
                androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = lightBackground
                    isAppearanceLightNavigationBars = lightBackground
                }
            }
        }
        androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            androidx.compose.material3.Scaffold(
                topBar = {
                    androidx.compose.material3.TopAppBar(
                        windowInsets = settingsTopBarInsets(),
                        title = { Text(title) },
                        navigationIcon = {
                            androidx.compose.material3.IconButton(onClick = onDismiss) {
                                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            }
                        }
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = content
                )
            }
        }
    }
}

/** Insets for a settings top bar: the status bar as usual, or. */
@Composable
fun settingsTopBarInsets(): androidx.compose.foundation.layout.WindowInsets =
    androidx.compose.foundation.layout.WindowInsets.systemBars
        .union(androidx.compose.foundation.layout.WindowInsets.displayCutout)
        .only(androidx.compose.foundation.layout.WindowInsetsSides.Horizontal + androidx.compose.foundation.layout.WindowInsetsSides.Top)

/** Full-screen dialog page whose content does *not* scroll as a whole. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsFixedDialog(
    title: String,
    onDismiss: () -> Unit,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val view = androidx.compose.ui.platform.LocalView.current
        val lightBackground = MaterialTheme.colorScheme.background.luminance() > 0.5f
        androidx.compose.runtime.SideEffect {
            (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.let { window ->
                androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = lightBackground
                    isAppearanceLightNavigationBars = lightBackground
                }
            }
        }
        androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            androidx.compose.material3.Scaffold(
                modifier = Modifier.imePadding(),
                topBar = {
                    androidx.compose.material3.TopAppBar(
                        windowInsets = settingsTopBarInsets(),
                        title = { Text(title) },
                        navigationIcon = {
                            androidx.compose.material3.IconButton(onClick = onDismiss) {
                                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            }
                        },
                        actions = actions
                    )
                },
                bottomBar = bottomBar
            ) { padding ->
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
                    content = content
                )
            }
        }
    }
}

private val Layer1Tone = Color(0xFF64748B)
private val Layer2Tone = Color(0xFF94A3B8)
private val Layer3Tone = Color(0xFFE2E8F0)

/** Explanation of the three layers. */
@Composable
fun LayerExplainerSheet(onDismiss: () -> Unit) {
    SettingsEditSheet(title = stringResource(R.string.settings_layer_help_title), onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Layer1Tone)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Layer2Tone)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Layer3Tone)
                )
            }
        }
        LayerExplainerRow(Layer1Tone, stringResource(R.string.settings_layer_1_title), stringResource(R.string.settings_layer_1_description))
        LayerExplainerRow(Layer2Tone, stringResource(R.string.settings_layer_2_title), stringResource(R.string.settings_layer_2_description))
        LayerExplainerRow(Layer3Tone, stringResource(R.string.settings_layer_3_title), stringResource(R.string.settings_layer_3_description))
    }
}

@Composable
private fun LayerExplainerRow(tone: Color, title: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(14.dp)
                .clip(CircleShape)
                .background(tone)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** One [IconShape] radio row, with a tiny live preview swatch of the shape itself. */
@Composable
fun IconShapePreference(
    label: String,
    value: IconShape,
    selected: IconShape,
    onSelect: (IconShape) -> Unit
) {
    val shape = when (value) {
        IconShape.CIRCLE -> CircleShape
        IconShape.ROUNDED_SQUARE -> RoundedCornerShape(6.dp)
    }
    SettingsPreference(
        icon = null,
        title = label,
        onClick = { onSelect(value) },
        leading = {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary, shape)
            )
        },
        trailing = { RadioButton(selected = value == selected, onClick = { onSelect(value) }) }
    )
}
