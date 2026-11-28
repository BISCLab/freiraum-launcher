package com.bisc.flauncher.ui.settings

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.bisc.flauncher.R
import com.bisc.flauncher.ui.LocalStackMarkers
import com.bisc.flauncher.ui.StackMarkers
import kotlin.math.abs
import kotlin.math.roundToInt

/** The three cards of a page's settings. */
enum class StackCard { SCREEN, ELEMENTS, ADJUST }

/** One part of a page: its number, what the list says about it, and its options. */
class StackElement(
    val id: String,
    val number: Int,
    val title: String,
    /** Under the name in the list, e.g. "Off" or "Own look"; null = nothing. */
    val summary: String? = null,
    /** False = switched off: drawn faint in the replica and the list. */
    val shown: Boolean = true,
    /** The replica's state the part is seen in; null = the normal one. */
    val state: String? = null,
    /** Keys of its draft fields: a dot marks the part while one of them is changed. */
    val draftKeys: List<String> = emptyList(),
    /** Where its number sits, as a fraction of its bounds (0,0 = top start). */
    val anchor: Offset = Offset(0f, 0f),
    /** "Automatic" or "Own look" with a reset under the sample; null = neither. */
    val ownLook: Boolean? = null,
    val onResetLook: () -> Unit = {},
    /** Said instead of "Automatic" while [ownLook] is false, e.g. "Like all folders". */
    val autoLabel: String? = null,
    /** In lists of many (apps, folders): its own picture instead of the number. */
    val leading: (@Composable () -> Unit)? = null,
    /** A heading over it in the list, shown where it changes from the one before. */
    val section: String? = null,
    /** False = not in the list right now (a search hides it), but still there to choose. */
    val inList: Boolean = true,
    /** The part in small on top of its options; null = none. */
    val sample: (@Composable () -> Unit)? = null,
    val options: @Composable ColumnScope.() -> Unit
)

/** The same eight colors on every page, number 0 (the page) first. */
private val NumberColors = listOf(
    Color(0xFF546E7A), Color(0xFFE53935), Color(0xFFEF6C00), Color(0xFF2E7D32),
    Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFF00897B), Color(0xFFD81B60)
)

internal fun stackNumberColor(number: Int): Color = NumberColors[number.mod(NumberColors.size)]

// ---- Draft ----

/** Everything changed in a stack, kept until Save. Lives as long as the page (survives turning the
 *  device); only what differs from the stored value is kept. */
class StackDraft : ViewModel() {
    private val edits = mutableStateMapOf<String, Any?>()
    private val savers = HashMap<String, (Any?) -> Unit>()
    private val together = LinkedHashMap<String, () -> Unit>()

    val dirty: Boolean get() = edits.isNotEmpty()
    fun changed(keys: List<String>): Boolean = keys.any { it in edits }

    @Suppress("UNCHECKED_CAST")
    internal fun <T> valueOf(key: String, saved: T): T = if (key in edits) edits[key] as T else saved
    internal fun <T> put(key: String, saved: T, value: T) { if (value == saved) edits.remove(key) else edits[key] = value }
    internal fun register(key: String, save: (Any?) -> Unit) { savers[key] = save }

    /** For values that are stored as one thing (a category): runs at Save, after the single savers,
     *  while the draft still holds every change. */
    fun saveTogether(block: () -> Unit) { together[""] = block }

    /** Like [saveTogether], one of many (each category in a list of them). */
    fun saveTogether(id: String, block: () -> Unit) { together[id] = block }

    fun save() {
        edits.toMap().forEach { (key, value) -> savers[key]?.invoke(value) }
        together.values.toList().forEach { it() }
        // Written once: an editor still open registers again, one closed has nothing left to write.
        together.clear()
        edits.clear()
    }
}

/** One value in a [StackDraft]: reads the draft, or the stored value while it is untouched. */
class DraftField<T>(private val draft: StackDraft, val key: String, private val saved: T) {
    var value: T
        get() = draft.valueOf(key, saved)
        set(v) = draft.put(key, saved, v)
}

