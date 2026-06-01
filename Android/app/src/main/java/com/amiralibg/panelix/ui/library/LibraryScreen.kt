package com.amiralibg.panelix.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amiralibg.panelix.data.AccentColor
import com.amiralibg.panelix.data.AppPreferences
import com.amiralibg.panelix.data.ComicEntity
import com.amiralibg.panelix.data.ComicFormat
import com.amiralibg.panelix.data.LibraryViewMode
import com.amiralibg.panelix.data.SortOption
import com.amiralibg.panelix.ui.components.ComicCard
import com.amiralibg.panelix.ui.components.CoverArt
import com.amiralibg.panelix.ui.components.SafFolderPicker
import com.amiralibg.panelix.ui.theme.LocalPanelixPalette
import com.amiralibg.panelix.ui.theme.PanelixDisplayFont
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
    val palette = LocalPanelixPalette.current
    val snackbar = remember { SnackbarHostState() }
    var showSort by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    LaunchedEffect(state.error) {
        if (state.error != null) snackbar.showSnackbar(state.error)
    }
    val picker = rememberLauncherForActivityResult(SafFolderPicker()) { result ->
        if (result != null) onAddFolder(result.uri, result.flags)
    }
    Scaffold(
        containerColor = palette.bg,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val showCover = state.preferences.showProgressOnCovers
        val showContinue = state.continueReading.isNotEmpty() && state.query.isBlank() && state.filter == null
        val cols = when (state.preferences.libraryViewMode) {
            LibraryViewMode.list -> 1
            LibraryViewMode.compact -> 3
            else -> 2
        }
        val isListMode = cols == 1
        val hGap = if (cols == 3) 14.dp else 18.dp
        val vGap = if (cols == 3) 18.dp else if (isListMode) 0.dp else 22.dp
        LazyVerticalGrid(
            columns = GridCells.Fixed(cols),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(hGap),
            verticalArrangement = Arrangement.spacedBy(vGap),
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg)
                .padding(padding),
        ) {
            item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                LibraryHeader(
                    count = state.comics.size,
                    inProgress = state.inProgressCount,
                    onAdd = { picker.launch(Unit) },
                    onSettings = onSettings,
                    onRescan = onRescan,
                )
            }
            item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    SearchPill(query = state.query, onQuery = onQuery)
                    if (state.isScanning) {
                        Spacer(Modifier.height(12.dp))
                        ScanProgressPanel(state.scanProgress)
                    }
                    if (showContinue) {
                        Spacer(Modifier.height(22.dp))
                        ContinueRail(
                            books = state.continueReading,
                            state = state,
                            onClick = onOpenComic,
                        )
                    }
                    Spacer(Modifier.height(if (showContinue) 26.dp else 18.dp))
                    ControlsRow(
                        density = state.preferences.libraryViewMode,
                        sort = state.preferences.sortOption,
                        filter = state.filter,
                        onViewMode = onViewMode,
                        onOpenSort = { showSort = true },
                        onOpenFilter = { showFilter = true },
                    )
                    Spacer(Modifier.height(14.dp))
                }
            }
            if (state.visibleComics.isEmpty() && !state.isScanning) {
                item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                    EmptyState(onAdd = { picker.launch(Unit) })
                }
            } else {
                items(state.visibleComics, key = { it.id }) { comic ->
                    ComicCard(
                        comic = comic,
                        compact = isListMode,
                        progress = state.progressFor(comic),
                        showProgressBar = showCover,
                        onClick = { onOpenComic(comic.id) },
                    )
                }
            }
        }
    }
    if (showSort) {
        ChoiceDialog(
            title = "Sort by",
            options = listOf(
                SortOption.recentlyOpened to "Recent",
                SortOption.recentlyAdded to "Added",
                SortOption.title to "A → Z",
            ),
            selected = state.preferences.sortOption,
            onPick = {
                onSort(it)
                showSort = false
            },
            onDismiss = { showSort = false },
        )
    }
    if (showFilter) {
        ChoiceDialog(
            title = "Format",
            options = listOf<Pair<ComicFormat?, String>>(
                null to "All",
                ComicFormat.pdf to "PDF",
                ComicFormat.cbz to "CBZ",
                ComicFormat.cbr to "CBR",
                ComicFormat.folder to "Folder",
            ),
            selected = state.filter,
            onPick = {
                onFilter(it)
                showFilter = false
            },
            onDismiss = { showFilter = false },
        )
    }
}

