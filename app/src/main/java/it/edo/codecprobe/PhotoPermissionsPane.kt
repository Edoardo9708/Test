package it.edo.codecprobe

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/** Each directory is a lazy Compose item; access checks belong to the Java coordinator. */
internal fun LazyListScope.photoPermissionItems(state: PhotoUiState, host: PhotoWorkspaceHost) {
    val permissions = state.permissions
    if (permissions.message.isNotEmpty()) {
        item("permissions-message") {
            AppPanel(modifier = Modifier.semantics { testTag = "photo-permissions" }) {
                SelectionContainer { Text(permissions.message, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
    if (permissions.rows.isNotEmpty()) {
        item("permissions-group") {
            Column(
                Modifier.fillMaxWidth().semantics { testTag = "photo-permission-group" },
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                permissions.rows.forEachIndexed { index, row ->
                    PhotoPermissionCard(state, host, row, index, permissions.rows.size)
                }
            }
        }
    }
    if (permissions.message.isEmpty()) {
        item("permissions-summary") {
            Column(Modifier.semantics { testTag = "photo-permissions" }) {
                if (permissions.total > permissions.rows.size) {
                    Text("… Altre ${permissions.total - permissions.rows.size} cartelle. Usa la destinazione per tutte le foto per gestirle insieme.",
                        style = MaterialTheme.typography.bodySmall)
                }
                Text("Cartelle autorizzate: ${permissions.authorized} / ${permissions.total}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PhotoPermissionCard(
    state: PhotoUiState,
    host: PhotoWorkspaceHost,
    row: OriginDirectoryAccess.Row,
    index: Int,
    count: Int
) {
    val fullPath = row.label.replace("primary:", "Memoria interna / ")
    var pathDialogOpen by remember(row.key) { mutableStateOf(false) }
    AppPanel(
        modifier = Modifier.semantics { testTag = "photo-permission:${row.key}" },
        shape = appGroupShape(index, count),
        contentSpacing = 4.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        borderColor = if (row.allowed) MaterialTheme.colorScheme.outline.copy(alpha = .48f)
            else MaterialTheme.colorScheme.error
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                compactFolderPath(fullPath),
                Modifier.weight(1f).clickable { pathDialogOpen = true }
                    .semantics { contentDescription = "Mostra percorso completo: $fullPath" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(if (row.allowed) "Autorizzata" else "Da autorizzare",
                style = MaterialTheme.typography.labelSmall,
                color = if (row.allowed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Foto: ${row.numbers}" + if (row.manual) " · manuale" else "",
                Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppButton(if (row.choose) "Scegli" else if (row.allowed) "Cambia" else "Autorizza",
                { if (row.allowed) host.changeFolder(row) else host.authorize(row) }, Modifier.width(112.dp).height(48.dp),
                enabled = !state.busy && !state.permissions.checking,
                compact = true, tag = "photo-authorize:${row.key}")
        }
    }
    if (pathDialogOpen) FolderPathDialog(fullPath, onClose = { pathDialogOpen = false })
}

@Composable
private fun FolderPathDialog(path: String, onClose: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    AppDialogSurface(onDismissRequest = onClose) {
                Text("Percorso completo", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    SelectionContainer {
                        Text(path, Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppButton("Copia", {
                        clipboard.setText(AnnotatedString(path))
                        onClose()
                    }, Modifier.weight(1f), compact = true, tag = "photo-path-copy")
                    AppButton("Chiudi", onClose, Modifier.weight(1f), compact = true, tag = "photo-path-close")
                }
    }
}

private fun compactFolderPath(path: String, maxLength: Int = 38): String {
    if (path.length <= maxLength) return path
    val visibleLength = maxLength - 1
    val leading = (visibleLength + 1) / 2
    val trailing = visibleLength - leading
    return path.take(leading) + "…" + path.takeLast(trailing)
}