@Suppress("UNCHECKED_CAST")
@Composable
fun <T> StackDraft.field(key: String, saved: T, save: (T) -> Unit): DraftField<T> {
    register(key) { save(it as T) }
    return DraftField(this, key, saved)
}

// ---- Pages inside the Adjust card ----

/** A detail editor (a color, a gradient, an image) shown inside the Adjust card instead of over
 *  the whole screen. What is changed on it goes into the draft at once. */
class StackSubPage(
    /** Always the latest from the editor, read only where the page is drawn. */
    val header: androidx.compose.runtime.State<(@Composable () -> Unit)?>,
    val content: androidx.compose.runtime.State<@Composable ColumnScope.() -> Unit>
) {
    var title by mutableStateOf("")
    var onBack: () -> Unit = {}
}

class StackSubPageHost {
    val pages = mutableStateListOf<StackSubPage>()
    val top: StackSubPage? get() = pages.lastOrNull()
}

/** Set inside a stack's Adjust card: [SettingsEditPage] opens there instead of as a window. */
val LocalStackSubPageHost = staticCompositionLocalOf<StackSubPageHost?> { null }

/** [SettingsEditPage] inside a stack: its changes go into the draft right away, back closes it. */
@Composable
internal fun HostedEditPage(
    host: StackSubPageHost,
    title: String,
    onDismiss: () -> Unit,
    onSave: (() -> Unit)?,
    dirty: Boolean,
    header: (@Composable () -> Unit)?,
    content: @Composable ColumnScope.() -> Unit
) {
    val latestHeader = androidx.compose.runtime.rememberUpdatedState(header)
    val latestContent = androidx.compose.runtime.rememberUpdatedState(content)
    val page = remember { StackSubPage(latestHeader, latestContent) }
    page.title = title
    page.onBack = onDismiss
    LaunchedEffect(dirty) { if (dirty) onSave?.invoke() }
    DisposableEffect(host) {
        host.pages.add(page)
        onDispose { host.pages.remove(page) }
    }
}

// ---- The stack ----

/**
 * A page's settings as a stack of three cards: the page as a replica with numbered parts, the list
 * of parts, and the options of the chosen one. Cards that are not open show as strips with their
 * name: earlier ones on the left, later ones on the right. Wide screens show two or three side by
 * side. One draft for everything, saved with one button.
 */
