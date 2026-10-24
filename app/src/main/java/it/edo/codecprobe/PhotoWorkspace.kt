package it.edo.codecprobe

import android.animation.ValueAnimator
import android.os.SystemClock
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
internal fun PhotoWorkspace(host: PhotoComposeHost) {
    val state = host.state
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    PhotoTheme(dark) {
        val background = remember { HazeState() }
        val content = remember { HazeState() }
        val glass = PhotoGlass(background, content, host.active && !state.busy)
        CompositionLocalProvider(LocalPhotoGlass provides glass) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing).imePadding().semantics { testTag = "photo-workspace" }) {
                PhotoBackdropCompose(Modifier.matchParentSize().hazeSource(background).hazeSource(content, zIndex = 0f), dark,
                    host.active && !state.busy && ValueAnimator.areAnimatorsEnabled())
                var titleHeight by remember { mutableIntStateOf(0) }
                var dockHeight by remember { mutableIntStateOf(0) }
                val density = LocalDensity.current
                AnimatedContent(
                    targetState = state.step,
                    transitionSpec = { (fadeIn(tween(200)) + slideInVertically(tween(220)) { it / 30 }) togetherWith fadeOut(tween(110)) },
                    label = "photo-step",
                    modifier = Modifier.fillMaxSize().hazeSource(content, zIndex = 1f)
                ) { step ->
                    key(step) {
                        val scroll = rememberScrollState()
                        Column(Modifier.fillMaxSize().verticalScroll(scroll).semantics { testTag = "photo-scroll" }
                            .padding(horizontal = 16.dp)
                            .padding(top = with(density) { titleHeight.toDp() } + 8.dp,
                                bottom = with(density) { dockHeight.toDp() } + 20.dp)) {
                            when (step) {
                                null -> PhotoChooser(state, host)
                                ImageScreen.FILES -> PhotoFilesPane(state, host)
                                ImageScreen.COMPRESSION -> PhotoCompressionPane(state, host)
                                ImageScreen.HEIC -> {
                                    PhotoStepHeading("03", "Avanzamento")
                                    AndroidView(factory = { host.legacyProgress() }, modifier = Modifier.fillMaxWidth())
                                }
                                ImageScreen.DETAILS -> {
                                    PhotoStepHeading("", "Diagnostica")
                                    AndroidView(factory = { host.legacyDiagnostics() }, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
                PhotoTitle(host, Modifier.align(Alignment.TopCenter).onSizeChanged { titleHeight = it.height })
                PhotoDock(state, host, Modifier.align(Alignment.BottomCenter).onSizeChanged { dockHeight = it.height })
            }
        }
    }
}

@Composable
private fun PhotoTitle(host: PhotoComposeHost, modifier: Modifier) {
    Row(modifier.fillMaxWidth().photoGlass(RoundedCornerShape(0.dp), chrome = true)
        .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("Foto", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() })
            Text("Meno spazio, più ricordi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalIconButton(onClick = { host.command(PhotoCommand.SETTINGS) }, modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f), contentColor = MaterialTheme.colorScheme.primary)) {
            Icon(Icons.Rounded.Settings, "Impostazioni immagini", Modifier.size(23.dp))
        }
    }
}

@Composable
private fun PhotoDock(state: PhotoUiState, host: PhotoComposeHost, modifier: Modifier) {
    Column(modifier.widthIn(max = 440.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PhotoButton("Indietro", { host.command(PhotoCommand.BACK) }, Modifier.weight(1f).fillMaxHeight().photoGlass(RoundedCornerShape(28.dp), chrome = true),
                icon = R.drawable.trial_back, tag = "photo-back", compact = true)
            if (state.step == ImageScreen.FILES || state.step == ImageScreen.COMPRESSION) {
                PhotoButton(if (state.step == ImageScreen.FILES) "Procedi al punto 2" else "Procedi al punto 3",
                    { host.command(PhotoCommand.NEXT) }, Modifier.weight(1f).fillMaxHeight(), icon = R.drawable.trial_next,
                    primary = true, tag = "photo-next", compact = true)
            }
        }
        val shape = RoundedCornerShape(28.dp)
        Row(Modifier.fillMaxWidth().height(56.dp).photoGlass(shape, chrome = true)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .6f), shape)
            .padding(5.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            PhotoNavItem("Video", R.drawable.trial_video, false, Modifier.weight(1f)) { host.command(PhotoCommand.VIDEO) }
            PhotoNavItem("Audio", R.drawable.trial_music, false, Modifier.weight(1f)) { host.command(PhotoCommand.AUDIO) }
            PhotoNavItem("Foto", R.drawable.trial_photo, true, Modifier.weight(1f)) {}
            PhotoNavItem("Analisi", R.drawable.trial_grid, false, Modifier.weight(1f)) { host.command(PhotoCommand.ANALYSIS) }
        }
    }
}

@Composable
private fun PhotoNavItem(text: String, icon: Int, selected: Boolean, modifier: Modifier, click: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = click, shape = RoundedCornerShape(23.dp),
        color = if (selected) colors.primaryContainer.copy(alpha = .8f) else Color.Transparent,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = .5f)) else null,
        modifier = modifier.fillMaxHeight().semantics { this.selected = selected; role = Role.Tab }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(painterResource(icon), null, Modifier.size(20.dp), tint = if (selected) colors.primary else colors.onSurfaceVariant)
            Text(text, fontSize = 11.sp, lineHeight = 13.sp, color = if (selected) colors.primary else colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun PhotoChooser(state: PhotoUiState, host: PhotoComposeHost) {
    Text("Da dove cominciamo?", Modifier.padding(top = 10.dp, bottom = 20.dp), style = MaterialTheme.typography.titleLarge)
    PhotoButton("Nuovo lavoro", { host.command(PhotoCommand.NEW) }, Modifier.fillMaxWidth().height(104.dp),
        icon = R.drawable.trial_add, shape = photoJoined(0), tag = "photo-new")
    Spacer(Modifier.height(4.dp))
    PhotoButton("Riprendi sessione", { host.command(PhotoCommand.RESUME) }, Modifier.fillMaxWidth().height(104.dp),
        icon = R.drawable.trial_play, enabled = state.previousEnabled, shape = photoJoined(2), tag = "photo-resume")
    Text(state.previousHint, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun PhotoStepHeading(number: String, title: String, subtitle: String? = null) {
    Row(Modifier.padding(horizontal = 6.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (number.isNotEmpty()) Text(number, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() })
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PhotoBackdropCompose(modifier: Modifier, dark: Boolean, moving: Boolean) {
    var elapsed by rememberSaveable { mutableLongStateOf(0L) }
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(moving) {
        if (!moving) return@LaunchedEffect
        var last = SystemClock.uptimeMillis()
        while (elapsed < 36_000) {
            delay(67) // Decorations update about 15 times per second and then stop.
            val now = SystemClock.uptimeMillis()
            elapsed += (now - last).coerceIn(0, 100)
            last = now
            phase = sin(elapsed * Math.PI / 18_000).toFloat()
        }
    }
    Canvas(modifier) {
        val shift = phase * 10.dp.toPx()
        val blue = if (dark) Color(0x3342618C) else Color(0x88C9DAFB)
        val gold = if (dark) Color(0x338C703B) else Color(0x88F9D99B)
        drawCircle(gold, 58.dp.toPx(), Offset(size.width * .89f + shift, size.height * .08f + shift))
        drawCircle(blue, 80.dp.toPx(), Offset(size.width * .06f - shift, size.height * .34f - shift))
        drawCircle(gold, 75.dp.toPx(), Offset(size.width * .96f + shift, size.height * .63f + shift))
        drawCircle(blue, 100.dp.toPx(), Offset(size.width * .08f - shift, size.height * .92f - shift))
    }
}
