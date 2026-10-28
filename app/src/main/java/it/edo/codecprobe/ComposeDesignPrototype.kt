package it.edo.codecprobe

import android.animation.ValueAnimator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

internal enum class PrototypeSection(val label: String, val icon: Int, val subtitle: String) {
    VIDEO("Video", R.drawable.trial_video, "HEVC · x265"),
    AUDIO("Audio", R.drawable.trial_music, "Opus · VBR"),
    PHOTO("Foto", R.drawable.trial_photo, "JPEG → HEIC"),
    ANALYSIS("Analisi", R.drawable.trial_grid, "Struttura dei file")
}

private enum class PrototypePage { START, FILES, OPTIONS, RUN, RESULTS, DETAILS, DIAGNOSTICS, TREE, HEX }
private data class PrototypeRoute(val section: PrototypeSection, val page: PrototypePage) : java.io.Serializable
private enum class DemoRunStatus(val label: String) {
    IDLE("Pronto"), PREPARING("Preparazione dimostrativa"), RUNNING("Elaborazione dimostrativa in corso"), PAUSED("Anteprima in pausa"),
    COMPLETE("Anteprima completata · nessun file convertito"), ERROR("Errore dimostrativo")
}

private fun PrototypeSection.toWorkspaceSection() = when (this) {
    PrototypeSection.VIDEO -> AppWorkspaceSection.VIDEO
    PrototypeSection.AUDIO -> AppWorkspaceSection.AUDIO
    PrototypeSection.PHOTO -> AppWorkspaceSection.PHOTO
    PrototypeSection.ANALYSIS -> AppWorkspaceSection.ANALYSIS
}

private fun AppWorkspaceSection.toPrototypeSection() = when (this) {
    AppWorkspaceSection.VIDEO -> PrototypeSection.VIDEO
    AppWorkspaceSection.AUDIO -> PrototypeSection.AUDIO
    AppWorkspaceSection.PHOTO -> PrototypeSection.PHOTO
    AppWorkspaceSection.ANALYSIS -> PrototypeSection.ANALYSIS
}

