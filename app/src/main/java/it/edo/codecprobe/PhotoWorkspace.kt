package it.edo.codecprobe

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

internal const val PHOTO_DIAGNOSTIC_STATIC_CONTENT = true
private val PHOTO_DIAGNOSTIC_BLOCK_HEIGHTS = listOf(
    128.dp, 208.dp, 152.dp, 184.dp, 216.dp, 136.dp,
    176.dp, 144.dp, 200.dp, 128.dp, 184.dp, 160.dp,
    128.dp, 208.dp, 152.dp, 184.dp, 216.dp, 136.dp,
    176.dp, 144.dp, 200.dp, 128.dp, 184.dp, 160.dp
)
private val PHOTO_DIAGNOSTIC_CONTENT = listOf(
    "Preparazione" to listOf("Il contenuto resta fermo", "Nessun file viene letto", "Scorrimento di prova"),
    "Origine dei file" to listOf("Percorso non elaborato", "Nessuna cartella aperta", "Dati solo dimostrativi", "Stato invariato"),
    "Selezione" to listOf("Elementi non selezionabili", "Nessun tocco richiesto", "Riquadro statico"),
    "Anteprima" to listOf("Nessuna immagine caricata", "Nessuna miniatura presente", "Spazio visivo costante", "Scroll continuo"),
    "Dettagli elemento" to listOf("Testo senza aggiornamenti", "Nessun metadato letto", "Riga di controllo", "Layout invariato"),
    "Nome file" to listOf("Nome puramente illustrativo", "Nessuna risorsa aperta", "Posizione successiva"),
    "Dimensioni" to listOf("Valori non calcolati", "Contenuto statico", "Nessun lavoro in background", "Spazio regolare"),
    "Opzioni" to listOf("Controlli non attivi", "Nessuna animazione", "Riquadro opaco"),
    "Destinazione" to listOf("Percorso dimostrativo", "Nessuna scrittura avviata", "Stato non osservato", "Blocco successivo"),
    "Salvataggio" to listOf("Nessun file salvato", "Testo fisso", "Contenuto di prova"),
    "Qualità" to listOf("Valore non modificabile", "Nessuno slider attivo", "Dimensione costante", "Scroll di confronto"),
    "Riepilogo" to listOf("Riga breve", "Una riga più lunga per", "confrontare il movimento"),
    "Permessi" to listOf("Nessun controllo permessi", "Nessuna richiesta di accesso", "Stato statico"),
    "Cartella" to listOf("Nessuna cartella consultata", "Nessuna lettura sul disco", "Pannello dimostrativo", "Ancora contenuto fisso"),
    "Avanzamento" to listOf("Nessuna conversione in corso", "Nessun indicatore animato", "Solo testo statico"),
    "Stato attività" to listOf("Nessun processo avviato", "Nessun dato aggiornato", "Riquadro di riferimento", "Tutto resta fermo"),
    "Informazioni" to listOf("Descrizione breve", "Una frase di lunghezza media", "Testo senza dipendenze"),
    "Controllo visivo" to listOf("Separatore ben visibile", "Margine interno regolare", "Altezza già definita", "Nessun effetto dinamico"),
    "Sezione successiva" to listOf("Il numero resta stabile", "Il testo non cambia", "La lista usa lo stesso scroll"),
    "Preferenze" to listOf("Nessuna preferenza caricata", "Nessuna opzione applicata", "Solo contenuto dimostrativo", "Scorrimento verticale"),
    "Riquadro statico" to listOf("Nessuna immagine", "Nessun caricamento Coil", "Nessuna elaborazione", "Nessun aggiornamento periodico"),
    "Prova di scorrimento" to listOf("Riferimento superiore", "Riferimento centrale", "Riferimento inferiore"),
    "Ultimo riferimento" to listOf("Numero sempre leggibile", "Testo statico e breve", "Separatore inferiore"),
    "Contenuto finale" to listOf("Il blocco mantiene altezza", "La spaziatura non cambia", "Fine della sequenza", "Nessuna azione collegata")
)

