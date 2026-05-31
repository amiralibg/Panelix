package com.amiralibg.panelix.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TopAppBarDefaults
import com.amiralibg.panelix.data.AppPreferences
import com.amiralibg.panelix.data.ComicEntity
import com.amiralibg.panelix.data.ComicFormat
import com.amiralibg.panelix.data.LibraryViewMode
import com.amiralibg.panelix.data.SortOption
import com.amiralibg.panelix.ui.components.ComicCard
import com.amiralibg.panelix.ui.components.SafFolderPicker
import com.amiralibg.panelix.ui.theme.PanelixTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onAddFolder: (Uri, Int) -> Unit,
    onRescan: () -> Unit,
    onQuery: (String) -> Unit,
    onSort: (SortOption) -> Unit,
    onViewMode: (LibraryViewMode) -> Unit,
    onFilter: (ComicFormat?) -> Unit,
    onOpenComic: (String) -> Unit,
    onSettings: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        if (state.error != null) snackbar.showSnackbar(state.error)
    }
    val picker = rememberLauncherForActivityResult(SafFolderPicker()) { result ->
        if (result != null) onAddFolder(result.uri, result.flags)
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Panelix", fontWeight = FontWeight.Bold)
                        Text("${state.comics.size} comics", style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = {
                    IconButton(onClick = { picker.launch(Unit) }) { Icon(Icons.Outlined.Add, "Add folder") }
                    IconButton(onClick = onRescan) { Icon(Icons.Outlined.Refresh, "Rescan") }
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Settings") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            LibraryControls(state, onQuery, onSort, onViewMode, onFilter)
            if (state.isScanning) {
                if (state.visibleComics.isEmpty()) {
                    FocusedScanProgress(state.scanProgress)
                } else {
                    ScanProgressPanel(state.scanProgress)
                }
            }
            if (state.visibleComics.isEmpty() && !state.isScanning) {
                Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
                    Text("No comics found", style = MaterialTheme.typography.headlineSmall)
                    Text("Add a folder or rescan after placing PDF, CBZ, CBR, or image folders in your library.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (state.preferences.libraryViewMode == LibraryViewMode.grid) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.continueReading.isNotEmpty()) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            SectionTitle("Continue Reading")
                        }
                        items(state.continueReading, key = { "continue-${it.id}" }) {
                            ComicCard(it, onClick = { onOpenComic(it.id) })
                        }
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            SectionTitle("Library")
                        }
                    }
                    items(state.visibleComics, key = { it.id }) {
                        ComicCard(it, onClick = { onOpenComic(it.id) })
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.continueReading.isNotEmpty()) {
                        item { SectionTitle("Continue Reading") }
                        items(state.continueReading, key = { "continue-${it.id}" }) {
                            ComicCard(it, compact = true, onClick = { onOpenComic(it.id) })
                        }
                        item { SectionTitle("Library") }
                    }
                    items(state.visibleComics, key = { it.id }) {
                        ComicCard(it, compact = true, onClick = { onOpenComic(it.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusedScanProgress(progress: ScanUiProgress?) {
    Box(
        Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.secondary
            )
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Scanning library", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Finding comics, counting pages, and preparing covers. You can leave this open while large archives are indexed.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ScanProgressPanel(progress)
            }
        }
    }
}

@Composable
private fun ScanProgressPanel(progress: ScanUiProgress?) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (progress == null || progress.total <= 0) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Counting comics...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Scanning ${progress.completed}/${progress.total}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        progress.estimatedRemainingMillis?.let { "~${formatEta(it)} left" } ?: "Finishing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                progress.currentTitle?.let {
                    Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun formatEta(milliseconds: Long): String {
    val seconds = (milliseconds / 1000).coerceAtLeast(1)
    return if (seconds < 60) "${seconds}s" else "${seconds / 60}m ${seconds % 60}s"
}

@Composable
private fun LibraryControls(
    state: LibraryUiState,
    onQuery: (String) -> Unit,
    onSort: (SortOption) -> Unit,
    onViewMode: (LibraryViewMode) -> Unit,
    onFilter: (ComicFormat?) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            placeholder = { Text("Search by title") },
            shape = RoundedCornerShape(8.dp),
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SortChip("Recently Added", SortOption.recentlyAdded, state.preferences.sortOption, onSort)
            SortChip("Recently Opened", SortOption.recentlyOpened, state.preferences.sortOption, onSort)
            SortChip("A-Z", SortOption.title, state.preferences.sortOption, onSort)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onViewMode(LibraryViewMode.grid) }) { Icon(Icons.Outlined.GridView, "Grid") }
            IconButton(onClick = { onViewMode(LibraryViewMode.list) }) { Icon(Icons.AutoMirrored.Outlined.ViewList, "List") }
            listOf(null to "All", ComicFormat.pdf to "PDF", ComicFormat.cbz to "CBZ", ComicFormat.cbr to "CBR", ComicFormat.folder to "Folder").forEach { (format, label) ->
                FilterChip(selected = state.filter == format, onClick = { onFilter(format) }, label = { Text(label, maxLines = 1) })
            }
        }
    }
}

