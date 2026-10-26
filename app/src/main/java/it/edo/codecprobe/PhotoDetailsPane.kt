package it.edo.codecprobe

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow

/** Basic, labelled fields stay in their own card, with a stable loading layout. */
@Composable
internal fun PhotoDetailsPane(state: PhotoUiState) {
    Surface(shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().semantics { testTag = "photo-details-panel" }) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (state.selectedNames.isEmpty()) "Dettagli della foto" else "File selezionati · ${state.selectedNames.size}",
                style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
            if (state.selectedNames.isNotEmpty()) {
                SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        state.selectedNames.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            } else key(state.currentUri) {
                Box(Modifier.fillMaxWidth()) {
                    val content = Modifier.fillMaxWidth().then(
                        if (state.previewLoading) Modifier.clearAndSetSemantics {} else Modifier)
                    SelectionContainer {
                        Column(content, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            state.detailRows.forEach { (label, value) ->
                                Row(Modifier.fillMaxWidth().heightIn(min = 22.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(if (state.previewLoading) "••••••••" else value,
                                        Modifier.weight(1.35f), maxLines = 2, overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    if (state.previewLoading) Text("Lettura dettagli…", Modifier.align(Alignment.Center)
                        .semantics { testTag = "photo-details-loading"; liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                if (state.detailError.isNotBlank() && !state.previewLoading) SelectionContainer {
                    Text(state.detailError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                var all by rememberSaveable { mutableStateOf(false) }
                Column(Modifier.fillMaxWidth()) {
                    TextButton(onClick = { all = !all }, enabled = !state.reading,
                        modifier = Modifier.fillMaxWidth().semantics { testTag = "photo-details-all" }) {
                        Text(if (state.reading) "Analisi completa in corso…" else if (all) "Mostra meno" else "Mostra tutto")
                    }
                    PhotoPopupFold(all && !state.reading) {
                        Box(Modifier.padding(top = 12.dp)) {
                            SelectionContainer { Text(state.allDetails, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }
    }
}
