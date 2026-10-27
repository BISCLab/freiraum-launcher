package com.bisc.flauncher.ui.settings

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

/** Human-readable name for the surface an appearance/background section's group title is for
 *  (e.g. "Widget", "Folder", "Text search"). A plain passthrough, kept so every call site uses the
 *  same pattern. */
@Composable
fun surfaceLabel(surfaceNameRes: Int): String = stringResource(surfaceNameRes)

/** Corner shape of all text fields in the settings. */
/** The one corner rounding every settings card uses (preference groups, reorderable lists, app
 *  rows, text fields), so no box rounds differently from the one next to it. */
val SettingsCardShape = RoundedCornerShape(12.dp)

val SettingsTextFieldShape = SettingsCardShape

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

/** A collapsible row for a [SettledSlider]: shows the current value (via [valueLabel]) and a chevron,
 *  and only reveals the slider once tapped, so a compact numeric setting doesn't always take up the
 *  extra vertical space in a settings list. */
@Composable
internal fun ExpandableSlider(
    value: Float,
    onSettled: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: @Composable (Float) -> String
) {
    var expanded by remember { mutableStateOf(false) }
    SettingsPreference(
        icon = null,
        title = valueLabel(value),
        onClick = { expanded = !expanded },
        trailing = {
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
    if (expanded) {
        SettledSlider(
            value = value,
            onSettled = onSettled,
            valueRange = valueRange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            label = { live -> Text(valueLabel(live), style = MaterialTheme.typography.bodyMedium) }
        )
    }
}

/** 0-100 percent case, e.g. corner roundness. */
@Composable
fun ExpandablePercentSlider(percent: Int, onSettled: (Int) -> Unit) {
    ExpandableSlider(
        value = percent.toFloat(),
        onSettled = { onSettled(it.roundToInt()) },
        valueRange = 0f..100f,
        valueLabel = { stringResource(R.string.settings_corner_radius_value, it.roundToInt()) }
    )
}

/** -100..100 signed percent case, e.g. background darken/lighten shade. */
@Composable
fun ExpandableShadePercentSlider(percent: Int, onSettled: (Int) -> Unit) {
    ExpandableSlider(
        value = percent.toFloat(),
        onSettled = { onSettled(it.roundToInt()) },
        valueRange = -100f..100f,
        valueLabel = { stringResource(R.string.settings_background_shade_value, it.roundToInt()) }
    )
}

/** 0..200 percent case for icon size — the icon's actual size relative to normal, 100 being
 *  neutral/default, smaller below and larger above. */
@Composable
fun ExpandableIconSizeSlider(percent: Int, onSettled: (Int) -> Unit) {
    ExpandableSlider(
        value = percent.toFloat(),
        onSettled = { onSettled(it.roundToInt()) },
        valueRange = 0f..200f,
        valueLabel = { stringResource(R.string.settings_icon_size_value, it.roundToInt()) }
    )
}

/** Same as [ExpandableIconSizeSlider], but [percent] is nullable (null = inherit the global
 *  default); moving the slider sets an explicit override, and a reset row clears it back to null
 *  once one is set — the same "Automatic until touched" pattern as [AutomaticColorPicker]. */
@Composable
fun ExpandableOptionalIconSizeSlider(percent: Int?, globalDefault: Int, onSettled: (Int?) -> Unit) {
    ExpandableSlider(
        value = (percent ?: globalDefault).toFloat(),
        onSettled = { onSettled(it.roundToInt()) },
        valueRange = 0f..200f,
        valueLabel = {
            if (percent == null) stringResource(R.string.settings_element_color_auto_hint)
            else stringResource(R.string.settings_icon_size_value, it.roundToInt())
        }
    )
    if (percent != null) {
        SettingsPreference(
            icon = null,
            title = stringResource(R.string.settings_element_color_reset),
            onClick = { onSettled(null) },
            trailing = null
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
    font: FontFamily? = null,
    enabled: Boolean = true,
    summary: String? = null
) {
    SettingsPreference(
        icon = null,
        title = label,
        summary = summary,
        titleFontFamily = font,
        enabled = enabled,
        onClick = { onSelect(value) },
        trailing = { RadioButton(selected = value == selected, onClick = { onSelect(value) }, enabled = enabled) }
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

/** Editor for a layer 2/3 appearance: fill, opacity, border, text color. [styleGroupTitle] is
 *  nullable for a caller whose own already-visible context (a page title, a related on/off switch
 *  right above) already names it — a repeated heading right underneath would just be noise, same
 *  as [ElementBorderSection]'s own doc on this. */
@Composable
fun ElementAppearanceSection(
    appearance: ElementAppearance,
    onAppearanceChange: (ElementAppearance) -> Unit,
    styleGroupTitle: String?,
    allowNone: Boolean = true,
    onEyedropperCapture: (() -> androidx.compose.ui.graphics.ImageBitmap?)? = null,
    /** False for full-screen surfaces (e.g. the search page), which have no edge to frame. */
    showBorder: Boolean = true
) {
    var colorEditorOpen by remember { mutableStateOf(false) }

    // One connected card for everything color-related: fill mode, then (once a fill mode other than
    // None needs an actual color) the text color right underneath — no separate untitled "Color" row
    // to tap through first, tapping "Color" itself opens its own full editor screen directly.
    SettingsPreferenceGroup(title = styleGroupTitle) {
        ElementFillMode.entries.filter { allowNone || it != ElementFillMode.NONE }.forEach { mode ->
            FillModePreference(
                value = mode,
                selected = appearance.fillMode,
                customFill = appearance.fill.asBrush(),
                onSelect = { selected ->
                    onAppearanceChange(appearance.copy(fillMode = selected))
                    if (selected == ElementFillMode.COLOR) colorEditorOpen = true
                }
            )
        }
        val textColorSeed = autoTextSeed(appearance)
        ColorPickerRow(
            title = stringResource(R.string.settings_element_text_color_label),
            summary = if (appearance.contentColor == null) stringResource(R.string.settings_element_color_auto_hint) else null,
            color = appearance.contentColor ?: textColorSeed,
            onColorChange = { onAppearanceChange(appearance.copy(contentColor = it)) }
        )
        if (appearance.contentColor != null) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_element_color_reset),
                onClick = { onAppearanceChange(appearance.copy(contentColor = null)) },
                trailing = null
            )
        }
    }
    if (colorEditorOpen) {
        var draft by rememberDraft(appearance)
        SettingsEditPage(title = stringResource(R.string.settings_background_source_color), onDismiss = { colorEditorOpen = false }, onSave = { onAppearanceChange(draft) }, dirty = draft != appearance) {
            ColorFillPickerContent(
                fill = draft.fill,
                onFillChange = { fill -> draft = draft.copy(fill = fill) },
                modifier = Modifier.fillMaxWidth(),
                onEyedropperCapture = onEyedropperCapture
            )
        }
    }

    if (appearance.fillMode != ElementFillMode.NONE) {
        SettingsPreferenceGroup(title = stringResource(R.string.settings_background_opacity_label)) {
            ExpandableSlider(
                value = appearance.opacity,
                onSettled = { onAppearanceChange(appearance.copy(opacity = it)) },
                valueRange = 0f..1f,
                valueLabel = { stringResource(R.string.settings_background_opacity_value, (it * 100).roundToInt()) }
            )
        }
    }

    // Border, all in one connected card under its own heading: on/off, then (once on) thickness and
    // color stacked right underneath — mirrors ElementBorderSection's own dedicated-screen layout.
    if (showBorder) SettingsPreferenceGroup(title = stringResource(R.string.settings_element_border_label)) {
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
        if (appearance.borderEnabled) {
            ExpandableSlider(
                value = appearance.borderWidth,
                onSettled = { onAppearanceChange(appearance.copy(borderWidth = (it * 2f).roundToInt() / 2f)) },
                valueRange = 0.5f..6f,
                valueLabel = { stringResource(R.string.settings_element_border_width_value, String.format(java.util.Locale.US, "%.1f", it)) }
            )
            val borderColorSeed = autoBorderSeed(appearance)
            ColorPickerRow(
                title = stringResource(R.string.settings_element_border_color),
                summary = if (appearance.borderColor == null) stringResource(R.string.settings_element_color_auto_hint) else null,
                color = appearance.borderColor ?: borderColorSeed,
                onColorChange = { onAppearanceChange(appearance.copy(borderColor = it)) }
            )
            if (appearance.borderColor != null) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_element_color_reset),
                    onClick = { onAppearanceChange(appearance.copy(borderColor = null)) },
                    trailing = null
                )
            }
        }
    }
}

