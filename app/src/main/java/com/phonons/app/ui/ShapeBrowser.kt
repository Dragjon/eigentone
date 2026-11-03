package com.phonons.app.ui

import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phonons.app.R
import com.phonons.app.data.CustomShape
import com.phonons.app.data.Preset
import com.phonons.app.data.Repo
import com.phonons.app.data.ShapePoint
import com.phonons.app.data.ShapeText
import com.phonons.app.data.SoundFinder
import com.phonons.app.dsp.GraphicEq
import com.phonons.app.dsp.GraphicShape
import com.phonons.app.dsp.LibraryShape
import com.phonons.app.dsp.ShapeCategory
import com.phonons.app.dsp.ShapeLibrary
import com.phonons.app.dsp.ShapeTrait
import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

private const val THUMBNAIL_RANGE_DB = 9f
private const val SHORT_SCREEN_DP = 520
private const val CARD_COLUMNS = 2
private const val FOLLOW_WARNING_DB = 0.75

/** Survives rotation, so Cancel can always put back exactly what was there before. */
private val PresetSaver = Saver<Preset, String>(
    save = { Json.encodeToString(Preset.serializer(), it) },
    restore = { Json.decodeFromString(Preset.serializer(), it) },
)

/** One card on the library's first page. */
private class BrowseOption(val label: String, val detail: String, val count: Int, val onClick: () -> Unit)

/**
 * Searchable list of graphic EQ shapes. It opens on a page showing every way in, so nothing is hidden
 * off the edge of the screen. Tapping a shape changes the live sound, so it can be judged on the music
 * that's playing. Nothing is kept until it's confirmed.
 */
