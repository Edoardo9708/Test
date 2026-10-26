package it.edo.codecprobe

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal fun LazyListScope.photoFilesItems(state: PhotoUiState, host: PhotoWorkspaceHost) {
    item("files-heading") {
        PhotoStepHeading("01", "Prepara il lavoro", "Nuovo lavoro")
    }
    item("files-actions") {
        PhotoButtonGroup(3) { index, button ->
            when (index) {
                0 -> PhotoButton("Aggiungi\nfile", { host.command(PhotoCommand.ADD_FILES) }, button.height(86.dp),
                    R.drawable.trial_add, enabled = !state.busy, vertical = true, shape = photoJoined(0, false), tag = "photo-add")
                1 -> PhotoButton("Leggi\ncartella", { host.command(PhotoCommand.ADD_FOLDER) }, button.height(86.dp),
                    R.drawable.trial_folder, enabled = !state.busy, vertical = true, shape = photoJoined(1, false), tag = "photo-folder")
                2 -> PhotoButton("Svuota", { host.command(PhotoCommand.CLEAR) }, button.height(86.dp),
                    R.drawable.trial_bin, enabled = !state.busy && state.total > 0, vertical = true, shape = photoJoined(2, false), tag = "photo-clear", destructive = true)
            }
        }
    }
    item("files-selected") {
        Column {
            PhotoHeading("File selezionati · ${state.total} foto", R.drawable.trial_photo)
            PhotoPanel(modifier = Modifier.semantics { testTag = "photo-selected-panel" }, contentSpacing = 6.dp, outlined = false) {
                if (state.total == 0) Text("Aggiungi le foto per iniziare.", style = MaterialTheme.typography.bodyMedium)
                else {
                    PhotoCarousel(state, host)
                    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (state.selectionCount > 0) "${state.selectionCount} foto selezionate" else "Tieni premuto per selezionare più foto.",
                            Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AnimatedVisibility(visible = state.selectionCount > 0,
                            enter = fadeIn(tween(160)) + scaleIn(
                                animationSpec = tween(160), initialScale = .88f,
                                transformOrigin = TransformOrigin(1f, .5f)),
                            exit = fadeOut(tween(120)) + scaleOut(
                                animationSpec = tween(120), targetScale = .94f,
                                transformOrigin = TransformOrigin(1f, .5f))) {
                            PhotoButton("Annulla selezione", { host.command(PhotoCommand.END_SELECTION) },
                                enabled = !state.busy, compact = true, tag = "photo-end-selection")
                        }
                    }
                    Column(Modifier.fillMaxWidth()) {
                        PhotoButtonGroup(3) { index, button ->
                            when (index) {
                                0 -> PhotoButton("Rimuovi", { host.command(PhotoCommand.REMOVE) }, button, enabled = !state.busy,
                                    shape = photoJoined(0, false), tag = "photo-remove", destructive = true)
                                1 -> PhotoButton("Rileggi", { host.command(PhotoCommand.REREAD) }, button, enabled = !state.busy,
                                    shape = photoJoined(1, false), tag = "photo-reread")
                                2 -> PhotoButton("Dettagli", { host.command(PhotoCommand.DETAILS) }, button, enabled = state.detailsEnabled,
                                    selected = state.detailsOpen, shape = photoJoined(2, false), tag = "photo-details")
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(state.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (state.summary.isNotBlank() || state.reading) Text(
                                state.summary.ifBlank { "Lettura dei dati…" }, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        PhotoPopupFold(state.detailsOpen) {
                            // Keep the gap inside the animated content to avoid parent layout jumps.
                            Column {
                                Spacer(Modifier.height(6.dp))
                                PhotoDetailsPane(state)
                            }
                        }
                    }
                }
                if (state.notice.isNotBlank()) Text(state.notice, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    photoSavingItems(state, host)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoCarousel(state: PhotoUiState, host: PhotoWorkspaceHost) {
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
            val tileShape = RoundedCornerShape(14.dp)
            val tileColor = when (Math.floorMod(item.uri.hashCode(), 3)) {
                0 -> MaterialTheme.colorScheme.primaryContainer
                1 -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.tertiaryContainer
            }
            val tile = Modifier.padding(vertical = 4.dp).size(64.dp).clip(tileShape)
                .background(tileColor)
                .border(if (item.selected) 3.dp else 1.dp,
                    if (item.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, tileShape)
                .combinedClickable(enabled = !state.busy, onClick = { host.select(index, false) }, onLongClick = { host.select(index, true) })
                .semantics { contentDescription = item.name; selected = item.selected; testTag = "photo-$index" }
            Box(tile)
        }
        if (state.total > state.items.size) item { Text("…", Modifier.padding(16.dp)) }
    }
}

private fun LazyListScope.photoSavingItems(state: PhotoUiState, host: PhotoWorkspaceHost) {
    item("saving-heading") { PhotoHeading("Salvataggio", R.drawable.trial_folder) }
    item("saving-scope") {
        PhotoPanel(shape = photoJoined(0), outlined = false) {
            Text("Cosa elaborare", style = MaterialTheme.typography.labelLarge)
            PhotoButtonGroup(2) { index, button ->
                if (index == 0) PhotoButton(state.currentLabel, { host.command(PhotoCommand.CURRENT) }, button,
                    selected = !state.general, enabled = !state.busy && state.total > 0, shape = photoJoined(0, false), tag = "photo-current")
                else PhotoButton(state.allLabel, { host.command(PhotoCommand.ALL) }, button,
                    selected = state.general, enabled = !state.busy, shape = photoJoined(2, false), tag = "photo-all")
            }
            Text(state.allocation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    item("saving-folder") {
        PhotoPanel(shape = photoJoined(1), outlined = false) {
            Column(Modifier.fillMaxWidth()) {
                Text("Dove salvare", style = MaterialTheme.typography.labelLarge)
                PhotoPopupFold(!state.general) {
                    Box(Modifier.padding(top = 12.dp)) {
                        PhotoToggle(state.exceptionLabel, state.exception, !state.busy && state.total > 0, host::exception, "photo-exception")
                    }
                }
                PhotoPopupFold(state.editorVisible) {
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
                        PhotoButtonGroup(2) { index, button ->
                            if (index == 0) PhotoButton("Origine", { host.command(PhotoCommand.ORIGIN) }, button,
                                selected = editorOrigin, enabled = state.editorVisible && !state.busy, shape = photoJoined(0, false))
                            else PhotoButton("Altra cartella", { host.command(PhotoCommand.DESTINATION) }, button,
                                selected = editorDestination, enabled = state.editorVisible && !state.busy, shape = photoJoined(2, false))
                        }
                        PhotoPopupFold(editorChooseVisible) {
                            Box(Modifier.padding(top = 12.dp)) {
                                PhotoButton("Scegli cartella…", { host.command(PhotoCommand.CHOOSE_FOLDER) }, Modifier.fillMaxWidth(),
                                    icon = R.drawable.trial_folder, enabled = state.editorVisible && !state.busy, tag = "photo-choose")
                            }
                        }
                        SelectionContainer(Modifier.padding(top = 12.dp)) {
                            Text(editorFolder, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                PhotoPopupFold(!state.editorVisible) {
                    Text("Segue la destinazione generale.", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    item("saving-name") {
        PhotoPanel(shape = photoJoined(1), outlined = false) {
            Text("Nome dei file", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = state.nameAddition,
                onValueChange = host::nameAddition,
                modifier = Modifier.fillMaxWidth().semantics { testTag = "photo-name-addition" },
                enabled = !state.busy,
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                label = { Text("Testo da aggiungere") },
                placeholder = { Text("Es. _modificata") }
            )
            PhotoButtonGroup(2) { index, button ->
                if (index == 0) PhotoButton("Aggiungi all’inizio", { host.namePrefix(true) }, button,
                    selected = state.namePrefix, enabled = !state.busy, shape = photoJoined(0, false), tag = "photo-name-prefix")
                else PhotoButton("Aggiungi alla fine", { host.namePrefix(false) }, button,
                    selected = !state.namePrefix, enabled = !state.busy, shape = photoJoined(2, false), tag = "photo-name-suffix")
            }
            Text("Il testo viene inserito prima dell’estensione .heic.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    item("saving-rules") {
        PhotoPanel(shape = photoJoined(2), outlined = false) {
            PhotoToggle("Conserva solo se più piccolo", state.smaller, !state.busy, host::smaller, "photo-smaller")
            Text("Nome originale con estensione .heic. Se esiste già, viene aggiunto un numero.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    item("permissions-heading") { PhotoHeading("Autorizzazioni cartelle", R.drawable.trial_folder) }
    photoPermissionItems(state, host)
}
