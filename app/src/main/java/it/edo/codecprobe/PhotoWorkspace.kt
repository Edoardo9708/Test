package it.edo.codecprobe

import androidx.compose.foundation.*
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.distinctUntilChanged

/** One scroll viewport and one source for the fixed glass chrome. */
@Composable
internal fun PhotoWorkspace(host: PhotoWorkspaceHost) {
    val state = host.state
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val useGlass = host.blurEnabled
    val chooser = state.step == null
    val revealState = rememberCircularRevealState()
    val firstPage = state.step == null || state.step == ImageScreen.FILES
    val filesStep = state.step == ImageScreen.FILES
    val imeVisible = rememberPhotoImeVisible()
    val hideFilesDock = filesStep && imeVisible
    AppTheme(dark) {
        val source = remember { HazeState() }
        val pages = rememberSaveableStateHolder()
        var titleHeight by remember { mutableIntStateOf(0) }
        var dockHeight by remember { mutableIntStateOf(0) }
        val density = LocalDensity.current
        val backdrop = remember(dark) {
            Brush.linearGradient(if (dark) listOf(Color(0xFF10151E), Color(0xFF182237), Color(0xFF211B31))
                else listOf(Color(0xFFFAF4F0), Color(0xFFEEF1F8), Color(0xFFF3EDF7)))
        }
        CompositionLocalProvider(LocalAppGlassState provides AppGlassState(source, host.active && !state.busy, useGlass)) {
            CircularRevealTransition(
              state = revealState,
              contentKey = state.step ?: "chooser",
              modifier = Modifier.fillMaxSize().semantics { testTag = "photo-workspace" }
            ) {
              Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
              Box(Modifier.fillMaxSize().then(if (firstPage) Modifier.background(backdrop)
                else Modifier.background(MaterialTheme.colorScheme.background))
                .then(if (useGlass) Modifier.hazeSource(source) else Modifier)
              ) {
                if (firstPage) AppAnimatedBackground(
                    modifier = Modifier.fillMaxSize(),
                    enabled = host.active && !state.busy
                )
                Box(Modifier.fillMaxSize().then(if (filesStep) Modifier else Modifier.imePadding())) {
                    pages.SaveableStateProvider(state.step ?: "chooser") {
                        val scroll = rememberLazyListState()
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            LazyColumn(state = scroll,
                                modifier = Modifier.widthIn(max = 560.dp).fillMaxSize()
                                    .then(if (filesStep) Modifier.imePadding() else Modifier)
                                    .semantics { testTag = "photo-scroll" },
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp,
                                    top = with(density) { titleHeight.toDp() } + 12.dp,
                                    bottom = if (hideFilesDock) 20.dp else with(density) { dockHeight.toDp() } + 20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                when (state.step) {
                                    null -> item("chooser") {
                                        PhotoChooser(state, host, Modifier.fillParentMaxHeight()) { action, origin, color ->
                                            revealState.start(origin, color) { action.onClick() }
                                        }
                                    }
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
                }
              }
              Box(Modifier.fillMaxSize().then(if (filesStep) Modifier else Modifier.imePadding())) {
                    AppTopBar("Foto", "JPEG → HEIC · v${BuildConfig.VERSION_NAME}",
                        Modifier.align(Alignment.TopCenter).onSizeChanged { titleHeight = it.height },
                        edgeVisible = firstPage, showSurface = !chooser) {
                        FilledTonalIconButton(onClick = { host.command(PhotoCommand.SETTINGS) }, modifier = Modifier.size(48.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.primary)) {
                            Icon(Icons.Rounded.Settings, "Impostazioni immagini", Modifier.size(23.dp))
                        }
                    }
                    PhotoDock(state, host, hideFilesDock, Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .onSizeChanged { dockHeight = it.height })
                    SideEffect {
                        host.setSystemBarsAppearance(dark, opaque = firstPage && !useGlass)
                    }
              }
              }
            }
        }
    }
}

@Composable
private fun PhotoDock(state: PhotoUiState, host: PhotoWorkspaceHost, hideFilesDock: Boolean, modifier: Modifier) {
    if (state.step == null || state.step == ImageScreen.FILES) {
        PhotoFilesDock(state, host, hideFilesDock, modifier)
    } else {
        PhotoWorkflowDock(state, host, modifier)
    }
}

@Composable
private fun PhotoFilesDock(state: PhotoUiState, host: PhotoWorkspaceHost, hidden: Boolean, modifier: Modifier) {
    Box(modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)) {
        Box(Modifier.widthIn(max = 512.dp).fillMaxWidth().align(Alignment.Center)
            .padding(horizontal = 12.dp, vertical = 6.dp).height(AppDesignTokens.Size.navigationBar), contentAlignment = Alignment.Center) {
            AnimatedVisibility(
                visible = !hidden,
                modifier = Modifier.fillMaxSize().then(if (hidden) Modifier.clearAndSetSemantics {} else Modifier),
                enter = fadeIn(tween(150, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(120, easing = FastOutSlowInEasing)),
                label = "photo-files-navigation"
            ) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val backVisible = state.step != null
                    val nextVisible = state.step == ImageScreen.FILES || state.step == ImageScreen.COMPRESSION
                    AppNavigationArrow(backVisible, !hidden, false, "photo-back", "Indietro", Modifier.size(AppDesignTokens.Size.navigationArrow)) {
                        host.command(PhotoCommand.BACK)
                    }
                    PhotoSectionBar(Modifier.weight(1f), enabled = !hidden) { host.command(it) }
                    AppNavigationArrow(nextVisible, !hidden, true, "photo-next", "Avanti", Modifier.size(AppDesignTokens.Size.navigationArrow)) {
                        host.command(PhotoCommand.NEXT)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoSectionBar(modifier: Modifier, enabled: Boolean, command: (PhotoCommand) -> Unit) {
    AppNavigationSurface(modifier, AppNavigationSurfaceStyle.GlassOutlined) {
        PhotoSectionItem("Video", R.drawable.trial_video, false, enabled, Modifier.weight(1f)) { command(PhotoCommand.VIDEO) }
        PhotoSectionItem("Audio", R.drawable.trial_music, false, enabled, Modifier.weight(1f)) { command(PhotoCommand.AUDIO) }
        PhotoSectionItem("Foto", R.drawable.trial_photo, true, enabled, Modifier.weight(1f)) {}
    }
}

@Composable
private fun PhotoSectionItem(text: String, icon: Int, selected: Boolean, enabled: Boolean, modifier: Modifier, click: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (selected) colors.primaryContainer else Color.Transparent,
        tween(180, easing = FastOutSlowInEasing), label = "photo-section-container"
    )
    val content by animateColorAsState(
        if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        tween(180, easing = FastOutSlowInEasing), label = "photo-section-content"
    )
    Surface(onClick = { if (enabled) click() }, enabled = enabled, modifier = modifier.height(52.dp)
        .semantics { role = Role.Tab; this.selected = selected; contentDescription = text; testTag = "photo-nav-${text.lowercase()}"; if (!enabled) hideFromAccessibility() },
        shape = CircleShape, color = container, contentColor = content) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, Modifier.size(AppDesignTokens.Size.navigationIcon))
        }
    }
}

@Composable
private fun PhotoWorkflowDock(state: PhotoUiState, host: PhotoWorkspaceHost, modifier: Modifier) {
    val dockShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val opaque = state.step == ImageScreen.FILES
    Box(modifier.fillMaxWidth()
        .then(if (opaque) Modifier.background(MaterialTheme.colorScheme.surface, dockShape) else Modifier.appGlass(dockShape))
        .windowInsetsPadding(WindowInsets.navigationBars)) {
      Column(Modifier.widthIn(max = 440.dp).fillMaxWidth().align(Alignment.Center)
        .padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppButton("Indietro", { host.command(PhotoCommand.BACK) }, Modifier.weight(1f).heightIn(min = 58.dp),
                icon = R.drawable.trial_back, tag = "photo-back", compact = true, chrome = !opaque)
            AnimatedVisibility(
                visible = state.step == ImageScreen.FILES || state.step == ImageScreen.COMPRESSION,
                modifier = Modifier.weight(1f),
                enter = expandHorizontally(tween(240, easing = FastOutSlowInEasing), expandFrom = Alignment.End) +
                    fadeIn(tween(170, easing = FastOutSlowInEasing)),
                exit = shrinkHorizontally(tween(220, easing = FastOutSlowInEasing), shrinkTowards = Alignment.End) +
                    fadeOut(tween(140))
            ) {
                AppButton(if (state.step == ImageScreen.FILES) "Procedi al punto 2" else "Procedi al punto 3",
                    { host.command(PhotoCommand.NEXT) }, Modifier.fillMaxWidth().heightIn(min = 58.dp), icon = R.drawable.trial_next,
                    primary = true, tag = "photo-next", compact = true)
            }
        }
        AppNavigationSurface(Modifier.fillMaxWidth(),
            if (opaque) AppNavigationSurfaceStyle.OpaqueVariant else AppNavigationSurfaceStyle.Glass) {
            PhotoNavItem("Video", R.drawable.trial_video, false, Modifier.weight(1f)) { host.command(PhotoCommand.VIDEO) }
            PhotoNavItem("Audio", R.drawable.trial_music, false, Modifier.weight(1f)) { host.command(PhotoCommand.AUDIO) }
            PhotoNavItem("Foto", R.drawable.trial_photo, true, Modifier.weight(1f)) {}
        }
      }
    }
}

@Composable
private fun rememberPhotoImeVisible(): Boolean {
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(density, ime) {
        snapshotFlow { ime.getBottom(density) > 0 }
            .distinctUntilChanged()
            .collect { visible = it }
    }
    return visible
}

@Composable
private fun RowScope.PhotoNavItem(text: String, icon: Int, selected: Boolean, modifier: Modifier, click: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    NavigationBarItem(
        selected = selected,
        onClick = click,
        icon = { Icon(painterResource(icon), contentDescription = text, modifier = Modifier.size(AppDesignTokens.Size.navigationIcon)) },
        label = null,
        modifier = modifier,
        alwaysShowLabel = false,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = colors.onPrimaryContainer,
            selectedTextColor = colors.onPrimaryContainer,
            indicatorColor = colors.primaryContainer,
            unselectedIconColor = colors.onSurfaceVariant,
            unselectedTextColor = colors.onSurfaceVariant
        )
    )
}

