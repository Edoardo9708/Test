package it.edo.codecprobe

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
internal fun PhotoCompressionPane(state: PhotoUiState, host: PhotoComposeHost) {
    PhotoStepHeading("02", "Compressione")
    PhotoPanel {
        Text("Formato di uscita", style = MaterialTheme.typography.labelLarge)
        Text("HEIC · dimensioni originali", style = MaterialTheme.typography.bodyLarge)
    }
    Spacer(Modifier.height(28.dp))
    PhotoPanel {
        var quality by remember(state.quality) { mutableFloatStateOf(state.quality.toFloat()) }
        Text("Qualità · ${quality.roundToInt()}/100", style = MaterialTheme.typography.titleMedium)
        Slider(value = quality, onValueChange = { quality = it }, onValueChangeFinished = { host.quality(quality.roundToInt()) },
            valueRange = 40f..95f, enabled = !state.busy, modifier = Modifier.semantics { testTag = "photo-quality" })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(50, 55, 60).forEach { value ->
                PhotoButton(value.toString(), { host.quality(value) }, Modifier.weight(1f), selected = state.quality == value, enabled = !state.busy)
            }
        }
        Text("Più qualità conserva più dettagli e può occupare più spazio.", style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(28.dp))
    PhotoPanel {
        Text("Preset", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("fast" to "Veloce", "medium" to "Medio", "slow" to "Lento").forEachIndexed { index, (value, label) ->
                PhotoButton(label, { host.preset(index) }, Modifier.weight(1f), selected = state.preset == value, enabled = !state.busy)
            }
        }
        Text("Lento prova più alternative e richiede più tempo.", style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(28.dp))
    PhotoPanel {
        var expanded by rememberSaveable { mutableStateOf(false) }
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) { Text("Opzioni avanzate") }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tune", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("standard", "ssim", "psnr").forEachIndexed { index, value ->
                        PhotoButton(value, { host.tune(index) }, Modifier.weight(1f), selected = state.tune == value, enabled = !state.busy)
                    }
                }
            }
        }
    }
}