@Composable
fun SettingsCardStackScreen(
    title: String,
    navController: NavController,
    onBack: () -> Unit,
    draft: StackDraft,
    elements: List<StackElement>,
    /** The replica's states, id to label; empty = only the normal one. The first is the normal one. */
    states: List<Pair<String, String>> = emptyList(),
    /** Opened from outside on one part: its options first. */
    startElement: String? = null,
    /** The card shown first when no part is asked for; a long list starts on Elements. */
    firstCard: StackCard = StackCard.SCREEN,
    /** On top of the list: a search, a way to make a new one. */
    listHeader: (@Composable () -> Unit)? = null,
    replica: @Composable (state: String?, markers: StackMarkers) -> Unit
) {
    var card by rememberSaveable { mutableStateOf(if (startElement != null) StackCard.ADJUST else firstCard) }
    var selectedId by rememberSaveable { mutableStateOf(startElement ?: elements.first().id) }
    var stateId by rememberSaveable { mutableStateOf(elements.find { it.id == startElement }?.state) }
    val selected = elements.find { it.id == selectedId } ?: elements.first()
    val host = remember { StackSubPageHost() }
    val markers = remember { StackMarkers() }
    markers.ghosted = elements.filter { !it.shown }.map { it.id }.toSet()

    val choose: (StackElement) -> Unit = { element ->
        selectedId = element.id
        // Show the part: its own state, or the normal one when it is not there in this state.
        val state = when {
            element.number == 0 -> stateId
            element.state != null -> element.state
            element.id in markers.bounds -> stateId
            else -> null
        }
        if (state != stateId) stateId = state
        host.pages.toList().forEach { it.onBack() }
        card = StackCard.ADJUST
    }

    SettingsSubScreen(title = title, navController = navController, onBack = onBack, onSave = draft::save, dirty = draft.dirty, scrollable = false) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val columns = when {
                maxWidth >= 840.dp -> 3
                maxWidth >= 600.dp -> 2
                else -> 1
            }
            BackHandler(enabled = host.top != null || (columns == 1 && card != StackCard.SCREEN) || (columns == 2 && card == StackCard.ADJUST)) {
                when {
                    host.top != null -> host.top?.onBack?.invoke()
                    columns == 2 -> card = StackCard.ELEMENTS
                    else -> card = StackCard.SCREEN
                }
            }
            val screen = @Composable {
                ScreenCard(
                    elements = elements,
                    selected = selected,
                    states = states,
                    stateId = stateId,
                    onState = { stateId = it },
                    markers = markers,
                    onChoose = choose,
                    replica = { androidx.compose.runtime.CompositionLocalProvider(LocalStackSelected provides selected.id) { replica(stateId, markers) } }
                )
            }
            val list = @Composable { ElementsCard(elements, selected, draft, listHeader, onChoose = choose) }
            val adjust = @Composable {
                androidx.compose.runtime.CompositionLocalProvider(LocalStackSubPageHost provides host) {
                    AdjustCard(selected, changed = draft.changed(selected.draftKeys), host = host)
                }
            }
            val gap = 8.dp
            when (columns) {
                3 -> Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Box(Modifier.weight(1f)) { screen() }
                    Box(Modifier.weight(0.8f)) { list() }
                    Box(Modifier.weight(1.2f)) { adjust() }
                }
                2 -> Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Box(Modifier.weight(1f)) { screen() }
                    CardRow(
                        cards = listOf(StackCard.ELEMENTS, StackCard.ADJUST),
                        current = if (card == StackCard.SCREEN) StackCard.ELEMENTS else card,
                        selected = selected,
                        onOpen = { card = it },
                        modifier = Modifier.weight(1f),
                        content = { if (it == StackCard.ELEMENTS) list() else adjust() }
                    )
                }
                else -> CardRow(
                    cards = StackCard.entries,
                    current = card,
                    selected = selected,
                    onOpen = { card = it },
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    content = { when (it) { StackCard.SCREEN -> screen(); StackCard.ELEMENTS -> list(); StackCard.ADJUST -> adjust() } }
                )
            }
        }
    }
}

/** Whether the system asks for no animations. */
@Composable
private fun animationsOff(): Boolean {
    val context = LocalContext.current
    return remember {
        android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** The open card full width, the others as strips; every card stays composed, so an open color
 *  editor or a scroll position is still there when its card comes back. */
@Composable
private fun CardRow(
    cards: List<StackCard>,
    current: StackCard,
    selected: StackElement,
    onOpen: (StackCard) -> Unit,
    modifier: Modifier,
    content: @Composable (StackCard) -> Unit
) {
    val index = cards.indexOf(current)
    val position by animateFloatAsState(
        targetValue = index.toFloat(),
        animationSpec = if (animationsOff()) snap() else tween(250),
        label = "stackCard"
    )
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        cards.take(index).forEach { CardStrip(it, selected, onOpen) }
        // Horizontal swipes away from the edges (the edges belong to the system's back gesture).
        var dragged by remember { mutableStateOf(0f) }
        val edge = with(LocalDensity.current) { 24.dp.toPx() }
        val threshold = with(LocalDensity.current) { 72.dp.toPx() }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .pointerInput(index, cards) {
                    var startX = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { startX = it.x; dragged = 0f },
                        onDragEnd = {
                            val fromEdge = startX < edge || startX > size.width - edge
                            if (!fromEdge && abs(dragged) > threshold) {
                                val next = (index + if (dragged < 0) 1 else -1).coerceIn(0, cards.lastIndex)
                                if (next != index) onOpen(cards[next])
                            }
                            dragged = 0f
                        },
                        onDragCancel = { dragged = 0f },
                        onHorizontalDrag = { _, amount -> dragged += amount }
                    )
                }
        ) {
            cards.forEachIndexed { i, c ->
                val offset = i - position
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                // Only the open card and, while sliding, its neighbour are placed.
                                if (abs(offset) < 1f) placeable.place(IntOffset((offset * placeable.width).roundToInt(), 0))
                            }
                        }
                ) { content(c) }
            }
        }
        cards.drop(index + 1).forEach { CardStrip(it, selected, onOpen) }
    }
}