@Composable
fun ShapeBrowser(onClose: () -> Unit) {
    val state by Repo.state.collectAsStateWithLifecycle()
    val before by rememberSaveable(stateSaver = PresetSaver) { mutableStateOf(Repo.state.value.current) }
    var bands by rememberSaveable { mutableIntStateOf(before.tone.graphicBands) }
    var layering by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ShapeDraft?>(null) }
    val yours = remember(state.customShapes) {
        state.customShapes.map { custom -> ShapeLibrary.yours(custom.id, custom.name, custom.points.map { it.hz to it.db }) }
    }

    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<ShapeCategory?>(null) }
    var favouritesOnly by rememberSaveable { mutableStateOf(false) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    var trait by rememberSaveable { mutableStateOf<ShapeTrait?>(null) }
    var pickedId by rememberSaveable { mutableStateOf<String?>(null) }
    var amount by rememberSaveable { mutableFloatStateOf(1f) }
    var hearingBefore by rememberSaveable { mutableStateOf(false) }
    val picked = remember(pickedId, yours) { pickedId?.let { ShapeLibrary.find(it, yours) } }
    val focus = LocalFocusManager.current

    val favourites = state.favoriteShapes
    val searching = query.isNotBlank()
    val onFirstPage = !searching && category == null && !favouritesOnly && !showAll && trait == null
    val results = remember(query, category, trait, favouritesOnly, if (favouritesOnly) favourites else null, yours) {
        ShapeLibrary.search(query, category, trait, if (favouritesOnly) favourites else null, yours)
    }
    val list = rememberLazyListState()
    LaunchedEffect(query, category, trait, favouritesOnly) { list.scrollToItem(0) }

    fun toneFor(shape: LibraryShape, level: Float) = before.tone.withShape(
        name = if (level == 1f) shape.shape.name else "${shape.shape.name} at ${kotlin.math.round(level * 100).toInt()}%",
        faders = if (layering) {
            GraphicEq.layered(
                GraphicEq.normalized(before.tone.graphicDb, before.tone.graphicBands).let {
                    GraphicEq.resample(it, before.tone.graphicBands, bands)
                },
                shape.shape, bands, level.toDouble(),
            )
        } else {
            GraphicEq.apply(shape.shape, bands, level.toDouble())
        },
        bands = bands,
        layered = layering,
    )

    // Matched to the sound before, so a shape isn't favoured just for being louder.
    fun hear(shape: LibraryShape, level: Float) = Repo.edit {
        SoundFinder.levelMatched(before, toneFor(shape, level), before, state.autoHeadroom)
    }

    fun restore() {
        Repo.edit { before }
        pickedId = null
        hearingBefore = false
        amount = 1f
        bands = before.tone.graphicBands
    }

    fun toFirstPage() {
        query = ""
        category = null
        favouritesOnly = false
        showAll = false
        trait = null
    }

    fun goBack() = when {
        picked != null -> restore()
        !onFirstPage -> toFirstPage()
        else -> onClose()
    }

    BackHandler { goBack() }

    val title = when {
        searching -> "Search results"
        favouritesOnly -> "Favourites"
        category != null -> category!!.label
        showAll -> "All shapes"
        trait != null -> trait!!.label
        else -> "Shape library"
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding(),
    ) {
        Row(Modifier.padding(start = 4.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { goBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ui_back))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = Palette.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            InfoButton(Help.shapeLibrary)
        }

        // A short screen can't fit the list and the preview controls, so a preview takes it over.
        val previewOnly = picked != null && LocalConfiguration.current.screenHeightDp < SHORT_SCREEN_DP

        if (!previewOnly) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.ui_rock_muddy_podcast_bass_100), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.ui_clear_search)) }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            )

            if (onFirstPage) {
                FirstPage(
                    modifier = Modifier.weight(1f),
                    favouriteCount = favourites.size,
                    yourCount = yours.size,
                    onAll = { showAll = true },
                    onFavourites = { favouritesOnly = true },
                    onCategory = { category = it },
                    onTrait = { trait = it },
                )
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 16.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${results.size} ${if (results.size == 1) "shape" else "shapes"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.TextDim,
                        modifier = Modifier.weight(1f),
                    )
                    SoundFilter(trait) { trait = it }
                }
                if (category == ShapeCategory.YOURS) {
                    Row(
                        Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = { editing = ShapeDraft() }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.ui_new_shape), maxLines = 1) }
                        FilledTonalButton(
                            enabled = !GraphicEq.isFlat(GraphicEq.normalized(before.tone.graphicDb, before.tone.graphicBands)),
                            onClick = {
                                val faders = GraphicEq.frequencies(before.tone.graphicBands)
                                    .zip(GraphicEq.normalized(before.tone.graphicDb, before.tone.graphicBands))
                                    .map { (hz, db) -> ShapePoint(hz, db) }
                                editing = ShapeDraft(text = ShapeText.format(faders))
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.ui_save_my_faders), maxLines = 1) }
                    }
                }
                LazyColumn(
                    state = list,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    if (results.isEmpty()) {
                        item(key = "empty") {
                            EmptyResults(favouritesEmpty = favouritesOnly && favourites.isEmpty(), onClear = ::toFirstPage)
                        }
                    }
                    results.forEachIndexed { index, shape ->
                        // Search results are ranked, so section headings would only break up the order.
                        val previous = results.getOrNull(index - 1)
                        if (!searching && (previous?.group != shape.group || previous.category != shape.category)) {
                            item(key = "group:${shape.category}:${shape.group}") {
                                Text(
                                    if (category == null) "${shape.category.label} · ${shape.group}" else shape.group,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Palette.TextDim,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                                )
                            }
                        }
                        item(key = shape.id) {
                            ShapeRow(
                                shape = shape,
                                showGroup = searching,
                                selected = shape.id == pickedId,
                                favourite = shape.id in favourites,
                                onToggleFavourite = { Repo.toggleFavoriteShape(shape.id) },
                                onEdit = if (shape.category == ShapeCategory.YOURS) {
                                    {
                                        val custom = state.customShapes.first { it.id == shape.id }
                                        editing = ShapeDraft(custom.id, custom.name, ShapeText.format(custom.points))
                                    }
                                } else {
                                    null
                                },
                                onClick = {
                                    focus.clearFocus()
                                    pickedId = shape.id
                                    amount = 1f
                                    hearingBefore = false
                                    hear(shape, 1f)
                                },
                            )
                        }
                    }
                }
            }
        }

        if (picked != null) {
            PreviewPanel(
                shape = picked,
                amount = amount,
                hearingBefore = hearingBefore,
                layering = layering,
                bands = bands,
                onLayering = {
                    layering = it
                    hearingBefore = false
                    hear(picked, amount)
                },
                onFinerFaders = {
                    bands = GraphicEq.BAND_OPTIONS.last()
                    hearingBefore = false
                    hear(picked, amount)
                },
                silentReason = when {
                    !state.enabled -> "Eigentone is off, so you won't hear this until it's on."
                    state.bypass -> "Bypass is on, so you won't hear this until it's off."
                    else -> null
                },
                onAmount = {
                    amount = it
                    hearingBefore = false
                    hear(picked, it)
                },
                onToggleBefore = {
                    hearingBefore = !hearingBefore
                    if (hearingBefore) Repo.edit { before } else hear(picked, amount)
                },
                onCancel = ::restore,
                onUse = {
                    // Without the comparison's temporary level trim.
                    Repo.edit { before.copy(tone = toneFor(picked, amount)) }
                    Repo.commit()
                    onClose()
                },
            )
        }
    }

    editing?.let { draft -> ShapeEditorDialog(draft, onDismiss = { editing = null }) }
}

