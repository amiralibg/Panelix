package com.amiralibg.panelix.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.amiralibg.panelix.data.AppPreferences
import com.amiralibg.panelix.data.FolderEntity
import com.amiralibg.panelix.data.ReaderLayoutMode
import com.amiralibg.panelix.data.ReadingDirection
import com.amiralibg.panelix.data.ThemePreference
import com.amiralibg.panelix.ui.components.SafFolderPicker
import com.amiralibg.panelix.ui.theme.PanelixTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onAddFolder: (Uri, Int) -> Unit,
    onRemoveFolder: (String) -> Unit,
    onRescan: () -> Unit,
    onTheme: (ThemePreference) -> Unit,
    onReaderMode: (ReaderLayoutMode) -> Unit,
    onDirection: (ReadingDirection) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<FolderEntity?>(null) }
    val picker = rememberLauncherForActivityResult(SafFolderPicker()) { result ->
        if (result != null) onAddFolder(result.uri, result.flags)
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { picker.launch(Unit) }) { Icon(Icons.Outlined.Add, "Add folder") }
                    IconButton(onClick = onRescan) { Icon(Icons.Outlined.Refresh, "Rescan") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                SettingsGroup("Theme") {
                    ThemePreference.entries.forEach { value ->
                        AssistChip(onClick = { onTheme(value) }, label = { Text(label(value, state.preferences.themePreference == value)) })
                    }
                }
            }
            item {
                SettingsGroup("Default reader mode") {
                    ReaderLayoutMode.entries.forEach { value ->
                        AssistChip(onClick = { onReaderMode(value) }, label = { Text(label(value, state.preferences.readerLayoutMode == value)) })
                    }
                }
            }
            item {
                SettingsGroup("Default reading direction") {
                    ReadingDirection.entries.forEach { value ->
                        AssistChip(onClick = { onDirection(value) }, label = { Text(label(value, state.preferences.readingDirection == value)) })
                    }
                }
            }
            item {
                Text("Folders", style = MaterialTheme.typography.titleMedium)
                Text("Android folder permissions are granted per folder through the Storage Access Framework. If permission is revoked, add the folder again.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(state.folders, key = { it.uri }) { folder ->
                Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 1.dp) {
                    ListItem(
                        headlineContent = { Text(folder.name) },
                        supportingContent = { Text(folder.uri, maxLines = 1) },
                        trailingContent = {
                            IconButton(onClick = { pendingDelete = folder }) { Icon(Icons.Outlined.Delete, "Remove") }
                        },
                        modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { pendingDelete = folder }),
                    )
                }
            }
        }
    }
    pendingDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove folder?") },
            text = { Text(folder.name) },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveFolder(folder.uri)
                    pendingDelete = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
        }
    }
}

private fun label(value: Enum<*>, selected: Boolean): String {
    val text = value.name.replaceFirstChar { it.uppercase() }
    return if (selected) "• $text" else text
}


@Preview(showBackground = true, backgroundColor = 0xFFF7F9F8)
@Composable
private fun SettingsScreenPreview() {
    PanelixTheme {
        SettingsScreen(
            state = SettingsUiState(
                preferences = AppPreferences(
                    themePreference = ThemePreference.light,
                    readerLayoutMode = ReaderLayoutMode.horizontal,
                    readingDirection = ReadingDirection.ltr,
                ),
                folders = listOf(
                    FolderEntity(id = 1, uri = "content://panelix/comics", name = "Comics"),
                    FolderEntity(id = 2, uri = "content://panelix/manga", name = "Manga"),
                ),
            ),
            onBack = {},
            onAddFolder = { _, _ -> },
            onRemoveFolder = {},
            onRescan = {},
            onTheme = {},
            onReaderMode = {},
            onDirection = {},
        )
    }
}
