package it.edo.codecprobe

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp

/** Each directory is a lazy Compose item; access checks belong to the Java coordinator. */
internal fun LazyListScope.photoPermissionItems(state: PhotoUiState, host: PhotoWorkspaceHost) {
    val permissions = state.permissions
    if (permissions.message.isNotEmpty()) {
        item("permissions-message") {
            PhotoPanel(modifier = Modifier.semantics { testTag = "photo-permissions" }, outlined = false) {
                SelectionContainer { Text(permissions.message, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
    itemsIndexed(permissions.rows, key = { _, row -> "permission:${row.key}" }) { index, row ->
        val shape = if (permissions.rows.size == 1) RoundedCornerShape(24.dp)
            else photoJoined(if (index == 0) 0 else if (index == permissions.rows.lastIndex) 2 else 1)
        PhotoPanel(modifier = Modifier.semantics { testTag = "photo-permission:${row.key}" }, shape = shape, outlined = false) {
            Text(if (row.allowed) "✓ Accesso verificato" else "! Da autorizzare",
                style = MaterialTheme.typography.labelLarge,
                color = if (row.allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            SelectionContainer { Text(row.label, style = MaterialTheme.typography.bodyMedium) }
            Text("Foto: ${row.numbers}" + if (row.manual) " · origine associata manualmente" else "",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PhotoButton(if (row.choose) "Scegli" else if (row.allowed) "Gestisci" else "Autorizza",
                { host.authorize(row) }, Modifier.fillMaxWidth(), enabled = !state.busy && !permissions.checking,
                tag = "photo-authorize:${row.key}")
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