/** What's being edited in the shape editor. A null id means it's new. */
private data class ShapeDraft(val id: String? = null, val name: String = "", val text: String = "")

/** Type in a curve, for example one copied from another app's EQ screen. */
@Composable
private fun ShapeEditorDialog(draft: ShapeDraft, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(draft.name) }
    var text by remember { mutableStateOf(draft.text) }
    val parsed = remember(text) { ShapeText.parse(text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Tooltip,
        title = { Text(if (draft.id == null) "New shape" else "Edit shape") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text(stringResource(R.string.ui_name)) })
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    minLines = 4,
                    maxLines = 10,
                    label = { Text(stringResource(R.string.ui_frequency_and_level)) },
                    placeholder = { Text("100 +4\n1.6k -2") },
                )
                when {
                    text.isBlank() -> Text(
                        stringResource(R.string.ui_one_point_per_line_such_as_100_4_or_1_6k),
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.TextDim,
                    )
                    parsed is ShapeText.Parsed.Problem -> Text(parsed.message, style = MaterialTheme.typography.bodySmall, color = Palette.Danger)
                    parsed is ShapeText.Parsed.Points -> {
                        val curve = GraphicEq.apply(GraphicShape(name, parsed.points.map { it.hz to it.db }), 31)
                        CurveThumbnail(curve, selected = true)
                    }
                }
                if (draft.id != null) {
                    TextButton(onClick = {
                        Repo.deleteShape(draft.id)
                        onDismiss()
                    }) { Text(stringResource(R.string.ui_delete_shape), color = Palette.Danger) }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && parsed is ShapeText.Parsed.Points,
                onClick = {
                    val points = (parsed as ShapeText.Parsed.Points).points
                    Repo.saveShape(CustomShape(draft.id ?: "custom:${UUID.randomUUID()}", name.trim(), points))
                    onDismiss()
                },
            ) { Text(stringResource(R.string.ui_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel)) } },
    )
}

/** Every category and sound at once. Cards and chips wrap, so none can end up off the screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FirstPage(
    modifier: Modifier,
    favouriteCount: Int,
    yourCount: Int,
    onAll: () -> Unit,
    onFavourites: () -> Unit,
    onCategory: (ShapeCategory) -> Unit,
    onTrait: (ShapeTrait) -> Unit,
) {
    val counts = remember { ShapeLibrary.all.groupingBy { it.category }.eachCount() }
    val options = listOf(
        BrowseOption("All shapes", "The whole library", ShapeLibrary.all.size, onAll),
        BrowseOption("Favourites", "Shapes you've starred", favouriteCount, onFavourites),
        BrowseOption(ShapeCategory.YOURS.label, ShapeCategory.YOURS.examples, yourCount) { onCategory(ShapeCategory.YOURS) },
    ) + (ShapeCategory.entries - ShapeCategory.YOURS).map { category ->
        BrowseOption(category.label, category.examples, counts[category] ?: 0) { onCategory(category) }
    }

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.ui_browse_by_type), style = MaterialTheme.typography.titleSmall, color = Palette.TextDim)
        options.chunked(CARD_COLUMNS).forEach { row ->
            // Equal heights keep the grid tidy when one card's text wraps further than its neighbour's.
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option ->
                    BrowseCard(
                        option,
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                // An odd last row keeps its card the same width as the others.
                if (row.size < CARD_COLUMNS) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.ui_browse_by_sound), style = MaterialTheme.typography.titleSmall, color = Palette.TextDim)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ShapeTrait.entries.forEach { option ->
                FilterChip(
                    selected = false,
                    onClick = { onTrait(option) },
                    label = { Text(option.label, maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(labelColor = Palette.Text),
                )
            }
        }
    }
}

@Composable
private fun BrowseCard(option: BrowseOption, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Surface)
            .clickable(role = Role.Button, onClick = option.onClick)
            .heightIn(min = 88.dp)
            .padding(12.dp),
    ) {
        Text(option.label, style = MaterialTheme.typography.titleSmall, color = Palette.Text)
        Spacer(Modifier.height(2.dp))
        Text(option.detail, style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
        Spacer(Modifier.weight(1f))
        Text("${option.count}", style = Readout, color = Palette.TextDim, modifier = Modifier.padding(top = 4.dp))
    }
}

/** A menu instead of a row of chips, so the list keeps its space and every choice can be seen at once. */
@Composable
private fun SoundFilter(selected: ShapeTrait?, onSelect: (ShapeTrait?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        val label = selected?.label ?: "Any sound"
        if (selected != null) {
            Button(onClick = { open = true }) {
                Text(label, maxLines = 1)
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
        } else {
            FilledTonalButton(onClick = { open = true }) {
                Text(label, maxLines = 1)
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ui_any_sound)) },
                onClick = {
                    onSelect(null)
                    open = false
                },
            )
            ShapeTrait.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelect(option)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun EmptyResults(favouritesEmpty: Boolean, onClear: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            if (favouritesEmpty) "Tap the star on any shape to keep it here." else "No shapes match that.",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextDim,
        )
        TextButton(onClick = onClear) { Text(stringResource(R.string.ui_back_to_the_library)) }
    }
}