/** One scroll viewport and one source for the fixed glass chrome. */
@Composable
internal fun PhotoWorkspace(host: PhotoWorkspaceHost) {
    val state = host.state
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val useGlass = host.blurEnabled && state.step != ImageScreen.FILES
    PhotoTheme(dark) {
        val source = remember { HazeState() }
        val pages = rememberSaveableStateHolder()
        var titleHeight by remember { mutableIntStateOf(0) }
        var dockHeight by remember { mutableIntStateOf(0) }
        val density = LocalDensity.current
        CompositionLocalProvider(LocalPhotoGlass provides PhotoGlass(source, host.active && !state.busy, useGlass)) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .then(if (useGlass) Modifier.hazeSource(source) else Modifier)
                .semantics { testTag = "photo-workspace" }) {
                Box(Modifier.fillMaxSize().imePadding()) {
                    pages.SaveableStateProvider(state.step ?: "chooser") {
                        val scroll = rememberLazyListState()
                        LazyColumn(state = scroll,
                            modifier = Modifier.fillMaxSize()
                            .semantics { testTag = "photo-scroll" },
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp,
                            top = with(density) { titleHeight.toDp() } + 12.dp,
                            bottom = with(density) { dockHeight.toDp() } + 20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (PHOTO_DIAGNOSTIC_STATIC_CONTENT && state.step == ImageScreen.FILES) {
                                PHOTO_DIAGNOSTIC_BLOCK_HEIGHTS.forEachIndexed { index, height ->
                                    item("photo-diagnostic-static-$index") {
                                        val (title, lines) = PHOTO_DIAGNOSTIC_CONTENT[index]
                                        Column(Modifier.fillMaxWidth().height(height)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                            .semantics { testTag = "photo-diagnostic-static-$index" },
                                            verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Text((index + 1).toString().padStart(2, '0'),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold)
                                                Text(title, color = MaterialTheme.colorScheme.onSurface,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold)
                                            }
                                            Box(Modifier.fillMaxWidth().height(1.dp)
                                                .background(MaterialTheme.colorScheme.outline))
                                            lines.forEach { line ->
                                                Text(line, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 12.sp, lineHeight = 15.sp, maxLines = 1)
                                            }
                                            Box(Modifier.fillMaxWidth().height(1.dp)
                                                .background(MaterialTheme.colorScheme.outline))
                                        }
                                    }
                                }
                            } else {
                                when (state.step) {
                                    null -> item("chooser") { PhotoChooser(state, host) }
                                    ImageScreen.FILES -> photoFilesItems(state, host)
                                    ImageScreen.COMPRESSION -> photoCompressionItems(state, host)
                                    ImageScreen.HEIC -> {
                                        item("progress-heading") { PhotoStepHeading("03", "Avanzamento") }
                                        item("progress") { host.progressContent() }
                                    }
                                    ImageScreen.DETAILS -> {
                                        item("diagnostics-heading") { PhotoStepHeading("", "Diagnostica") }
                                        item("diagnostics") { host.diagnosticsContent() }
                                    }
                                }
                            }
                        }
                    }
                    PhotoTitle(host, Modifier.align(Alignment.TopCenter).onSizeChanged { titleHeight = it.height }, opaque = state.step == ImageScreen.FILES)
                    PhotoDock(state, host, Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .onSizeChanged { dockHeight = it.height })
                    SideEffect {
                        host.setSystemBarsAppearance(dark, state.step == ImageScreen.FILES)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoTitle(host: PhotoWorkspaceHost, modifier: Modifier, opaque: Boolean) {
    Row(modifier.fillMaxWidth()
        .then(if (opaque) Modifier.background(MaterialTheme.colorScheme.surface) else Modifier.photoGlass(RoundedCornerShape(0.dp)))
        .windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("Foto", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() })
            Text("Meno spazio, più ricordi. · v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalIconButton(onClick = { host.command(PhotoCommand.SETTINGS) }, modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.primary)) {
            Icon(Icons.Rounded.Settings, "Impostazioni immagini", Modifier.size(23.dp))
        }
    }
}

@Composable
private fun PhotoDock(state: PhotoUiState, host: PhotoWorkspaceHost, modifier: Modifier) {
    val dockShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val opaque = state.step == ImageScreen.FILES
    Box(modifier.fillMaxWidth()
        .then(if (opaque) Modifier.background(MaterialTheme.colorScheme.surface, dockShape) else Modifier.photoGlass(dockShape))
        .windowInsetsPadding(WindowInsets.navigationBars)) {
      Column(Modifier.widthIn(max = 440.dp).fillMaxWidth().align(Alignment.Center)
        .padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PhotoButton("Indietro", { host.command(PhotoCommand.BACK) }, Modifier.weight(1f).heightIn(min = 52.dp),
                icon = R.drawable.trial_back, tag = "photo-back", compact = true, chrome = !opaque)
            if (state.step == ImageScreen.FILES || state.step == ImageScreen.COMPRESSION) {
                PhotoButton(if (state.step == ImageScreen.FILES) "Procedi al punto 2" else "Procedi al punto 3",
                    { host.command(PhotoCommand.NEXT) }, Modifier.weight(1f).heightIn(min = 52.dp), icon = R.drawable.trial_next,
                    primary = true, tag = "photo-next", compact = true)
            }
        }
        val shape = RoundedCornerShape(28.dp)
        Row(Modifier.fillMaxWidth().height(56.dp)
            .then(if (opaque) Modifier.background(MaterialTheme.colorScheme.surfaceVariant, shape) else Modifier.photoGlass(shape))
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(5.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            PhotoNavItem("Video", R.drawable.trial_video, false, Modifier.weight(1f)) { host.command(PhotoCommand.VIDEO) }
            PhotoNavItem("Audio", R.drawable.trial_music, false, Modifier.weight(1f)) { host.command(PhotoCommand.AUDIO) }
            PhotoNavItem("Foto", R.drawable.trial_photo, true, Modifier.weight(1f)) {}
            PhotoNavItem("Analisi", R.drawable.trial_grid, false, Modifier.weight(1f)) { host.command(PhotoCommand.ANALYSIS) }
        }
      }
    }
}

@Composable
private fun PhotoNavItem(text: String, icon: Int, selected: Boolean, modifier: Modifier, click: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val fill = if (selected) colors.primaryContainer else colors.surfaceVariant
    val edge = if (selected) colors.primary else colors.outline
    val ink = if (selected) colors.primary else colors.onSurfaceVariant
    Surface(onClick = click, shape = RoundedCornerShape(50),
        color = fill, contentColor = ink,
        border = androidx.compose.foundation.BorderStroke(1.dp, edge),
        modifier = modifier.fillMaxHeight()
        .semantics { this.selected = selected; role = Role.Tab }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(painterResource(icon), null, Modifier.size(20.dp))
            Text(text, fontSize = 11.sp, lineHeight = 13.sp)
        }
    }
}

@Composable
private fun PhotoChooser(state: PhotoUiState, host: PhotoWorkspaceHost) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Da dove cominciamo?", Modifier.padding(top = 10.dp, bottom = 20.dp), style = MaterialTheme.typography.titleLarge)
        PhotoButton("Nuovo lavoro", { host.command(PhotoCommand.NEW) }, Modifier.fillMaxWidth().height(104.dp),
            icon = R.drawable.trial_add, shape = photoJoined(0), tag = "photo-new")
        Spacer(Modifier.height(4.dp))
        PhotoButton("Riprendi sessione", { host.command(PhotoCommand.RESUME) }, Modifier.fillMaxWidth().height(104.dp),
            icon = R.drawable.trial_play, enabled = state.previousEnabled, shape = photoJoined(2), tag = "photo-resume")
        Text(state.previousHint, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun PhotoStepHeading(number: String, title: String, subtitle: String? = null) {
    val featured = number == "01"
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = if (featured) 2.dp else 6.dp, vertical = if (featured) 16.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (featured) 14.dp else 12.dp)) {
        if (number.isNotEmpty()) {
            if (featured) Box(Modifier.size(48.dp).background(colors.primary, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                Text(number, color = colors.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { contentDescription = "Passaggio $number" })
            } else Text(number, color = colors.primary, style = MaterialTheme.typography.titleMedium)
        }
        Column {
            Text(title,
                style = if (featured) MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.SansSerif) else MaterialTheme.typography.titleMedium,
                fontWeight = if (featured) FontWeight.SemiBold else FontWeight.Normal,
                color = if (featured) colors.onSurface else colors.onSurfaceVariant,
                modifier = Modifier.semantics { heading() })
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