@Composable
private fun LibraryHeader(
    count: Int,
    inProgress: Int,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
    onRescan: () -> Unit,
) {
    val palette = LocalPanelixPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Text(
                "Panelix",
                style = TextStyle(
                    fontFamily = PanelixDisplayFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 27.sp,
                    letterSpacing = (-0.5).sp,
                    color = palette.text,
                    lineHeight = 28.sp,
                ),
            )
            Spacer(Modifier.height(5.dp))
            val sub = buildString {
                append("$count comics")
                if (inProgress > 0) append(" · $inProgress in progress")
            }
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconChip(onAdd) { Icon(Icons.Outlined.Add, "Add folder", tint = palette.text, modifier = Modifier.size(20.dp)) }
            IconChip(onRescan) { Icon(Icons.Outlined.Refresh, "Rescan", tint = palette.text, modifier = Modifier.size(20.dp)) }
            IconChip(onSettings) { Icon(Icons.Outlined.Settings, "Settings", tint = palette.text, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun IconChip(onClick: () -> Unit, content: @Composable () -> Unit) {
    val palette = LocalPanelixPalette.current
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surface)
            .border(1.dp, palette.line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun SearchPill(query: String, onQuery: (String) -> Unit) {
    val palette = LocalPanelixPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surface)
            .border(1.dp, palette.line, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Search, null, tint = palette.muted, modifier = Modifier.size(19.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    "Search your library",
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.muted,
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.text),
                cursorBrush = SolidColor(palette.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ContinueRail(
    books: List<ComicEntity>,
    state: LibraryUiState,
    onClick: (String) -> Unit,
) {
    val palette = LocalPanelixPalette.current
    Column {
        Text(
            "Continue reading",
            style = MaterialTheme.typography.titleMedium,
            color = palette.text,
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(books, key = { it.id }) { book ->
                ContinueCard(
                    book = book,
                    progress = state.progressFor(book),
                    page = state.currentPage(book),
                    showProgressBar = state.preferences.showProgressOnCovers,
                    onClick = { onClick(book.id) },
                )
            }
        }
    }
}

@Composable
private fun ContinueCard(
    book: ComicEntity,
    progress: Float,
    page: Int,
    showProgressBar: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalPanelixPalette.current
    Column(
        Modifier
            .width(124.dp)
            .clickable(onClick = onClick),
    ) {
        CoverArt(
            comic = book,
            progress = progress,
            showProgressBar = false,
            modifier = Modifier.size(width = 124.dp, height = 176.dp),
            radius = 14.dp,
        )
        if (showProgressBar) {
            Spacer(Modifier.height(9.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.surfaceHi),
            ) {
                Box(
                    Modifier
                        .fillMaxSize(progress.coerceIn(0f, 1f))
                        .background(palette.accent),
                )
            }
        } else {
            Spacer(Modifier.height(9.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            book.title,
            style = MaterialTheme.typography.titleSmall,
            color = palette.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        val pages = book.pageCount ?: 0
        Text(
            if (pages > 0) "Page $page of $pages" else "Page $page",
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ControlsRow(
    density: LibraryViewMode,
    sort: SortOption,
    filter: ComicFormat?,
    onViewMode: (LibraryViewMode) -> Unit,
    onOpenSort: () -> Unit,
    onOpenFilter: () -> Unit,
) {
    val palette = LocalPanelixPalette.current
    Row(
        Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "Library",
            style = MaterialTheme.typography.titleLarge,
            color = palette.text,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortPill(label = sortLabel(sort), onClick = onOpenSort)
            FilterButton(active = filter != null, onClick = onOpenFilter)
            DensityToggle(density = density, onChange = onViewMode)
        }
    }
}

@Composable
private fun SortPill(label: String, onClick: () -> Unit) {
    val palette = LocalPanelixPalette.current
    Row(
        Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(palette.surface)
            .border(1.dp, palette.line, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Outlined.Schedule, null, tint = palette.sub, modifier = Modifier.size(15.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = palette.text, fontWeight = FontWeight.SemiBold)
        Icon(Icons.Outlined.ExpandMore, null, tint = palette.muted, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun FilterButton(active: Boolean, onClick: () -> Unit) {
    val palette = LocalPanelixPalette.current
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) palette.accent.copy(alpha = 0.16f) else palette.surface)
            .border(1.dp, if (active) palette.accent else palette.line, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.FilterAlt,
            "Filter",
            tint = if (active) palette.accent else palette.sub,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun DensityToggle(density: LibraryViewMode, onChange: (LibraryViewMode) -> Unit) {
    val palette = LocalPanelixPalette.current
    Row(
        Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(palette.surface)
            .border(1.dp, palette.line, RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DensityCell(LibraryViewMode.grid, density, onChange) {
            Icon(Icons.Outlined.GridView, "Grid", tint = it, modifier = Modifier.size(16.dp))
        }
        DensityCell(LibraryViewMode.list, density, onChange) {
            Icon(Icons.AutoMirrored.Outlined.ViewList, "List", tint = it, modifier = Modifier.size(16.dp))
        }
        DensityCell(LibraryViewMode.compact, density, onChange) {
            Icon(Icons.Outlined.AutoAwesomeMotion, "Compact", tint = it, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun DensityCell(
    target: LibraryViewMode,
    current: LibraryViewMode,
    onChange: (LibraryViewMode) -> Unit,
    icon: @Composable (androidx.compose.ui.graphics.Color) -> Unit,
) {
    val palette = LocalPanelixPalette.current
    val selected = target == current
    Box(
        Modifier
            .size(width = 32.dp, height = 28.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) palette.surfaceHi else androidx.compose.ui.graphics.Color.Transparent)
            .clickable { onChange(target) },
        contentAlignment = Alignment.Center,
    ) {
        icon(if (selected) palette.text else palette.muted)
    }
}

@Composable
private fun ScanProgressPanel(progress: ScanUiProgress?) {
    val palette = LocalPanelixPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surface)
            .border(1.dp, palette.line, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (progress == null || progress.total <= 0) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = palette.accent)
            Text("Counting comics...", style = MaterialTheme.typography.bodySmall, color = palette.muted)
        } else {
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier.fillMaxWidth(),
                color = palette.accent,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Scanning ${progress.completed}/${progress.total}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                )
                Text(
                    progress.estimatedRemainingMillis?.let { "~${formatEta(it)} left" } ?: "Finishing...",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                )
            }
            progress.currentTitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = palette.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    val palette = LocalPanelixPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(palette.surface)
                .border(1.dp, palette.line, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.AutoAwesomeMotion, null, tint = palette.accent)
        }
        Spacer(Modifier.height(18.dp))
        Text("No comics yet", style = MaterialTheme.typography.titleLarge, color = palette.text)
        Spacer(Modifier.height(6.dp))
        Text(
            "Add a folder with PDF, CBZ, CBR, or image folders to start your library.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.muted,
        )
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(palette.accent)
                .clickable(onClick = onAdd)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Outlined.Add, null, tint = palette.onAccent, modifier = Modifier.size(18.dp))
            Text("Add folder", color = palette.onAccent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPanelixPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = palette.text) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(value) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(label, color = palette.text, style = MaterialTheme.typography.bodyLarge)
                        if (value == selected) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(palette.accent),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = palette.accent)
            }
        },
    )
}

private fun sortLabel(option: SortOption) = when (option) {
    SortOption.recentlyOpened -> "Recent"
    SortOption.recentlyAdded -> "Added"
    SortOption.title -> "A → Z"
}

private fun formatEta(milliseconds: Long): String {
    val seconds = (milliseconds / 1000).coerceAtLeast(1)
    return if (seconds < 60) "${seconds}s" else "${seconds / 60}m ${seconds % 60}s"
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0E10)
@Composable
private fun LibraryScreenPreview() {
    PanelixTheme(darkTheme = true, accent = AccentColor.coral) {
        LibraryScreen(
            state = LibraryUiState(
                comics = previewComics(),
                continueReading = previewComics().take(3),
                preferences = AppPreferences(
                    hasCompletedOnboarding = true,
                    libraryViewMode = LibraryViewMode.grid,
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
    previewComic("hush", "Batman: Hush", ComicFormat.cbr, 291),
    previewComic("damned", "Batman: Damned 2", ComicFormat.cbr, 52),
    previewComic("deadpool", "Deadpool", ComicFormat.cbz, 96),
    previewComic("joke", "The Killing Joke", ComicFormat.cbr, 64),
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
