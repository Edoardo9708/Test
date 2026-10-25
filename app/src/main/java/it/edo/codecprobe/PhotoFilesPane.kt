package it.edo.codecprobe

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun PhotoFilesPane(state: PhotoUiState, host: PhotoComposeHost) {
    PhotoStepHeading("01", "Prepara il lavoro", "Nuovo lavoro")
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
    PhotoHeading("File selezionati · ${state.total} foto", R.drawable.trial_photo)
    PhotoPanel {
        if (state.total == 0) Text("Aggiungi le foto per iniziare.", style = MaterialTheme.typography.bodyMedium)
        else {
            PhotoCarousel(state, host)
            Text("Tieni premuto per selezionare più foto.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(state.name, style = MaterialTheme.typography.bodySmall, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(state.summary.ifBlank { if (state.reading) "Lettura dei dati…" else " " }, minLines = 2, maxLines = 2,
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            PhotoFold(state.detailsOpen) {
                PhotoDetailsPane(state)
            }
        }
        Text(state.notice.ifBlank { " " }, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    PhotoSaving(state, host)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoCarousel(state: PhotoUiState, host: PhotoComposeHost) {
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
            val placeholder = minions[Math.floorMod(item.uri.hashCode(), minions.size)]
            val request = remember(item.uri, item.revision) {
                ImageRequest.Builder(context).data(item.uri).size(192).memoryCacheKey("${item.uri}:${item.revision}")
                    .placeholder(placeholder).error(placeholder).crossfade(false).build()
            }
            val tile = Modifier.padding(vertical = 4.dp).size(64.dp).clip(RoundedCornerShape(14.dp))
                .border(if (item.selected) 3.dp else 1.dp,
                    if (item.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                .combinedClickable(enabled = !state.busy, onClick = { host.select(index, false) }, onLongClick = { host.select(index, true) })
                .semantics { selected = item.selected; testTag = "photo-$index" }
            if (host.active) AsyncImage(request, item.name, imageLoader = host.imageLoader, contentScale = ContentScale.Crop, modifier = tile)
            else Image(androidx.compose.ui.res.painterResource(placeholder), item.name, modifier = tile, contentScale = ContentScale.Crop)
        }
        if (state.total > state.items.size) item { Text("…", Modifier.padding(16.dp)) }
    }
}

@Composable
private fun PhotoSaving(state: PhotoUiState, host: PhotoComposeHost) {
    PhotoHeading("Salvataggio", R.drawable.trial_folder)
    PhotoPanel(shape = photoJoined(0)) {
        Text("Cosa elaborare", style = MaterialTheme.typography.labelLarge)
        PhotoButtonGroup(2) { index, button ->
            if (index == 0) PhotoButton(state.currentLabel, { host.command(PhotoCommand.CURRENT) }, button,
                selected = !state.general, enabled = !state.busy && state.total > 0, shape = photoJoined(0, false), tag = "photo-current")
            else PhotoButton(state.allLabel, { host.command(PhotoCommand.ALL) }, button,
                selected = state.general, enabled = !state.busy, shape = photoJoined(2, false), tag = "photo-all")
        }
        Text(state.allocation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(4.dp))
    PhotoPanel(shape = photoJoined(1)) {
        Text("Dove salvare", style = MaterialTheme.typography.labelLarge)
        PhotoFold(!state.general) {
            PhotoToggle(state.exceptionLabel, state.exception, !state.busy && state.total > 0, host::exception, "photo-exception")
        }
        PhotoFold(state.editorVisible) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PhotoButtonGroup(2) { index, button ->
                    if (index == 0) PhotoButton("Origine", { host.command(PhotoCommand.ORIGIN) }, button,
                        selected = state.origin, enabled = !state.busy, shape = photoJoined(0, false))
                    else PhotoButton("Altra cartella", { host.command(PhotoCommand.DESTINATION) }, button,
                        selected = state.destination, enabled = !state.busy, shape = photoJoined(2, false))
                }
                PhotoFold(state.chooseVisible) {
                    PhotoButton("Scegli cartella…", { host.command(PhotoCommand.CHOOSE_FOLDER) }, Modifier.fillMaxWidth(),
                        icon = R.drawable.trial_folder, enabled = !state.busy, tag = "photo-choose")
                }
                SelectionContainer { Text(state.folder, style = MaterialTheme.typography.bodySmall) }
            }
        }
        PhotoFold(!state.editorVisible) { Text("Segue la destinazione generale.", style = MaterialTheme.typography.bodySmall) }
    }
    Spacer(Modifier.height(4.dp))
    PhotoPanel(shape = photoJoined(2)) {
        PhotoToggle("Conserva solo se più piccolo", state.smaller, !state.busy, host::smaller, "photo-smaller")
        Text("Nome originale con estensione .heic. Se esiste già, viene aggiunto un numero.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    PhotoHeading("Autorizzazioni cartelle", R.drawable.trial_folder)
    AndroidView(factory = { host.legacyPermissions() }, modifier = Modifier.fillMaxWidth().semantics { testTag = "photo-permissions" })
}

private val minions = intArrayOf(R.drawable.minion_01, R.drawable.minion_02, R.drawable.minion_03, R.drawable.minion_04,
    R.drawable.minion_05, R.drawable.minion_06, R.drawable.minion_07, R.drawable.minion_08, R.drawable.minion_09, R.drawable.minion_10,
    R.drawable.minion_11, R.drawable.minion_12, R.drawable.minion_13, R.drawable.minion_14, R.drawable.minion_15, R.drawable.minion_16,
    R.drawable.minion_17, R.drawable.minion_18, R.drawable.minion_19, R.drawable.minion_20)