@Composable
private fun ShapeRow(
    shape: LibraryShape,
    showGroup: Boolean,
    selected: Boolean,
    favourite: Boolean,
    onToggleFavourite: () -> Unit,
    onEdit: (() -> Unit)?,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) Palette.SurfaceHigh else Palette.Background)
            .semantics { this.selected = selected }
            .clickable(role = Role.Button, onClickLabel = "Hear ${shape.name}", onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CurveThumbnail(shape.curve, selected)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                shape.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) Palette.Amber else Palette.Text,
            )
            Text(
                if (showGroup) "${shape.group}. ${shape.description}" else shape.description,
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextDim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onEdit != null) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit ${shape.name}", tint = Palette.TextDim)
            }
        }
        IconButton(onClick = onToggleFavourite) {
            Icon(
                // Filled against outlined, so the state doesn't rely on colour alone.
                painterResource(if (favourite) R.drawable.ic_star else R.drawable.ic_star_border),
                contentDescription = if (favourite) "Remove ${shape.name} from favourites" else "Add ${shape.name} to favourites",
                tint = if (favourite) Palette.Amber else Palette.TextDim,
            )
        }
    }
}

/** Every thumbnail uses the same scale, so a strong shape looks stronger than a gentle one. */
@Composable
private fun CurveThumbnail(curve: List<Double>, selected: Boolean) {
    Canvas(
        Modifier
            .size(width = 56.dp, height = 36.dp)
            .background(Palette.Surface),
    ) {
        val middle = size.height / 2
        drawLine(Palette.Track, Offset(0f, middle), Offset(size.width, middle), strokeWidth = 1f)
        val path = Path()
        curve.forEachIndexed { i, db ->
            val x = size.width * i / (curve.size - 1)
            val y = middle - (db.toFloat() / THUMBNAIL_RANGE_DB).coerceIn(-1f, 1f) * (middle - 2f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, if (selected) Palette.Amber else Palette.Text, style = Stroke(width = 1.5.dp.toPx()))
    }
}

@Composable
private fun PreviewPanel(
    shape: LibraryShape,
    amount: Float,
    hearingBefore: Boolean,
    layering: Boolean,
    bands: Int,
    onLayering: (Boolean) -> Unit,
    onFinerFaders: () -> Unit,
    silentReason: String?,
    onAmount: (Float) -> Unit,
    onToggleBefore: () -> Unit,
    onCancel: () -> Unit,
    onUse: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Palette.Surface)
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 8.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                shape.name,
                style = MaterialTheme.typography.titleMedium,
                color = Palette.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            if (hearingBefore) {
                Button(onClick = onToggleBefore) { Text(stringResource(R.string.ui_hearing_before)) }
            } else {
                FilledTonalButton(onClick = onToggleBefore) { Text(stringResource(R.string.ui_hear_before)) }
            }
        }
        if (silentReason != null) {
            Text(
                silentReason,
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        } else {
            NothingPlayingHint(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        val followError = remember(shape, bands) { GraphicEq.followError(shape.shape, bands) }
        if (followError > FOLLOW_WARNING_DB && bands < GraphicEq.BAND_OPTIONS.last()) {
            Row(
                Modifier.padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$bands faders miss this shape by up to ${String.format(Locale.US, "%.1f", followError)} dB.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.Text,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onFinerFaders) { Text("Use ${GraphicEq.BAND_OPTIONS.last()}") }
            }
        }
        Row(
            Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                SegmentedButton(
                    selected = !layering,
                    onClick = { onLayering(false) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    colors = segmentColors(),
                    icon = {},
                ) { Text(stringResource(R.string.ui_replace), maxLines = 1) }
                SegmentedButton(
                    selected = layering,
                    onClick = { onLayering(true) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    colors = segmentColors(),
                    icon = {},
                ) { Text(stringResource(R.string.ui_add), maxLines = 1) }
            }
            InfoButton(Help.shapeMode)
        }
        SliderSetting(
            title = stringResource(R.string.ui_amount),
            tip = Help.shapeAmount,
            readout = "${(amount * 100).roundToInt()}%",
            value = amount,
            range = 0.25f..1.5f,
            default = 1f,
            onChange = onAmount,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.ui_cancel)) }
            Button(onClick = onUse, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.ui_use_this)) }
        }
    }
}