@Composable
internal fun ComposeDesignPrototype(onExit: () -> Unit, onOpenPhoto: () -> Unit) {
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    var dark by remember(systemDark) { mutableStateOf(systemDark) }
    var settingsOpen by remember { mutableStateOf(false) }
    var demoResumeEnabled by remember { mutableStateOf(false) }
    var route by remember { mutableStateOf(PrototypeRoute(PrototypeSection.VIDEO, PrototypePage.START)) }
    val selectedSamples = remember { mutableStateMapOf<PrototypeSection, Boolean>() }
    val runStatuses = remember { mutableStateMapOf<PrototypeSection, DemoRunStatus>() }
    val selectedFolder = remember { mutableStateMapOf<PrototypeSection, String>() }
    val revealState = rememberCircularRevealState()
    val pages = rememberSaveableStateHolder()
    val haze = remember { HazeState() }
    val glass = remember { AppGlassState(haze, enabled = true, blurEnabled = true) }
    val backdrop = remember(dark) {
        Brush.linearGradient(if (dark) listOf(Color(0xFF10151E), Color(0xFF182237), Color(0xFF211B31))
            else listOf(Color(0xFFFAF4F0), Color(0xFFEEF1F8), Color(0xFFF3EDF7)))
    }

    fun go(next: PrototypePage) { route = route.copy(page = next) }
    fun back() {
        route = when (route.page) {
            PrototypePage.START -> { onExit(); route }
            PrototypePage.FILES -> route.copy(page = PrototypePage.START)
            PrototypePage.OPTIONS -> route.copy(page = PrototypePage.FILES)
            PrototypePage.RUN -> route.copy(page = PrototypePage.OPTIONS)
            PrototypePage.RESULTS -> route.copy(page = PrototypePage.RUN)
            PrototypePage.DETAILS -> route.copy(page = if (route.section == PrototypeSection.PHOTO) PrototypePage.FILES else PrototypePage.RESULTS)
            PrototypePage.DIAGNOSTICS -> route.copy(page = PrototypePage.RESULTS)
            PrototypePage.TREE -> route.copy(page = PrototypePage.FILES)
            PrototypePage.HEX -> route.copy(page = PrototypePage.TREE)
        }
    }
    BackHandler(enabled = true) { back() }

    AppTheme(dark) {
        CompositionLocalProvider(LocalAppGlassState provides glass) {
            CircularRevealTransition(
                state = revealState,
                contentKey = route,
                modifier = Modifier.fillMaxSize().semantics { testTag = "compose-design-prototype" }
            ) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    Box(Modifier.fillMaxSize().background(backdrop).hazeSource(haze))
                    AppAnimatedBackground(Modifier.fillMaxSize(), enabled = true)
                    Column(Modifier.fillMaxSize().imePadding()) {
                        AppTopBar(
                            title = route.section.label,
                            subtitle = "${route.section.subtitle} · Anteprima UI",
                            modifier = Modifier,
                            edgeVisible = false,
                            showSurface = false
                        ) {
                            FilledTonalIconButton(onClick = { settingsOpen = true }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Rounded.Settings, contentDescription = "Impostazioni anteprima")
                            }
                        }
                        AnimatedContent(
                            targetState = route,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            transitionSpec = {
                                val duration = if (initialState.section == targetState.section) 290 else 200
                                if (!ValueAnimator.areAnimatorsEnabled()) {
                                    EnterTransition.None togetherWith ExitTransition.None
                                } else if (initialState.section != targetState.section) {
                                    (fadeIn(tween(duration, easing = FastOutSlowInEasing)) togetherWith
                                        fadeOut(tween(duration / 2, easing = FastOutSlowInEasing)))
                                } else {
                                    val forward = targetState.page.ordinal > initialState.page.ordinal
                                    val enterOffset: (Int) -> Int = { full -> if (forward) full / 12 else -full / 12 }
                                    val exitOffset: (Int) -> Int = { full -> if (forward) -full / 12 else full / 12 }
                                    (slideInHorizontally(tween(duration, easing = FastOutSlowInEasing), enterOffset) +
                                        fadeIn(tween(duration, easing = FastOutSlowInEasing))) togetherWith
                                        (slideOutHorizontally(tween(duration, easing = FastOutSlowInEasing), exitOffset) +
                                            fadeOut(tween(duration, easing = FastOutSlowInEasing)))
                                }
                            },
                            label = "prototype-route"
                        ) { target ->
                            pages.SaveableStateProvider("${target.section.name}-${target.page.name}") {
                                PrototypePageContent(
                                    route = target,
                                    hasSamples = selectedSamples[target.section] == true,
                                    runStatus = runStatuses[target.section] ?: DemoRunStatus.IDLE,
                                    folder = selectedFolder[target.section] ?: "Cartella dimostrativa",
                                    resumeVisible = demoResumeEnabled,
                                    onToggleSamples = { selectedSamples[target.section] = selectedSamples[target.section] != true },
                                    onNext = { go(nextPage(target)) },
                                    onDetails = { go(PrototypePage.DETAILS) },
                                    onBack = { back() },
                                    onRunStatus = { runStatuses[target.section] = it },
                                    onFolder = {
                                        selectedFolder[target.section] = if ((selectedFolder[target.section] ?: "") == "Cartella dimostrativa")
                                            "Memoria dispositivo / Convertiti (demo)" else "Cartella dimostrativa"
                                    },
                                    onStartAction = { action, center, color ->
                                        revealState.start(center, color) {
                                            route = target.copy(page = if (target.section == PrototypeSection.ANALYSIS) PrototypePage.FILES else PrototypePage.FILES)
                                            if (action.text.startsWith("Riprendi")) runStatuses[target.section] = DemoRunStatus.PAUSED
                                        }
                                    }
                                )
                            }
                        }
                        PrototypeBottomBar(route.section) { section ->
                            if (section == PrototypeSection.PHOTO) onOpenPhoto()
                            else route = PrototypeRoute(section, PrototypePage.START)
                        }
                    }
                }
            }
            if (settingsOpen) {
                AppDialogSurface(onDismissRequest = { settingsOpen = false }) {
                    Text("Anteprima dell’interfaccia", style = MaterialTheme.typography.titleLarge)
                    Text("Le modifiche di questa modalità non salvano preferenze di conversione.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AppSwitchRow("Tema scuro", dark, true, { dark = it }, "prototype-dark-theme")
                    AppSwitchRow("Mostra sessione recuperabile demo", demoResumeEnabled, true,
                        { demoResumeEnabled = it }, "prototype-demo-session")
                    AppButton("Chiudi", { settingsOpen = false }, Modifier.fillMaxWidth(), primary = true)
                }
            }
        }
    }
}

