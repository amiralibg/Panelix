package com.amiralibg.panelix.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amiralibg.panelix.ui.theme.LocalPanelixPalette

/**
 * Shown while an update is available, downloading, or ready to install.
 * Returns without rendering for any other status.
 */
@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onPrimary: () -> Unit,
    onDismiss: () -> Unit,
) {
    val info = state.info ?: return
    if (state.status != UpdateStatus.Available &&
        state.status != UpdateStatus.Downloading &&
        state.status != UpdateStatus.ReadyToInstall
    ) return

    val palette = LocalPanelixPalette.current
    val downloading = state.status == UpdateStatus.Downloading

    AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        containerColor = palette.surface,
        title = { Text("Update available", color = palette.text) },
        text = {
            Column {
                Text(
                    "Version ${info.versionName}",
                    color = palette.accent,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                )
                if (info.releaseNotes.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        info.releaseNotes.take(500),
                        color = palette.muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (downloading) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { state.downloadProgress },
                        color = palette.accent,
                        trackColor = palette.surfaceHi,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${(state.downloadProgress * 100).toInt()}%",
                        color = palette.muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            if (!downloading) {
                val label = if (state.status == UpdateStatus.ReadyToInstall) "Install" else "Update"
                TextButton(onClick = onPrimary) {
                    Text(label, color = palette.accent, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!downloading) {
                TextButton(onClick = onDismiss) { Text("Later", color = palette.muted) }
            }
        },
    )
}