/** Background-only half of [ElementAppearanceSection] — fill mode, color and opacity, with no
 *  group title of its own (the page it's placed on is already titled "Background", so a repeated
 *  heading would just add noise). Used by the Fill screens of the Dock, Panel, folders and search. */
@Composable
fun ElementBackgroundSection(appearance: ElementAppearance, onAppearanceChange: (ElementAppearance) -> Unit, allowNone: Boolean = true) {
    // Selecting "Color" opens its own window directly — no separate "Color" row to tap through
    // first, same as every other color picker in the app. Opacity lives inside that same window
    // rather than as its own section, since it's meaningless without a color/fill to apply to.
    var colorEditorOpen by remember { mutableStateOf(false) }
    SettingsPreferenceGroup {
        ElementFillMode.entries.filter { allowNone || it != ElementFillMode.NONE }.forEach { mode ->
            FillModePreference(
                value = mode,
                selected = appearance.fillMode,
                customFill = appearance.fill.asBrush(),
                onSelect = { selected ->
                    onAppearanceChange(appearance.copy(fillMode = selected))
                    if (selected == ElementFillMode.COLOR) colorEditorOpen = true
                }
            )
        }
    }
    if (colorEditorOpen) {
        var draft by rememberDraft(appearance)
        SettingsEditPage(title = stringResource(R.string.settings_background_source_color), onDismiss = { colorEditorOpen = false }, onSave = { onAppearanceChange(draft) }, dirty = draft != appearance) {
            ColorFillPickerContent(
                fill = draft.fill,
                onFillChange = { fill -> draft = draft.copy(fill = fill) },
                modifier = Modifier.fillMaxWidth()
            )
            SettingsPreferenceGroup(title = stringResource(R.string.settings_background_opacity_label)) {
                SettledSlider(
                    value = draft.opacity,
                    onSettled = { draft = draft.copy(opacity = it) },
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
    }
}

/** Border-only half of [ElementAppearanceSection] — enable switch, thickness and color all live
 *  in one connected card (no repeated "Border" sub-heading, since the page itself is already
 *  titled "Border"). Thickness expands inline when tapped; color opens in its own window like
 *  every other color picker in the app. Used by the Dock and Favorites Panel's own dedicated
 *  Border screens. */
@Composable
fun ElementBorderSection(appearance: ElementAppearance, onAppearanceChange: (ElementAppearance) -> Unit) {
    var thicknessExpanded by remember { mutableStateOf(false) }
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
        if (appearance.borderEnabled) {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_element_border_style_label),
                summary = stringResource(R.string.settings_element_border_width_value, String.format(java.util.Locale.US, "%.1f", appearance.borderWidth)),
                onClick = { thicknessExpanded = !thicknessExpanded },
                trailing = null
            )
            if (thicknessExpanded) {
                SettledSlider(
                    value = appearance.borderWidth,
                    onSettled = { onAppearanceChange(appearance.copy(borderWidth = (it * 2f).roundToInt() / 2f)) },
                    valueRange = 0.5f..6f,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    label = { live ->
                        Text(
                            stringResource(R.string.settings_element_border_width_value, String.format(java.util.Locale.US, "%.1f", live)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }
            val borderColorSeed = autoBorderSeed(appearance)
            val borderColorValue = appearance.borderColor ?: borderColorSeed
            ColorPickerRow(
                title = stringResource(R.string.settings_element_border_color),
                summary = if (appearance.borderColor == null) stringResource(R.string.settings_element_color_auto_hint) else null,
                color = borderColorValue,
                onColorChange = { onAppearanceChange(appearance.copy(borderColor = it)) }
            )
            if (appearance.borderColor != null) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_element_color_reset),
                    onClick = { onAppearanceChange(appearance.copy(borderColor = null)) },
                    trailing = null
                )
            }
        }
    }
}

/** Captions toggle + (when on) text color, as one connected card — shared by the Dock's and
 *  Favorites Panel's own Elements screens. The color reuses [ElementAppearance.contentColor],
 *  the same "Text color" already shown elsewhere for a row, so there's only one text-color
 *  setting per row rather than a second one specific to captions. */
@Composable
fun CaptionsSection(
    showLabels: Boolean,
    onShowLabelsChange: (Boolean) -> Unit,
    appearance: ElementAppearance,
    onAppearanceChange: (ElementAppearance) -> Unit,
    title: String? = null
) {
    SettingsPreferenceGroup(title = title) {
        CaptionsRows(showLabels, onShowLabelsChange, appearance, onAppearanceChange)
    }
}

/** The rows of [CaptionsSection] without its own card — for joining a card with other switches. */
@Composable
fun CaptionsRows(
    showLabels: Boolean,
    onShowLabelsChange: (Boolean) -> Unit,
    appearance: ElementAppearance,
    onAppearanceChange: (ElementAppearance) -> Unit
) {
    run {
        SettingsPreference(
            icon = null,
            title = stringResource(R.string.settings_element_show_labels_label),
            onClick = { onShowLabelsChange(!showLabels) },
            trailing = { Switch(checked = showLabels, onCheckedChange = onShowLabelsChange) }
        )
        if (showLabels) {
            val textColorSeed = autoTextSeed(appearance)
            val textColorValue = appearance.contentColor ?: textColorSeed
            ColorPickerRow(
                title = stringResource(R.string.settings_element_text_color_label),
                summary = if (appearance.contentColor == null) stringResource(R.string.settings_element_color_auto_hint) else null,
                color = textColorValue,
                onColorChange = { onAppearanceChange(appearance.copy(contentColor = it)) }
            )
            if (appearance.contentColor != null) {
                SettingsPreference(
                    icon = null,
                    title = stringResource(R.string.settings_element_color_reset),
                    onClick = { onAppearanceChange(appearance.copy(contentColor = null)) },
                    trailing = null
                )
            }
        }
    }
}

private fun autoTextSeed(appearance: ElementAppearance): Int {
    val light = appearance.fillMode == ElementFillMode.COLOR && androidx.compose.ui.graphics.Color(appearance.fill.color).luminance() > 0.5f
    return if (light) android.graphics.Color.BLACK else android.graphics.Color.WHITE
}

@Composable
fun AutomaticColorPicker(title: String, custom: Int?, automaticSeed: Int, onChange: (Int?) -> Unit, ownGroup: Boolean = true) {
    ColorPickerSection(
        title = title,
        summary = if (custom == null) stringResource(R.string.settings_element_color_auto_hint) else null,
        color = custom ?: automaticSeed,
        onColorChange = { onChange(it) },
        ownGroup = ownGroup
    )
    if (custom != null) {
        val resetRow = @Composable {
            SettingsPreference(
                icon = null,
                title = stringResource(R.string.settings_element_color_reset),
                onClick = { onChange(null) },
                trailing = null
            )
        }
        if (ownGroup) SettingsPreferenceGroup { resetRow() } else resetRow()
    }
}

private fun autoBorderSeed(appearance: ElementAppearance): Int = autoTextSeed(appearance)

/** A titled full-screen editor page (color pickers and other detail editors). With [onSave] it
 *  edits a draft: a Save bar commits it and closes, and leaving while [dirty] asks first. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsEditPage(title: String, onDismiss: () -> Unit, onSave: (() -> Unit)? = null, dirty: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    var confirmLeave by remember { mutableStateOf(false) }
    val guardedDismiss = { if (onSave != null && dirty) confirmLeave = true else onDismiss() }
    if (confirmLeave) {
        UnsavedChangesDialog(
            onSave = { confirmLeave = false; onSave?.invoke(); onDismiss() },
            onDiscard = { confirmLeave = false; onDismiss() },
            onKeepEditing = { confirmLeave = false }
        )
    }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = guardedDismiss,
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
                            androidx.compose.material3.IconButton(onClick = guardedDismiss) {
                                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            }
                        }
                    )
                },
                bottomBar = {
                    if (onSave != null) SettingsSaveBar(enabled = dirty, onSave = { onSave(); onDismiss() })
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

/** One [IconShape] radio row, with a tiny live preview swatch of the shape itself. [selected] may be
 *  null (e.g. a per-app override that currently inherits the global default — nothing matches it). */
@Composable
fun IconShapePreference(
    label: String,
    value: IconShape,
    selected: IconShape?,
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

