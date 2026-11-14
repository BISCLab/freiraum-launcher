package com.bisc.flauncher.ui.settings

import androidx.compose.foundation.layout.width
import com.bisc.flauncher.ui.theme.current
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
 * re-serializes and rewrites the *whole* file to disk - firing that on every drag frame (as plain
 * `onValueChange` does) can queue up dozens of full-file writes per second while dragging, which is
 * what made these screens feel massively laggy, and could even bury a just-made unrelated change
 * (like picking a background image) behind a long backlog of stale slider writes. The slider itself
 * still tracks the drag smoothly via local state - [label] is handed that live value so any text next
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
fun ExpandablePercentSlider(percent: Int, onSettled: (Int) -> Unit, max: Int = 100) {
    CornerPercentPicker(percent = percent.coerceAtMost(max), onChange = onSettled, max = max)
}

/**
 * Corners picked at a glance: five presets as tiles, each showing a small square with that
 * rounding, and a field for an own value. Every change takes effect at once, so the preview above
 * follows right away.
 */
@Composable
fun CornerPercentPicker(percent: Int, onChange: (Int) -> Unit, max: Int = 100) {
    val presets = listOf(0, 25, 50, 75, 100).filter { it <= max }
    var text by remember(percent) { mutableStateOf(percent.toString()) }
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            presets.forEach { value ->
                val picked = value == percent
                val frame = RoundedCornerShape(14.dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(frame)
                        .background(if (picked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(if (picked) 2.dp else 0.dp, if (picked) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent, frame)
                        .selectable(selected = picked, role = androidx.compose.ui.semantics.Role.RadioButton) { onChange(value) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // A square of 28 dp shows the rounding exactly as on the shared scale.
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(com.bisc.flauncher.ui.theme.PartCornerShape(value))
                            .border(2.dp, if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, com.bisc.flauncher.ui.theme.PartCornerShape(value))
                    )
                    Text(stringResource(R.string.settings_corner_radius_value, value), style = MaterialTheme.typography.labelMedium,
                        color = if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_corner_own_value), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }.take(3)
                    text = digits
                    digits.toIntOrNull()?.coerceIn(0, max)?.let { if (it != percent) onChange(it) }
                },
                singleLine = true,
                suffix = { Text("%") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                shape = SettingsTextFieldShape,
                modifier = Modifier.width(110.dp)
            )
        }
    }
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

/** 0..200 percent case for icon size - the icon's actual size relative to normal, 100 being
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
 *  once one is set - the same "Automatic until touched" pattern as [AutomaticColorPicker]. */
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
    if (percent != null) AutoResetLine(onClick = { onSettled(null) })
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
        trailing = { SelectedMark(value == selected) }
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
        trailing = { SelectedMark(value == selected) }
    )
}

/** Editor for a layer 2/3 appearance: fill, opacity, border, text color. [styleGroupTitle] is
 *  nullable for a caller whose own already-visible context (a page title, a related on/off switch
 *  right above) already names it - a repeated heading right underneath would just be noise, same
 *  as the rest of this section. */
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
    // None needs an actual color) the text color right underneath - no separate untitled "Color" row
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
            onColorChange = { onAppearanceChange(appearance.copy(contentColor = it)) },
            onReset = if (appearance.contentColor != null) ({ onAppearanceChange(appearance.copy(contentColor = null)) }) else null
        )
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
    // color stacked right underneath.
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
                onColorChange = { onAppearanceChange(appearance.copy(borderColor = it)) },
                onReset = if (appearance.borderColor != null) ({ onAppearanceChange(appearance.copy(borderColor = null)) }) else null
            )
        }
    }
}

/** Captions toggle + (when on) text color, as one connected card - shared by the Dock's and
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

/** The rows of [CaptionsSection] without its own card - for joining a card with other switches. */
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
                onColorChange = { onAppearanceChange(appearance.copy(contentColor = it)) },
                onReset = if (appearance.contentColor != null) ({ onAppearanceChange(appearance.copy(contentColor = null)) }) else null
            )
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
        ownGroup = ownGroup,
        onReset = if (custom != null) ({ onChange(null) }) else null
    )
}

private fun autoBorderSeed(appearance: ElementAppearance): Int = autoTextSeed(appearance)

