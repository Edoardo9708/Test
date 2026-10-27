package it.edo.codecprobe

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Metadata is visually grouped in its own compact surface within the selected-files panel. */
@Composable
internal fun PhotoDetailsPane(state: PhotoUiState) {
    Column(Modifier.fillMaxWidth().semantics { testTag = "photo-details-panel" }) {
        Surface(
            modifier = Modifier.fillMaxWidth().semantics { testTag = "photo-details-card" },
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .22f))
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (state.selectedNames.isEmpty()) "Dettagli della foto" else "Dettagli della selezione",
                    style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })

                if (state.selectedNames.isNotEmpty()) {
                    SelectionContainer {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            state.selectedNames.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                } else {
                    AnimatedContent(
                        targetState = state.metadata,
                        transitionSpec = {
                            val enter = fadeIn(tween(200, easing = FastOutSlowInEasing))
                            val exit = if (targetState.loading) ExitTransition.None
                                else fadeOut(tween(180, easing = FastOutSlowInEasing))
                            (enter togetherWith exit).using(
                                SizeTransform(clip = false) { _, _ ->
                                    tween(220, easing = FastOutSlowInEasing)
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth().semantics { testTag = "photo-metadata-container" },
                        contentAlignment = Alignment.TopStart,
                        label = "photo-metadata-crossfade"
                    ) { metadata ->
                        Column(Modifier.fillMaxWidth().semantics {
                            testTag = "photo-metadata-content-${metadata.uri.hashCode()}"
                        }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (metadata.loading) PhotoDetailsSkeleton(metadata.rows)
                            else PhotoDetailRows(metadata.rows)
                            Box(Modifier.fillMaxWidth().heightIn(min = 32.dp), contentAlignment = Alignment.CenterStart) {
                                when {
                                    metadata.loading -> Text("Lettura dettagli…", Modifier.fillMaxWidth()
                                        .semantics { testTag = "photo-details-loading"; liveRegion = LiveRegionMode.Polite },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary)
                                    metadata.error.isNotBlank() -> Text(metadata.error, Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    metadata.analyzing -> Text("Analisi della foto…", Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    else -> Spacer(Modifier.height(32.dp))
                                }
                            }
                        }
                    }
                    var all by rememberSaveable { mutableStateOf(false) }
                    TextButton(onClick = { all = !all }, enabled = !state.reading,
                        modifier = Modifier.align(Alignment.End).semantics { testTag = "photo-details-all" }) {
                        Text(if (state.reading) "Analisi completa in corso…" else if (all) "Mostra meno" else "Mostra tutto")
                    }
                    AppPopupTransition(all && !state.reading) {
                        SelectionContainer {
                            Text(state.allDetails, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoDetailRows(rows: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    SelectionContainer(modifier) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth().heightIn(min = 28.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(label, Modifier.weight(.9f), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(value, Modifier.weight(1.1f), style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun PhotoDetailsSkeleton(rows: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f)
    Column(modifier.fillMaxWidth().semantics { testTag = "photo-details-skeleton" },
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEachIndexed { index, (label, _) ->
            Row(Modifier.fillMaxWidth().heightIn(min = 28.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label, Modifier.weight(.9f), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.weight(1.1f), contentAlignment = Alignment.CenterEnd) {
                    Box(Modifier.width(listOf(72, 54, 82, 64, 94)[index % 5].dp).height(8.dp)
                        .clip(RoundedCornerShape(50)).blur(2.dp).background(color))
                }
            }
        }
    }
}