@Composable
private fun cardName(card: StackCard): String = when (card) {
    StackCard.SCREEN -> stringResource(R.string.stack_card_screen)
    StackCard.ELEMENTS -> stringResource(R.string.stack_card_elements)
    StackCard.ADJUST -> stringResource(R.string.stack_card_adjust)
}

/** A closed card: a strip with its name turned on its side. */
@Composable
private fun CardStrip(card: StackCard, selected: StackElement, onOpen: (StackCard) -> Unit) {
    val name = cardName(card)
    val label = if (card == StackCard.ADJUST) "$name · ${selected.title}" else name
    val fontScale = LocalDensity.current.fontScale
    val description = stringResource(R.string.stack_open_card, label)
    Box(
        modifier = Modifier
            .width((32 * fontScale.coerceIn(1f, 1.25f)).dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = { onOpen(card) })
            .semantics { role = Role.Button; contentDescription = description }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (card == StackCard.ADJUST) NumberBadge(selected.number, size = 22.dp)
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.sideways()
            )
        }
    }
}

/** Text turned a quarter to the right, reading from top to bottom. */
private fun Modifier.sideways(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(Constraints(maxWidth = if (constraints.hasBoundedHeight) constraints.maxHeight else Constraints.Infinity))
    layout(placeable.height, placeable.width) {
        placeable.placeWithLayer((placeable.height - placeable.width) / 2, (placeable.width - placeable.height) / 2) { rotationZ = 90f }
    }
}

/** A part's number in its color. */
@Composable
internal fun NumberBadge(number: Int, size: Dp = 24.dp, dot: Boolean = false) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(stackNumberColor(number))
                .border(1.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.5f).sp, maxLines = 1)
        }
        if (dot) Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(9.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .border(1.dp, Color.White, CircleShape)
        )
    }
}

// ---- Card 1: the screen ----

@Composable
private fun ScreenCard(
    elements: List<StackElement>,
    selected: StackElement,
    states: List<Pair<String, String>>,
    stateId: String?,
    onState: (String?) -> Unit,
    markers: StackMarkers,
    onChoose: (StackElement) -> Unit,
    replica: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (states.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                states.forEachIndexed { i, (id, label) ->
                    val value = if (i == 0) null else id
                    FilterChip(selected = stateId == value, onClick = { onState(value) }, label = { Text(label, maxLines = 1) })
                }
            }
        }
        Replica(
            elements = elements.filter { it.state == null || it.state == stateId },
            page = elements.first(),
            selected = selected,
            markers = markers,
            onChoose = onChoose,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            content = replica
        )
    }
}

/**
 * The page built from the launcher's own parts at the real screen size, drawn smaller, with every
 * touch held back; the numbers sit on top at full size. A tap on a part or its number opens its
 * options; a tap on free space opens the page's.
 */