private fun nextPage(route: PrototypeRoute): PrototypePage = when (route.page) {
    PrototypePage.START -> if (route.section == PrototypeSection.ANALYSIS) PrototypePage.FILES else PrototypePage.FILES
    PrototypePage.FILES -> if (route.section == PrototypeSection.ANALYSIS) PrototypePage.TREE else PrototypePage.OPTIONS
    PrototypePage.OPTIONS -> PrototypePage.RUN
    PrototypePage.RUN -> PrototypePage.RESULTS
    PrototypePage.RESULTS -> if (route.section == PrototypeSection.ANALYSIS) PrototypePage.HEX else PrototypePage.DIAGNOSTICS
    PrototypePage.DETAILS -> PrototypePage.RESULTS
    PrototypePage.DIAGNOSTICS -> PrototypePage.START
    PrototypePage.TREE -> PrototypePage.HEX
    PrototypePage.HEX -> PrototypePage.TREE
}

@Composable
private fun PrototypeBottomBar(selected: PrototypeSection, onSelected: (PrototypeSection) -> Unit) {
    AppWorkspaceNavigationBar(
        selected = selected.toWorkspaceSection(),
        onSectionSelected = { onSelected(it.toPrototypeSection()) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
        style = AppNavigationSurfaceStyle.GlassOutlined,
        tagPrefix = "prototype-tab"
    )
}

@Composable
private fun PrototypePageContent(
    route: PrototypeRoute,
    hasSamples: Boolean,
    runStatus: DemoRunStatus,
    folder: String,
    resumeVisible: Boolean,
    onToggleSamples: () -> Unit,
    onNext: () -> Unit,
    onDetails: () -> Unit,
    onBack: () -> Unit,
    onRunStatus: (DemoRunStatus) -> Unit,
    onFolder: () -> Unit,
    onStartAction: (StartAction, androidx.compose.ui.geometry.Offset, Color) -> Unit
) {
    if (route.page == PrototypePage.START) {
        val primaryText = if (route.section == PrototypeSection.ANALYSIS) "Analizza file" else "Nuovo lavoro"
        val actions = buildList {
            add(StartAction(R.drawable.trial_add, primaryText, tag = "prototype-new") {})
            if (resumeVisible && route.section != PrototypeSection.ANALYSIS) {
                add(StartAction(R.drawable.trial_play, "Riprendi", StartActionVariant.Secondary, tag = "prototype-resume") {})
            }
        }
        Box(Modifier.fillMaxSize().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text("${route.section.label} · preparati a iniziare", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.semantics { testTag = "prototype-start-heading" })
                StartActionLayout(actions, Modifier.fillMaxWidth().heightIn(min = 230.dp), onStartAction)
                Text("ANTEPRIMA · dati dimostrativi, nessun file verrà elaborato",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().semantics { testTag = "prototype-content" },
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(pageTitle(route), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                AppButton("Indietro", onBack, compact = true)
            }
        }
        item {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .78f), shape = CircleShape) {
                Text("ANTEPRIMA UI · dati dimostrativi isolati", Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        when (route.page) {
            PrototypePage.FILES -> item {
                FilesPanel(route.section, hasSamples, folder, onToggleSamples, onFolder, onNext, onDetails)
            }
            PrototypePage.OPTIONS -> item { OptionsPanel(route.section, onNext) }
            PrototypePage.RUN -> item { RunPanel(route.section, runStatus, onRunStatus, onNext) }
            PrototypePage.RESULTS -> item { ResultsPanel(route.section, runStatus, onNext, onDetails) }
            PrototypePage.DETAILS -> item { DetailsPanel(route.section, onNext) }
            PrototypePage.DIAGNOSTICS -> item { DiagnosticsPanel(route.section, onNext) }
            PrototypePage.TREE -> item { AnalysisTreePanel(onNext) }
            PrototypePage.HEX -> item { AnalysisHexPanel(onNext) }
            PrototypePage.START -> Unit
        }
    }
}

@Composable
private fun FilesPanel(
    section: PrototypeSection, hasSamples: Boolean, folder: String,
    toggleSamples: () -> Unit, onFolder: () -> Unit, onNext: () -> Unit, onDetails: () -> Unit
) {
    if (section == PrototypeSection.ANALYSIS) {
        AppPanel {
                Text("Sorgenti disponibili · demo", style = MaterialTheme.typography.titleMedium)
                Text("L’elenco reale si popola dai file selezionati nelle sezioni dell’app.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                AppButton("Mostra esempi JPEG e MP4", toggleSamples, Modifier.fillMaxWidth(), icon = R.drawable.trial_add)
                if (hasSamples) {
                    AppButton("vacanza.jpg · JPEG", onNext, Modifier.fillMaxWidth(), compact = true)
                    AppButton("ripresa.mp4 · MP4", onNext, Modifier.fillMaxWidth(), compact = true)
                }
        }
    } else {
        AppPanelGroup(spacing = AppDesignTokens.Spacing.groupedPanels) {
            repeat(3) { panel -> AppGroupPanel(position = panel, count = 3) {
                when (panel) {
                    0 -> {
                        Text(selectionTitle(section), style = MaterialTheme.typography.titleMedium)
                        Text(if (hasSamples) "3 elementi dimostrativi selezionati" else emptyMessage(section),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (hasSamples) sampleNames(section).forEach { name ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(painterResource(section.icon), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(name, Modifier.weight(1f).padding(start = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("✓", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        AppButton(if (hasSamples) "Svuota esempi" else "Aggiungi file dimostrativi", toggleSamples,
                            Modifier.fillMaxWidth(), icon = if (hasSamples) null else R.drawable.trial_add)
                    }
                    1 -> {
                        Text("Destinazione · stato visivo", style = MaterialTheme.typography.titleMedium)
                        Text(folder, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        AppButton("Cambia cartella (demo)", onFolder, Modifier.fillMaxWidth(), compact = true)
                    }
                    else -> {
                        Text("${section.label} · stato dei contenuti", style = MaterialTheme.typography.titleMedium)
                        if (section == PrototypeSection.PHOTO) {
                            Text("JPEG · 4032 × 3024 · 3,82 MB · EXIF disponibile (demo)")
                            AppButton("Apri dettagli", onDetails, Modifier.fillMaxWidth(), compact = true)
                        } else if (section == PrototypeSection.AUDIO) {
                            var lyrics by remember { mutableStateOf(false) }
                            AppSwitchRow("Testi .lrc abbinati (demo)", lyrics, true, { lyrics = it }, "prototype-lyrics")
                        } else {
                            AppSwitchRow("Mantieni metadati disponibili (demo)", true, true, {}, "prototype-metadata")
                        }
                    }
                }
            } }
        }
    }
    Spacer(Modifier.height(2.dp))
    AppButton("Continua", onNext, Modifier.fillMaxWidth(), primary = true, enabled = hasSamples)
    if (!hasSamples) {
        Text(if (section == PrototypeSection.ANALYSIS) "Aggiungi sorgenti demo per aprire l’albero." else "Aggiungi almeno un elemento dimostrativo per proseguire.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OptionsPanel(section: PrototypeSection, onNext: () -> Unit) {
    AppPanelGroup {
        AppGroupPanel(position = 0, count = 3) {
            when (section) {
                PrototypeSection.VIDEO -> VideoOptions()
                PrototypeSection.AUDIO -> AudioOptions()
                PrototypeSection.PHOTO -> PhotoOptions()
                PrototypeSection.ANALYSIS -> Text("L’analisi non prevede opzioni di conversione.")
            }
        }
        AppGroupPanel(position = 1, count = 3) {
            AppSwitchRow("Salva solo se più piccolo (demo)", true, true, {}, "prototype-only-smaller")
            AppSwitchRow("Mantieni schermo acceso (demo)", false, true, {}, "prototype-keep-screen")
        }
        AppGroupPanel(position = 2, count = 3) {
            Text("Impostazioni dimostrative", style = MaterialTheme.typography.titleSmall)
            Text("Questi controlli descrivono il flusso previsto e non modificano le preferenze del motore reale.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    AppButton("Vai all’elaborazione dimostrativa", onNext, Modifier.fillMaxWidth(), primary = true)
}

@Composable
private fun VideoOptions() {
    var crf by remember { mutableFloatStateOf(26f) }
    var preset by remember { mutableIntStateOf(1) }
    Text("HEVC · qualità CRF ${crf.toInt()}", style = MaterialTheme.typography.titleMedium)
    AppSlider(crf, { crf = it }, Modifier.fillMaxWidth(), valueRange = 18f..35f)
    Text("Preset x265", style = MaterialTheme.typography.labelLarge)
    AppButtonGroup(3) { index, modifier -> AppButton(listOf("Fast", "Medium", "Slow")[index], { preset = index }, modifier, selected = preset == index, compact = true) }
    Text("Modalità CPU", style = MaterialTheme.typography.labelLarge)
    AppButtonGroup(3) { index, modifier -> AppButton(listOf("Auto", "Equilibrata", "Massima")[index], {}, modifier, selected = index == 1, compact = true) }
}

@Composable
private fun AudioOptions() {
    var bitrate by remember { mutableFloatStateOf(128f) }
    Text("Opus · bitrate ${bitrate.toInt()} kbit/s", style = MaterialTheme.typography.titleMedium)
    AppSlider(bitrate, { bitrate = it }, Modifier.fillMaxWidth(), valueRange = 64f..256f)
    AppButtonGroup(3) { index, modifier ->
        val value = listOf(96, 128, 160)[index]
        AppButton("$value kbit/s", { bitrate = value.toFloat() }, modifier, selected = bitrate.toInt() == value, compact = true)
    }
}

@Composable
private fun PhotoOptions() {
    var quality by remember { mutableFloatStateOf(65f) }
    Text("Qualità HEIC · ${quality.toInt()}/100", style = MaterialTheme.typography.titleMedium)
    AppSlider(quality, { quality = it }, Modifier.fillMaxWidth(), valueRange = 40f..95f)
    AppButtonGroup(3) { index, modifier ->
        val value = listOf(50, 55, 60)[index]
        AppButton("$value", { quality = value.toFloat() }, modifier, selected = quality.toInt() == value, compact = true)
    }
}

@Composable
private fun RunPanel(section: PrototypeSection, status: DemoRunStatus, change: (DemoRunStatus) -> Unit, onNext: () -> Unit) {
    AppPanelGroup {
        AppGroupPanel(position = 0, count = 2) {
            Text("Stato anteprima · dimostrativo", style = MaterialTheme.typography.titleMedium)
            Text(status.label, color = when (status) {
                DemoRunStatus.ERROR -> MaterialTheme.colorScheme.error
                DemoRunStatus.COMPLETE -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            })
            if (status == DemoRunStatus.PREPARING || status == DemoRunStatus.RUNNING || status == DemoRunStatus.PAUSED) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (status == DemoRunStatus.IDLE) AppButton("Prepara anteprima", { change(DemoRunStatus.PREPARING) }, Modifier.fillMaxWidth())
            if (status == DemoRunStatus.PREPARING) AppButton("Avvia anteprima", { change(DemoRunStatus.RUNNING) }, Modifier.fillMaxWidth(), primary = true)
            if (status == DemoRunStatus.RUNNING) AppButton("Pausa anteprima", { change(DemoRunStatus.PAUSED) }, Modifier.fillMaxWidth())
            if (status == DemoRunStatus.PAUSED) AppButton("Riprendi anteprima", { change(DemoRunStatus.RUNNING) }, Modifier.fillMaxWidth(), primary = true)
        }
        AppGroupPanel(position = 1, count = 2) {
            Text("Prova degli stati", style = MaterialTheme.typography.titleSmall)
            AppButtonGroup(2) { index, modifier ->
                AppButton(if (index == 0) "Completa demo" else "Errore demo",
                    { change(if (index == 0) DemoRunStatus.COMPLETE else DemoRunStatus.ERROR) }, modifier,
                    destructive = index == 1, compact = true)
            }
            AppButton("Annulla anteprima", { change(DemoRunStatus.IDLE) }, Modifier.fillMaxWidth(), compact = true)
            Text("Non viene avviato alcun codec né scritto alcun file.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (status == DemoRunStatus.COMPLETE || status == DemoRunStatus.ERROR) {
        AppButton(if (status == DemoRunStatus.COMPLETE) "Apri resoconto demo" else "Apri dettagli errore demo",
            onNext, Modifier.fillMaxWidth(), primary = status == DemoRunStatus.COMPLETE)
    }
}

@Composable
private fun ResultsPanel(section: PrototypeSection, status: DemoRunStatus, onNext: () -> Unit, onDetails: () -> Unit) {
    AppPanel {
        Text("Report lavori · dimostrativo", style = MaterialTheme.typography.titleMedium)
        if (status == DemoRunStatus.COMPLETE) {
            sampleNames(section).forEachIndexed { index, name ->
                Text("${index + 1}. $name · esito di anteprima", maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text("Riepilogo e dimensioni sono dati dimostrativi, non risultati di conversione.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(if (status == DemoRunStatus.ERROR) "Dettagli dell’errore dimostrativo" else "Nessun risultato reale o dimostrativo completato.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AppButton("Apri dettagli", onDetails, Modifier.fillMaxWidth(), compact = true)
    }
    AppButton("Diagnostica e log dimostrativi", onNext, Modifier.fillMaxWidth())
}

@Composable
private fun DetailsPanel(section: PrototypeSection, onNext: () -> Unit) {
    AppPanel {
        Text("Dettagli ${section.label.lowercase()} · demo", style = MaterialTheme.typography.titleMedium)
        listOf("Nome lungo: vacanza_estiva_2026_originale_finale.jpg", "Dimensioni: 4032 × 3024", "Metadati: anteprima", "Percorso: /storage/emulated/0/Convertiti (demo)")
            .forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        AppButton("Mostra diagnostica", onNext, Modifier.fillMaxWidth())
    }
}

@Composable
private fun DiagnosticsPanel(section: PrototypeSection, onNext: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    AppPanel {
        Text("Diagnostica ${section.label.lowercase()}", style = MaterialTheme.typography.titleMedium)
        Text("Le funzioni reali di esportazione restano nel workspace funzionale.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppButton(if (expanded) "Nascondi log demo" else "Mostra log demo", { expanded = !expanded }, Modifier.fillMaxWidth())
        if (expanded) Text("[demo] Stato grafico visualizzato. Nessun log applicativo letto o modificato.",
            style = MaterialTheme.typography.bodySmall)
    }
    AppButton("Torna all’inizio", onNext, Modifier.fillMaxWidth())
}

@Composable
private fun AnalysisTreePanel(onNext: () -> Unit) {
    AppPanel {
        Text("Struttura · esempio dimostrativo", style = MaterialTheme.typography.titleMedium)
        listOf("SOI · inizio immagine", "APP1 · EXIF e XMP", "DQT · tabelle quantizzazione", "SOS · dati compressi").forEachIndexed { index, item ->
            AppButton("$item · ${index * 128} byte", onNext, Modifier.fillMaxWidth(), compact = true)
        }
        Text("Offset e dimensioni sono esempi grafici non letti da un file.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AnalysisHexPanel(onNext: () -> Unit) {
    AppPanel {
        Text("HEX · esempio dimostrativo", style = MaterialTheme.typography.titleMedium)
        Text("00000000  FF D8 FF E0 00 10 4A 46 49 46 00 01\n0000000C  01 00 00 01 00 01 00 00 FF DB 00 43",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
        AppButton("Livello superiore", onNext, Modifier.fillMaxWidth())
    }
}

private fun pageTitle(route: PrototypeRoute): String = when (route.page) {
    PrototypePage.START -> "${route.section.label} · inizio"
    PrototypePage.FILES -> if (route.section == PrototypeSection.ANALYSIS) "Sorgenti" else "File e destinazione"
    PrototypePage.OPTIONS -> "Configurazione"
    PrototypePage.RUN -> "Elaborazione · demo"
    PrototypePage.RESULTS -> "Risultati · demo"
    PrototypePage.DETAILS -> "Dettagli · demo"
    PrototypePage.DIAGNOSTICS -> "Diagnostica"
    PrototypePage.TREE -> "Struttura del file · demo"
    PrototypePage.HEX -> "Vista HEX · demo"
}

private fun selectionTitle(section: PrototypeSection) = when (section) {
    PrototypeSection.VIDEO -> "Video selezionati"
    PrototypeSection.AUDIO -> "Brani selezionati"
    PrototypeSection.PHOTO -> "Foto selezionate"
    PrototypeSection.ANALYSIS -> "Sorgenti"
}

private fun emptyMessage(section: PrototypeSection) = when (section) {
    PrototypeSection.VIDEO -> "Nessun video selezionato"
    PrototypeSection.AUDIO -> "Nessun brano selezionato"
    PrototypeSection.PHOTO -> "Nessuna foto selezionata"
    PrototypeSection.ANALYSIS -> "Nessuna sorgente disponibile"
}

private fun sampleNames(section: PrototypeSection): List<String> = when (section) {
    PrototypeSection.VIDEO -> listOf("ripresa_mare_2026.mp4", "compleanno_originale.mkv", "clip_lunga_con_nome_esteso.mov")
    PrototypeSection.AUDIO -> listOf("concerto_live.flac", "registrazione_lunga.m4a", "traccia_demo.mp3")
    PrototypeSection.PHOTO -> listOf("vacanza_estiva_2026.jpg", "famiglia_originale.jpg", "panorama_nome_molto_lungo.jpeg")
    PrototypeSection.ANALYSIS -> listOf("vacanza.jpg", "ripresa.mp4", "archivio_audio.m4a")
}