/** A titled full-screen editor page (color pickers and other detail editors). With [onSave] it
 *  edits a draft: a Save bar commits it and closes, and leaving while [dirty] asks first. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsEditPage(
    title: String,
    onDismiss: () -> Unit,
    onSave: (() -> Unit)? = null,
    dirty: Boolean = false,
    /** Stays on top while the content below scrolls, e.g. a live preview of what is edited. */
    header: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
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
                                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(com.bisc.flauncher.R.string.settings_back))
                            }
                        }
                    )
                },
                bottomBar = {
                    if (onSave != null) SettingsSaveBar(enabled = dirty, onSave = { onSave(); onDismiss() })
                }
            ) { padding ->
                androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Column(modifier = Modifier.widthIn(max = com.bisc.flauncher.ui.MaxContentWidth).fillMaxWidth()) {
                        if (header != null) {
                            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)) { header() }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
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
                                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(com.bisc.flauncher.R.string.settings_back))
                            }
                        },
                        actions = actions
                    )
                },
                bottomBar = bottomBar
            ) { padding ->
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = com.bisc.flauncher.ui.MaxContentWidth).padding(horizontal = 16.dp),
                    content = content
                )
            }
        }
    }
}

/** One [IconShape] radio row, with a tiny live preview swatch of the shape itself. [selected] may be
 *  null (e.g. a per-app override that currently inherits the global default - nothing matches it). */
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
        trailing = { SelectedMark(value == selected) }
    )
}


/** A row that picks one of two or three short options right in place. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun <T> SettingsChoiceRow(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, info: String? = null) {
    SettingsPreference(
        icon = null,
        title = title,
        info = info,
        onClick = null,
        trailing = {
            SingleChoiceSegmentedButtonRow {
                options.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = value == selected,
                        onClick = { onSelect(value) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        icon = {},
                        label = { Text(label, maxLines = 1, softWrap = false) }
                    )
                }
            }
        }
    )
}

/** One tile of a [SettingsTileGrid]. [badge] is a short fact in the accent color (a count, the choice). */
class SettingsTile(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val summary: String? = null,
    val badge: String? = null,
    /** The area the tile leads to, for its color; null = the area of the page it is on. */
    val area: com.bisc.flauncher.ui.theme.SettingsArea? = null,
    val onClick: () -> Unit
)

/**
 * Overview pages: big tiles side by side instead of a long list. Every tile of a grid has the same
 * size, and symbol and name sit at the same height in all of them. Small tiles (three or more in a
 * line) show symbol, name and at most a short badge, never a summary, and are equally tall on
 * every page.
 */
@Composable
fun SettingsTileGrid(tiles: List<SettingsTile>, columns: Int = 2, title: String? = null, info: String? = null, compact: Boolean = false) {
    val small = columns >= 3
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (title != null) SettingsGroupLabel(title, info = info)
        androidx.compose.ui.layout.Layout(
            modifier = Modifier.fillMaxWidth(),
            content = {
                tiles.forEach { tile ->
                    Column(
                        modifier = Modifier
                            .clip(SettingsCardShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable(onClick = tile.onClick)
                            .padding(horizontal = if (small) 10.dp else 14.dp, vertical = if (compact) 10.dp else if (small) 14.dp else 16.dp),
                        horizontalAlignment = if (small) Alignment.CenterHorizontally else Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AreaSymbol(tile.icon, tile.area, if (small) 40.dp else 44.dp)
                        WholeWordsText(
                            tile.title,
                            style = if (small) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                            textAlign = if (small) androidx.compose.ui.text.style.TextAlign.Center else null,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (!small) tile.summary?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
                        }
                        tile.badge?.let {
                            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                textAlign = if (small) androidx.compose.ui.text.style.TextAlign.Center else null)
                        }
                    }
                }
            }
        ) { measurables, constraints ->
            val gap = 10.dp.roundToPx()
            val width = ((constraints.maxWidth - gap * (columns - 1)) / columns).coerceAtLeast(0)
            // One height for the whole grid: the tallest tile's, and for small tiles never below the
            // shared small height, so small tiles match from page to page.
            val tallest = measurables.maxOfOrNull { it.maxIntrinsicHeight(width) } ?: 0
            val height = if (small) maxOf(tallest, (if (compact) COMPACT_TILE_HEIGHT else SMALL_TILE_HEIGHT).roundToPx()) else tallest
            val placeables = measurables.map { it.measure(androidx.compose.ui.unit.Constraints.fixed(width, height)) }
            val rows = (placeables.size + columns - 1) / columns
            layout(constraints.maxWidth, (rows * height + (rows - 1).coerceAtLeast(0) * gap).coerceAtLeast(0)) {
                placeables.forEachIndexed { i, p -> p.place((i % columns) * (width + gap), (i / columns) * (height + gap)) }
            }
        }
    }
}

private val SMALL_TILE_HEIGHT = 124.dp
/** Tiles that only have a name (the quick access): no room kept for a short fact. */
private val COMPACT_TILE_HEIGHT = 96.dp

/**
 * A tile's name: at most two lines, and never broken inside a word. When a word is too long for the
 * width ("Startbildschirm" in a small tile), the text gets a little smaller until it fits.
 */