@Composable
private fun Replica(
    elements: List<StackElement>,
    page: StackElement,
    selected: StackElement,
    markers: StackMarkers,
    onChoose: (StackElement) -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    // The whole window, bars included, like the page on the home screen.
    val configuration = LocalConfiguration.current
    val root = androidx.compose.ui.platform.LocalView.current.rootView
    val density0 = LocalDensity.current
    val screenW = if (root.width > 0) with(density0) { root.width.toDp() } else configuration.screenWidthDp.dp
    val screenH = if (root.height > 0) with(density0) { root.height.toDp() } else configuration.screenHeightDp.dp + 48.dp
    BoxWithConstraints(modifier = modifier.padding(horizontal = 14.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
        val scale = minOf(maxWidth / screenW, maxHeight / screenH)
        val w = screenW * scale
        val h = screenH * scale
        val phoneShape = RoundedCornerShape(22.dp)
        val selectedColor = stackNumberColor(selected.number)
        val outline = MaterialTheme.colorScheme.outline
        val density = LocalDensity.current
        Box(
            modifier = Modifier
                .size(w, h)
                .onGloballyPositioned { markers.container = it }
        ) {
            // The page itself.
            Box(
                Modifier
                    .size(w, h)
                    .clip(phoneShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Layout(content = {
                    androidx.compose.runtime.CompositionLocalProvider(LocalStackMarkers provides markers) { content() }
                }) { measurables, _ ->
                    val fullW = with(density) { screenW.roundToPx() }
                    val fullH = with(density) { screenH.roundToPx() }
                    val placeables = measurables.map { it.measure(Constraints.fixed(fullW, fullH)) }
                    layout(with(density) { w.roundToPx() }, with(density) { h.roundToPx() }) {
                        placeables.forEach {
                            it.placeWithLayer(0, 0) {
                                scaleX = scale; scaleY = scale
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                            }
                        }
                    }
                }
            }
            // Frames: faint parts dashed, the chosen one in its color.
            Box(
                Modifier
                    .size(w, h)
                    .border(2.dp, outline, phoneShape)
                    .drawBehind {
                        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                        elements.forEach { e ->
                            val r = markers.bounds[e.id] ?: return@forEach
                            if (!e.shown) drawRoundRect(outline, r.topLeft, r.size, CornerRadius(6.dp.toPx()), style = Stroke(1.5.dp.toPx(), pathEffect = dash))
                        }
                        val r = markers.bounds[selected.id]
                        if (r != null && selected !== page) {
                            val pad = 3.dp.toPx()
                            drawRoundRect(selectedColor, Offset(r.left - pad, r.top - pad), androidx.compose.ui.geometry.Size(r.width + 2 * pad, r.height + 2 * pad), CornerRadius(8.dp.toPx()), style = Stroke(2.dp.toPx()))
                        }
                    }
                    // Lies over the page, so no touch reaches it: a tap chooses a part.
                    .pointerInput(elements) {
                        detectTapGestures { at ->
                            val hit = elements
                                .filter { it !== page }
                                .mapNotNull { e -> markers.bounds[e.id]?.takeIf { it.inflate(6.dp.toPx()).contains(at) }?.let { e to it } }
                                .minByOrNull { (_, r) -> r.width * r.height }
                            onChoose(hit?.first ?: page)
                        }
                    }
            )
            // The numbers, the page's on the corner of the frame.
            val badge = 24.dp
            val half = with(density) { (badge / 2).toPx() }
            elements.forEach { e ->
                val at = if (e === page) Offset(with(density) { w.toPx() } - half, half)
                else markers.bounds[e.id]?.let { r -> Offset(r.left + r.width * e.anchor.x, r.top + r.height * e.anchor.y) } ?: return@forEach
                val x = (at.x - half).coerceIn(-half, with(density) { w.toPx() } - half)
                val y = (at.y - half).coerceIn(-half, with(density) { h.toPx() } - half)
                val name = e.title
                Box(
                    modifier = Modifier
                        .layout { m, c -> val p = m.measure(c); layout(p.width, p.height) { p.place(x.roundToInt() - (p.width / 2 - half.roundToInt()), y.roundToInt() - (p.height / 2 - half.roundToInt())) } }
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(onClick = { onChoose(e) })
                        .semantics { contentDescription = "${e.number}, $name" },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.then(if (!e.shown) Modifier.graphicsLayerAlpha(0.6f) else Modifier)) { NumberBadge(e.number, badge) }
                }
            }
        }
    }
}

private fun Modifier.graphicsLayerAlpha(alpha: Float): Modifier = this.then(Modifier.layout { m, c ->
    val p = m.measure(c)
    layout(p.width, p.height) { p.placeWithLayer(0, 0) { this.alpha = alpha } }
})

private fun Rect.inflate(by: Float) = Rect(left - by, top - by, right + by, bottom + by)

// ---- Card 2: the list of parts ----

@Composable
private fun ElementsCard(elements: List<StackElement>, selected: StackElement, draft: StackDraft, header: (@Composable () -> Unit)?, onChoose: (StackElement) -> Unit) {
    val shown = elements.filter { it.inList }
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item(key = "intro") {
            if (header != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) { header() }
            } else {
                Text(
                    stringResource(R.string.stack_elements_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
        shown.forEachIndexed { i, e ->
            val section = e.section
            if (section != null && section != shown.getOrNull(i - 1)?.section) {
                item(key = "section_$section") {
                    Text(
                        section,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp)
                    )
                }
            }
            item(key = e.id) {
                val chosen = e === selected
                val changed = draft.changed(e.draftKeys)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (chosen) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                        .clickable(onClick = { onChoose(e) })
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val lead = e.leading
                    if (lead != null) Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { lead() }
                    else NumberBadge(e.number, 28.dp, dot = changed)
                    Column(Modifier.weight(1f)) {
                        Text(
                            e.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (e.shown) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        e.summary?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                    // A changed one in a list of pictures: the dot sits here.
                    if (lead != null && changed) Box(Modifier.size(9.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** The part shown in the replica right now, for replicas that draw only the chosen one. */
val LocalStackSelected = staticCompositionLocalOf { "" }

/** Tabs inside one part's options, for parts with several sides (a folder: folder, icon, window,
 *  contents). [start] picks the first one shown. */
@Composable
internal fun ColumnScope.StackTabs(tabs: List<Pair<String, @Composable ColumnScope.() -> Unit>>, start: Int = 0) {
    var tab by rememberSaveable { mutableStateOf(start.coerceIn(0, tabs.lastIndex)) }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { i, (label, _) ->
            FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(label, maxLines = 1) })
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) { tabs[tab].second(this) }
}

/** A search field on top of a long list. */
@Composable
internal fun StackSearchField(query: String, onQuery: (String) -> Unit, hint: String) {
    androidx.compose.material3.OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        placeholder = { Text(hint) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

/** A row that makes a new one, on top of a list. */
@Composable
internal fun StackAddRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

// ---- Card 3: the chosen part's options ----

@Composable
private fun AdjustCard(element: StackElement, changed: Boolean, host: StackSubPageHost) {
    val sub = host.top
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        // The options stay composed under an open detail page, which lives inside them.
        androidx.compose.runtime.key(element.id) {
            Column(Modifier.fillMaxSize().hideIf(sub != null)) {
                AdjustHead(element, changed)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    content = element.options
                )
            }
        }
        if (sub != null) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { sub.onBack() })
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    Text(sub.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                sub.header.value?.let { Box(Modifier.padding(horizontal = 12.dp)) { it() } }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) { sub.content.value(this) }
            }
        }
    }
}

/** Number, name and, where there is one, the part in small with "Automatic" or "Own look". */
@Composable
private fun AdjustHead(element: StackElement, changed: Boolean) {
    Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val lead = element.leading
            if (lead != null) Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { lead() } else NumberBadge(element.number, 28.dp, dot = changed)
            Text(element.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        element.sample?.invoke()
        element.ownLook?.let { own ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (own) stringResource(R.string.settings_look_custom) else element.autoLabel ?: stringResource(R.string.settings_look_automatic),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (own) AutoResetChip(onClick = element.onResetLook, label = stringResource(R.string.settings_look_reset))
            }
        }
    }
}

/** Measured and kept, but not placed: not drawn and not touchable. */
private fun Modifier.hideIf(hidden: Boolean): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) { if (!hidden) placeable.place(0, 0) }
}

/** A switch row that turns a part on or off, first in its options. */
@Composable
internal fun StackShowSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit, summary: String? = null, enabled: Boolean = true) {
    SettingsPreferenceGroup {
        SettingsPreference(
            icon = null,
            title = title,
            summary = summary,
            enabled = enabled,
            onClick = { onChange(!checked) },
            trailing = { androidx.compose.material3.Switch(checked = checked, enabled = enabled, onCheckedChange = onChange) }
        )
    }
}

/** The options under a switched-off part: still there to see, but dimmed. */
@Composable
internal fun StackDimmed(dimmed: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().then(if (dimmed) Modifier.graphicsLayerAlpha(0.45f) else Modifier),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        content = content
    )
}
