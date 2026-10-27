package it.edo.codecprobe

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

internal fun LazyListScope.photoFilesItems(state: PhotoUiState, host: PhotoWorkspaceHost) {
    item("files-heading") {
        PhotoStepHeading("01", "Prepara il lavoro", "Nuovo lavoro")
    }
    item("files-actions") {
        AppButtonGroup(3) { index, button ->
            when (index) {
                0 -> AppButton("Aggiungi\nfile", { host.command(PhotoCommand.ADD_FILES) }, button.height(86.dp),
                    R.drawable.trial_add, enabled = !state.busy, vertical = true, shape = appJoinedShape(0, false), tag = "photo-add")
                1 -> AppButton("Leggi\ncartella", { host.command(PhotoCommand.ADD_FOLDER) }, button.height(86.dp),
                    R.drawable.trial_folder, enabled = !state.busy, vertical = true, shape = appJoinedShape(1, false), tag = "photo-folder")
                2 -> AppButton("Svuota", { host.command(PhotoCommand.CLEAR) }, button.height(86.dp),
                    R.drawable.trial_bin, enabled = !state.busy && state.total > 0, vertical = true, shape = appJoinedShape(2, false), tag = "photo-clear", destructive = true)
            }
        }
    }
    item("files-selected") {
        Column {
            AppSectionHeading(when (state.total) {
                0 -> "Foto selezionate"
                1 -> "1 foto selezionata"
                else -> "${state.total} foto selezionate"
            }, R.drawable.trial_photo)
            AppPanel(modifier = Modifier.semantics { testTag = "photo-selected-panel" }, contentSpacing = 6.dp) {
                if (state.total == 0) Text("Aggiungi le foto per iniziare.", style = MaterialTheme.typography.bodyMedium)
                else {
                    PhotoCarousel(state, host)
                    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (state.selectionCount > 0) "${state.selectionCount} foto selezionate" else "Tieni premuto per selezionare più foto.",
                            Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AnimatedVisibility(visible = state.selectionCount > 0,
                            enter = expandHorizontally(
                                animationSpec = tween(200, easing = FastOutSlowInEasing), expandFrom = Alignment.End
                            ) + fadeIn(tween(170, easing = FastOutSlowInEasing)),
                            exit = shrinkHorizontally(
                                animationSpec = tween(170, easing = FastOutSlowInEasing), shrinkTowards = Alignment.End
                            ) + fadeOut(tween(130))) {
                            AppButton("Annulla selezione", { host.command(PhotoCommand.END_SELECTION) },
                                enabled = !state.busy, compact = true, tag = "photo-end-selection")
                        }
                    }
                    Column(Modifier.fillMaxWidth()) {
                        AppButtonGroup(3) { index, button ->
                            when (index) {
                                0 -> AppButton("Rimuovi", { host.command(PhotoCommand.REMOVE) }, button, enabled = !state.busy,
                                    shape = appJoinedShape(0, false), tag = "photo-remove", destructive = true)
                                1 -> AppButton("Rileggi", { host.command(PhotoCommand.REREAD) }, button, enabled = !state.busy,
                                    shape = appJoinedShape(1, false), tag = "photo-reread")
                                2 -> AppButton("Dettagli", { host.command(PhotoCommand.DETAILS) }, button, enabled = state.detailsEnabled,
                                    selected = state.detailsOpen, shape = appJoinedShape(2, false), tag = "photo-details")
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        AnimatedContent(
                            targetState = state.metadata,
                            transitionSpec = {
                                (fadeIn(tween(180, easing = FastOutSlowInEasing)) togetherWith
                                    fadeOut(tween(180, easing = FastOutSlowInEasing))).using(
                                    androidx.compose.animation.SizeTransform(clip = false) { _, _ ->
                                        tween(220, easing = FastOutSlowInEasing)
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.TopStart,
                            label = "photo-metadata-transition"
                        ) { metadata ->
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(metadata.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Box(Modifier.fillMaxWidth().heightIn(min = 20.dp), contentAlignment = Alignment.CenterStart) {
                                    when {
                                        metadata.loading -> Text("Lettura dei dati…", maxLines = 2, overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        metadata.summary.isNotBlank() -> Text(metadata.summary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        metadata.error.isNotBlank() -> Text("Metadati non disponibili", maxLines = 2, overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                        else -> Spacer(Modifier.height(20.dp))
                                    }
                                }
                                Box(Modifier.fillMaxWidth().heightIn(min = 32.dp), contentAlignment = Alignment.CenterStart) {
                                    if (metadata.status.isNotBlank()) Text(metadata.status, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (metadata.error.isNotBlank()) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.onSurfaceVariant)
                                    else Spacer(Modifier.height(32.dp))
                                }
                            }
                        }
                        AppPopupTransition(state.detailsOpen, synchronizedExit = true) {
                            // Keep the gap inside the animated content to avoid parent layout jumps.
                            Column {
                                Spacer(Modifier.height(6.dp))
                                PhotoDetailsPane(state)
                            }
                        }
                    }
                }
                if (state.selectedNames.isNotEmpty() && state.notice.isNotBlank()) Text(state.notice, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    photoSavingItems(state, host)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoCarousel(state: PhotoUiState, host: PhotoWorkspaceHost) {
    val context = LocalContext.current
    val carousel = rememberLazyListState()
    var inViewport by remember { mutableStateOf(false) }
    LaunchedEffect(host.active, state.busy, state.items, inViewport) {
        if (!host.active || state.busy || !inViewport) { host.visiblePhotos(emptyList()); return@LaunchedEffect }
        snapshotFlow { carousel.layoutInfo.visibleItemsInfo.mapNotNull { state.items.getOrNull(it.index)?.uri } }
        .distinctUntilChanged().collectLatest { uris ->
            delay(140) // Let a fling settle instead of reading every photo it passes.
            host.visiblePhotos(uris)
        }
    }
    DisposableEffect(host) { onDispose { host.visiblePhotos(emptyList()) } }
    LazyRow(state = carousel, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().height(72.dp)
        .onGloballyPositioned { val bounds = it.boundsInWindow(); inViewport = bounds.width > 0f && bounds.height > 0f }
        .semantics { testTag = "photo-carousel" }) {
        itemsIndexed(state.items, key = { _, item -> item.uri }) { index, item ->
            val placeholder = PHOTO_MINION_PLACEHOLDERS[Math.floorMod(item.uri.hashCode(), PHOTO_MINION_PLACEHOLDERS.size)]
            val request = remember(context, item.uri, item.revision) {
                ImageRequest.Builder(context).data(item.uri).size(192)
                    .memoryCacheKey("${item.uri}:${item.revision}")
                    .placeholder(placeholder).error(placeholder).crossfade(false).build()
            }
            val tileShape = RoundedCornerShape(14.dp)
            val tileBorderColor by animateColorAsState(
                if (item.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                tween(180, easing = FastOutSlowInEasing), label = "photo-thumbnail-border"
            )
            val tileBorderWidth by animateDpAsState(if (item.selected) 3.dp else 1.dp,
                tween(180, easing = FastOutSlowInEasing), label = "photo-thumbnail-border-width")
            val tile = Modifier.padding(vertical = 4.dp).size(AppDesignTokens.Size.thumbnail).clip(tileShape)
                .border(tileBorderWidth, tileBorderColor, tileShape)
                .combinedClickable(enabled = !state.busy, onClick = { host.select(index, false) }, onLongClick = { host.select(index, true) })
                .semantics { contentDescription = item.name; selected = item.selected; testTag = "photo-$index" }
            val imageLoader = host.imageLoader
            if (host.active && imageLoader != null) {
                AsyncImage(request, item.name, imageLoader = imageLoader, contentScale = ContentScale.Crop, modifier = tile)
            } else {
                Image(painterResource(placeholder), item.name, modifier = tile, contentScale = ContentScale.Crop)
            }
        }
        if (state.total > state.items.size) item { Text("…", Modifier.padding(16.dp)) }
    }
}

private val PHOTO_MINION_PLACEHOLDERS = intArrayOf(
    R.drawable.minion_01, R.drawable.minion_02, R.drawable.minion_03, R.drawable.minion_04,
    R.drawable.minion_05, R.drawable.minion_06, R.drawable.minion_07, R.drawable.minion_08,
    R.drawable.minion_09, R.drawable.minion_10, R.drawable.minion_11, R.drawable.minion_12,
    R.drawable.minion_13, R.drawable.minion_14, R.drawable.minion_15, R.drawable.minion_16,
    R.drawable.minion_17, R.drawable.minion_18, R.drawable.minion_19, R.drawable.minion_20
)

private fun LazyListScope.photoSavingItems(state: PhotoUiState, host: PhotoWorkspaceHost) {
    item("saving-heading") { AppSectionHeading("Salvataggio", R.drawable.trial_folder) }
    item("saving-group") {
        AppPanelGroup(Modifier.semantics { testTag = "photo-saving-group" }) {
            AppGroupPanel(position = 0) {
                Text("Cosa elaborare", style = MaterialTheme.typography.labelLarge)
                AppButtonGroup(2) { index, button ->
                    if (index == 0) AppButton(state.currentLabel, { host.command(PhotoCommand.CURRENT) }, button,
                        selected = !state.general, enabled = !state.busy && state.total > 0, shape = appJoinedShape(0, false), tag = "photo-current")
                    else AppButton(state.allLabel, { host.command(PhotoCommand.ALL) }, button,
                        selected = state.general, enabled = !state.busy, shape = appJoinedShape(2, false), tag = "photo-all")
                }
                Text(state.allocation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AppGroupPanel(position = 1) {
                Column(Modifier.fillMaxWidth()) {
                    Text("Dove salvare", style = MaterialTheme.typography.labelLarge)
                    AppPopupTransition(!state.general) {
                        Box(Modifier.padding(top = 12.dp)) {
                            AppSwitchRow(state.exceptionLabel, state.exception, !state.busy && state.total > 0, host::exception, "photo-exception")
                        }
                    }
                    AppPopupTransition(state.editorVisible) {
                        // Keep the editor intact while its outer fold closes.
                        var editorOrigin by remember { mutableStateOf(state.origin) }
                        var editorDestination by remember { mutableStateOf(state.destination) }
                        var editorChooseVisible by remember { mutableStateOf(state.chooseVisible) }
                        var editorFolder by remember { mutableStateOf(state.folder) }
                        if (state.editorVisible) {
                            editorOrigin = state.origin
                            editorDestination = state.destination
                            editorChooseVisible = state.chooseVisible
                            editorFolder = state.folder
                        }
                        Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                            AppButtonGroup(2) { index, button ->
                                if (index == 0) AppButton("Origine", { host.command(PhotoCommand.ORIGIN) }, button,
                                    selected = editorOrigin, enabled = state.editorVisible && !state.busy, shape = appJoinedShape(0, false))
                                else AppButton("Altra cartella", { host.command(PhotoCommand.DESTINATION) }, button,
                                    selected = editorDestination, enabled = state.editorVisible && !state.busy, shape = appJoinedShape(2, false))
                            }
                            AppPopupTransition(editorChooseVisible) {
                                Box(Modifier.padding(top = 12.dp)) {
                                    AppButton("Scegli cartella…", { host.command(PhotoCommand.CHOOSE_FOLDER) }, Modifier.fillMaxWidth(),
                                        icon = R.drawable.trial_folder, enabled = state.editorVisible && !state.busy, tag = "photo-choose")
                                }
                            }
                            SelectionContainer(Modifier.padding(top = 12.dp)) {
                                Text(editorFolder, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    AppPopupTransition(!state.editorVisible) {
                        Text("Segue la destinazione generale.", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            AppGroupPanel(position = 2) {
                val focusManager = LocalFocusManager.current
                var nameFieldFocused by remember { mutableStateOf(false) }
                Text("Nome dei file", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = state.nameAddition,
                    onValueChange = host::nameAddition,
                    modifier = Modifier.fillMaxWidth()
                        .onFocusChanged { nameFieldFocused = it.isFocused }
                        .semantics { testTag = "photo-name-addition" },
                    enabled = !state.busy,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(AppDesignTokens.Shape.field),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    label = { Text("Testo da aggiungere") },
                    placeholder = { Text("Es. titolo_ oppure _estate") },
                    trailingIcon = {
                        if (nameFieldFocused) IconButton(
                            onClick = { focusManager.clearFocus() },
                            modifier = Modifier.semantics { testTag = "photo-name-finish" }
                        ) { Icon(Icons.Rounded.Check, contentDescription = "Fine modifica") }
                    }
                )
                Text(
                    "Esempio: la mia magnifica foto.jpeg → " +
                        ImageOutput.name("la mia magnifica foto.jpeg", false, state.nameAddition, state.namePrefix),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { testTag = "photo-name-preview" }
                )
                AppButtonGroup(2) { index, button ->
                    if (index == 0) AppButton("Aggiungi all’inizio", { focusManager.clearFocus(); host.namePrefix(true) }, button,
                        selected = state.namePrefix, enabled = !state.busy, shape = appJoinedShape(0, false), tag = "photo-name-prefix")
                    else AppButton("Aggiungi alla fine", { focusManager.clearFocus(); host.namePrefix(false) }, button,
                        selected = !state.namePrefix, enabled = !state.busy, shape = appJoinedShape(2, false), tag = "photo-name-suffix")
                }
            }
            AppGroupPanel(position = 3) {
                AppSwitchRow("Conserva solo se più piccolo", state.smaller, !state.busy, host::smaller, "photo-smaller")
                Text("Nome originale con estensione .heic. Se esiste già, viene aggiunto un numero.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    item("permissions-heading") { AppSectionHeading("Autorizzazioni cartelle", R.drawable.trial_folder) }
    photoPermissionItems(state, host)
}