@Composable
private fun PhotoChooser(
    state: PhotoUiState,
    host: PhotoWorkspaceHost,
    modifier: Modifier = Modifier,
    onAction: (StartAction, androidx.compose.ui.geometry.Offset, Color) -> Unit
) {
    val actions = buildList {
        add(StartAction(
            icon = R.drawable.trial_add,
            text = "Nuovo lavoro",
            variant = StartActionVariant.Primary,
            tag = "photo-new",
            onClick = { host.command(PhotoCommand.NEW) }
        ))
        if (state.previousEnabled) add(StartAction(
            icon = R.drawable.trial_play,
            text = "Riprendi",
            variant = StartActionVariant.Secondary,
            tag = "photo-resume",
            onClick = { host.command(PhotoCommand.RESUME) }
        ))
    }
    StartActionLayout(actions, modifier, onAction)
}

@Composable
internal fun PhotoStepHeading(number: String, title: String, subtitle: String? = null) {
    val featured = number == "01"
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = if (featured) 2.dp else 6.dp, vertical = if (featured) 16.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (featured) 14.dp else 12.dp)) {
        if (number.isNotEmpty()) {
            if (featured) Box(Modifier.size(48.dp).background(colors.primary, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                Text(number, color = colors.onPrimary, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.semantics { contentDescription = "Passaggio $number" })
            } else Text(number, color = colors.primary, style = MaterialTheme.typography.titleMedium)
        }
        Column {
            Text(title,
                style = if (featured) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                color = if (featured) colors.onSurface else colors.onSurfaceVariant,
                modifier = Modifier.semantics { heading() })
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