@Composable
private fun WholeWordsText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    textAlign: androidx.compose.ui.text.style.TextAlign?,
    modifier: Modifier = Modifier
) {
    // Shrinks step by step after each layout that broke a word, and only shows once it fits.
    var scale by remember(text, style) { androidx.compose.runtime.mutableFloatStateOf(1f) }
    var ready by remember(text, style) { mutableStateOf(false) }
    val scaled = if (scale == 1f) style else style.copy(fontSize = style.fontSize * scale, lineHeight = style.fontSize * scale * 1.25f)
    Text(
        text,
        style = scaled,
        textAlign = textAlign,
        maxLines = 2,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        modifier = modifier.drawWithContent { if (ready) drawContent() },
        onTextLayout = { layout ->
            val brokenWord = (0 until layout.lineCount - 1).any { line ->
                val end = layout.getLineEnd(line)
                end in 1 until text.length && !text[end - 1].isWhitespace() && !text[end].isWhitespace() && text[end - 1] != '-'
            }
            // Down to about two thirds of the size; past that it would not read well anyway.
            if ((brokenWord || layout.hasVisualOverflow) && scale > 0.66f) scale *= 0.94f else ready = true
        }
    )
}

/** The mark of a choice: a filled circle with a check when chosen, an empty ring when not. Same as
 *  in the choice windows (see ChoicePanel). */
@Composable
fun SelectedMark(selected: Boolean) {
    val shape = androidx.compose.foundation.shape.CircleShape
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(shape)
            .then(
                if (selected) Modifier.background(MaterialTheme.colorScheme.primary)
                else Modifier.border(2.dp, MaterialTheme.colorScheme.outline, shape)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Icon(androidx.compose.material.icons.Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
    }
}

/** One tile of a [PictureChoiceGrid]: a picture of what it means, its name, and whether it is on. */
class PictureChoice(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    /** Off shows dimmed with a crossed-out eye instead of an empty frame (for things shown or hidden). */
    val dimWhenOff: Boolean = false,
    val picture: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
)

/**
 * Choices as equal tiles with a picture each, three to a line: the one that is on with an accent
 * edge and a check, its name in the accent. [toggles] = each tile switches on its own (a switch for
 * screen readers); otherwise one of them is picked (a radio button for screen readers).
 */
@Composable
fun PictureChoiceGrid(choices: List<PictureChoice>, title: String? = null, info: String? = null, toggles: Boolean = false, pictureAspect: Float = 1f) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (title != null) SettingsGroupLabel(title, info = info)
        com.bisc.flauncher.ui.EqualGrid(count = choices.size, columns = 3) { index ->
            val choice = choices[index]
            val frame = RoundedCornerShape(18.dp)
            val stateText = stringResource(if (choice.selected) R.string.settings_choice_on else R.string.settings_choice_off)
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        if (toggles) Modifier.toggleable(value = choice.selected, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = { choice.onClick() })
                        else Modifier.selectable(selected = choice.selected, role = androidx.compose.ui.semantics.Role.RadioButton, onClick = choice.onClick)
                    )
                    .androidxSemanticsState(stateText),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(pictureAspect),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.fillMaxSize().clip(frame).background(MaterialTheme.colorScheme.surfaceContainer)) {
                        Box(
                            modifier = Modifier.fillMaxSize().alpha(if (!choice.selected && choice.dimWhenOff) 0.35f else 1f),
                            contentAlignment = Alignment.Center,
                            content = choice.picture
                        )
                    }
                    // The frame on top and outside the clip, so its corners stay whole.
                    Box(
                        Modifier.matchParentSize().border(
                            if (choice.selected) 3.dp else 1.dp,
                            if (choice.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            frame
                        )
                    )
                    if (choice.selected) {
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) { SelectedMark(true) }
                    } else if (choice.dimWhenOff) {
                        Icon(
                            androidx.compose.material.icons.Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(22.dp)
                        )
                    }
                }
                Text(
                    choice.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (choice.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
            }
        }
    }
}

/** Says "on" or "off" to screen readers, next to the role the tile already has. */
private fun Modifier.androidxSemanticsState(state: String): Modifier =
    this.semantics { stateDescription = state }

/** A symbol in black or white on a circle tinted in its area's color - the same everywhere in the
 *  settings, so the symbol keeps its contrast and the color tells the area. */
@Composable
fun AreaSymbol(icon: androidx.compose.ui.graphics.vector.ImageVector, area: com.bisc.flauncher.ui.theme.SettingsArea? = null, size: androidx.compose.ui.unit.Dp = 40.dp) {
    val color = area?.current() ?: MaterialTheme.colorScheme.primary
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.35f)).background(color.copy(alpha = if (dark) 0.30f else 0.20f)),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(size * 0.55f)) }
}
