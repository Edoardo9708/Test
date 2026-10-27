package it.edo.codecprobe

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal fun LazyListScope.photoCompressionItems(state: PhotoUiState, host: PhotoWorkspaceHost) {
    item("compression-heading") { PhotoStepHeading("02", "Compressione") }
    item("compression-0") {
        AppPanel {
            Text("Formato di uscita", style = MaterialTheme.typography.labelLarge)
            Text("HEIC · dimensioni originali", style = MaterialTheme.typography.bodyLarge)
        }
    }
    item("compression-1") {
        AppPanel {
            var quality by remember(state.quality) { mutableFloatStateOf(state.quality.toFloat()) }
            Text("Qualità · ${quality.roundToInt()}/100", style = MaterialTheme.typography.titleMedium)
            AppSlider(value = quality, onValueChange = { quality = it }, onValueChangeFinished = { host.quality(quality.roundToInt()) },
                valueRange = 40f..95f, enabled = !state.busy, modifier = Modifier.semantics { testTag = "photo-quality" })
            AppButtonGroup(3) { index, button ->
                val value = qualityPresets[index]
                AppButton(value.toString(), { host.quality(value) }, button,
                    selected = state.quality == value, enabled = !state.busy, shape = appJoinedShape(index, false))
            }
            Text("Più qualità conserva più dettagli e può occupare più spazio.", style = MaterialTheme.typography.bodySmall)
        }
    }
    item("compression-2") {
        AppPanel {
            Text("Preset", style = MaterialTheme.typography.titleMedium)
            AppButtonGroup(3) { index, button ->
                val (value, label) = compressionPresets[index]
                AppButton(label, { host.preset(index) }, button, selected = state.preset == value,
                    enabled = !state.busy, shape = appJoinedShape(index, false))
            }
            Text("Lento prova più alternative e richiede più tempo.", style = MaterialTheme.typography.bodySmall)
        }
    }
    item("compression-3") {
        AppPanel {
            var expanded by rememberSaveable { mutableStateOf(false) }
            Column(Modifier.fillMaxWidth()) {
                TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()
                    .semantics { selected = expanded; testTag = "photo-advanced-toggle" }) {
                    Text(if (expanded) "Nascondi opzioni avanzate" else "Mostra opzioni avanzate")
                }
                AppFold(expanded) {
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Tune", style = MaterialTheme.typography.titleSmall)
                        AppButtonGroup(3) { index, button ->
                            val value = compressionTunes[index]
                            AppButton(value, { host.tune(index) }, button, selected = state.tune == value,
                                enabled = !state.busy, shape = appJoinedShape(index, false))
                        }
                    }
                }
            }
        }
    }
}

private val qualityPresets = intArrayOf(50, 55, 60)
private val compressionPresets = listOf("fast" to "Veloce", "medium" to "Medio", "slow" to "Lento")
private val compressionTunes = arrayOf("standard", "ssim", "psnr")