@Composable
private fun SortChip(label: String, value: SortOption, selected: SortOption, onSort: (SortOption) -> Unit) {
    AssistChip(onClick = { onSort(value) }, label = {
        Text(if (value == selected) "• $label" else label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    })
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, Modifier.padding(top = 10.dp, bottom = 4.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}


@Preview(showBackground = true, backgroundColor = 0xFFF7F9F8)
@Composable
private fun LibraryScreenGridPreview() {
    PanelixTheme {
        LibraryScreen(
            state = LibraryUiState(
                comics = previewComics(),
                continueReading = previewComics().take(2),
                preferences = AppPreferences(
                    hasCompletedOnboarding = true,
                    libraryViewMode = LibraryViewMode.grid,
                    sortOption = SortOption.recentlyAdded,
                ),
            ),
            onAddFolder = { _, _ -> },
            onRescan = {},
            onQuery = {},
            onSort = {},
            onViewMode = {},
            onFilter = {},
            onOpenComic = {},
            onSettings = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F9F8)
@Composable
private fun LibraryScreenScanningPreview() {
    PanelixTheme {
        LibraryScreen(
            state = LibraryUiState(
                isScanning = true,
                scanProgress = ScanUiProgress(
                    completed = 18,
                    total = 42,
                    currentTitle = "The Clockwork Harbor #7.cbz",
                    estimatedRemainingMillis = 95_000,
                ),
            ),
            onAddFolder = { _, _ -> },
            onRescan = {},
            onQuery = {},
            onSort = {},
            onViewMode = {},
            onFilter = {},
            onOpenComic = {},
            onSettings = {},
        )
    }
}

private fun previewComics() = listOf(
    previewComic("clockwork", "The Clockwork Harbor", ComicFormat.cbz, 124),
    previewComic("paper-moon", "Paper Moon Detective", ComicFormat.pdf, 88),
    previewComic("signal", "Signal Ghosts", ComicFormat.cbr, 156, "Cover pending"),
    previewComic("garden", "The Glass Garden", ComicFormat.folder, 64),
)

private fun previewComic(
    id: String,
    title: String,
    format: ComicFormat,
    pageCount: Int,
    parserMessage: String? = null,
) = ComicEntity(
    id = id,
    uri = "content://panelix/$id",
    folderUri = "content://panelix/library",
    title = title,
    format = format,
    coverUri = null,
    pageCount = pageCount,
    fileSize = null,
    addedAt = id.length.toLong(),
    updatedAt = id.length.toLong(),
    lastOpenedAt = id.length.toLong(),
    isAvailable = true,
    parserMessage = parserMessage,
    sourceModifiedAt = null,
)
