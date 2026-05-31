package com.amiralibg.panelix.ui.reader

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.amiralibg.panelix.data.BookmarkEntity
import com.amiralibg.panelix.data.ComicEntity
import com.amiralibg.panelix.data.ComicFormat
import com.amiralibg.panelix.data.ReaderLayoutMode
import com.amiralibg.panelix.data.ReadingDirection
import com.amiralibg.panelix.parser.ReaderPage
import com.amiralibg.panelix.ui.theme.PanelixTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    state: ReaderUiState,
    onBack: () -> Unit,
    onPage: (Int) -> Unit,
    onBookmark: () -> Unit,
    onPrefs: (ReaderLayoutMode, ReadingDirection, Float, Float) -> Unit,
) {
    HideSystemBars()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var controls by remember { mutableStateOf(true) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showTune by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(controls) {
        if (controls) {
            delay(3500)
            controls = false
        }
    }
    LaunchedEffect(state.currentPage, state.pageCount) {
        if (state.pageCount > 0 && state.currentPage == state.pageCount - 1) {
            Toast.makeText(context, "Last page reached", Toast.LENGTH_SHORT).show()
        }
    }
    Box(
        Modifier.fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyUp && (it.key == Key.VolumeDown || it.key == Key.DirectionRight)) {
                    onPage(state.currentPage + if (state.direction == ReadingDirection.ltr) 1 else -1)
                    true
                } else if (it.type == KeyEventType.KeyUp && (it.key == Key.VolumeUp || it.key == Key.DirectionLeft)) {
                    onPage(state.currentPage + if (state.direction == ReadingDirection.ltr) -1 else 1)
                    true
                } else false
            }
            .pointerInput(Unit) { detectTapGestures { controls = !controls } },
    ) {
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            state.parserMessage != null && state.pages.isEmpty() -> Text(state.parserMessage, Modifier.align(Alignment.Center).padding(24.dp), color = Color.White)
            state.pages.isEmpty() -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Preparing first page...", color = Color.White)
            }
            state.readerMode == ReaderLayoutMode.vertical -> VerticalReader(state, onPage)
            else -> PagedReader(state, onPage)
        }
        if (!controls) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { controls = true })
                    }
            )
        }
        if (controls) {
            ReaderOverlay(
                state = state,
                onBack = onBack,
                onBookmark = {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onBookmark()
                },
                onBookmarks = { showBookmarks = true },
                onTune = { showTune = true },
                onPrevious = { onPage(state.currentPage - 1) },
                onNext = { onPage(state.currentPage + 1) },
                onJump = onPage,
            )
        }
    }
    if (showBookmarks) {
        ModalBottomSheet(onDismissRequest = { showBookmarks = false }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Bookmarks", style = MaterialTheme.typography.titleLarge)
                if (state.bookmarks.isEmpty()) Text("No bookmarks yet")
                state.bookmarks.forEach {
                    AssistChip(onClick = {
                        onPage(it.page)
                        showBookmarks = false
                    }, label = { Text("Page ${it.page + 1}") })
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
    if (showTune) {
        ModalBottomSheet(onDismissRequest = { showTune = false }) {
            TuneSheet(state, onPrefs)
        }
    }
}

@Composable
private fun HideSystemBars() {
    if (LocalInspectionMode.current) return
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VerticalReader(state: ReaderUiState, onPage: (Int) -> Unit) {
    val pagerState = rememberPagerState(initialPage = state.currentPage) { state.pageCount.coerceAtLeast(1) }
    LaunchedEffect(state.currentPage) {
        if (state.currentPage != pagerState.currentPage) pagerState.animateScrollToPage(state.currentPage)
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { onPage(it) }
    }
    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        pageSpacing = 8.dp,
        contentPadding = PaddingValues(vertical = 8.dp),
        beyondViewportPageCount = 1,
    ) { page ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            state.pages.getOrNull(page)?.let { PageImage(it, state, Modifier.fillMaxSize()) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PagedReader(state: ReaderUiState, onPage: (Int) -> Unit) {
    val pagerState = rememberPagerState(initialPage = state.currentPage) { state.pageCount.coerceAtLeast(1) }
    LaunchedEffect(state.currentPage) {
        if (state.currentPage != pagerState.currentPage) pagerState.animateScrollToPage(state.currentPage)
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { onPage(it) }
    }
    HorizontalPager(
        state = pagerState,
        reverseLayout = state.direction == ReadingDirection.rtl,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
    ) { page ->
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (state.readerMode == ReaderLayoutMode.spread && maxWidth > 720.dp && page % 2 == 0) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    state.pages.getOrNull(page)?.let { PageImage(it, state, Modifier.weight(1f)) }
                    state.pages.getOrNull(page + 1)?.let { PageImage(it, state, Modifier.weight(1f)) }
                }
            } else {
                state.pages.getOrNull(page)?.let { PageImage(it, state, Modifier.fillMaxSize()) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageImage(page: ReaderPage, state: ReaderUiState, modifier: Modifier) {
    val context = LocalContext.current
    var scale by remember(page.index) { mutableStateOf(1f) }
    var offset by remember(page.index) { mutableStateOf(Offset.Zero) }
    val isZoomed by remember { derivedStateOf { scale > 1.01f } }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = nextScale
        offset = if (nextScale <= 1.01f) Offset.Zero else offset + panChange
    }
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(page.uri)
            .allowHardware(false)
            .crossfade(false)
            .build(),
        contentDescription = "Page ${page.index + 1}",
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.colorMatrix(readerMatrix(state.brightness, state.contrast)),
        modifier = modifier
            .pointerInput(page.index, isZoomed) {
                if (!isZoomed) return@pointerInput
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offset += dragAmount
                }
            }
            .transformable(
                state = transformableState,
                canPan = { scale > 1.01f },
                lockRotationOnZoomPan = true,
            )
            .pointerInput(page.index) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
    )
}

@Composable
private fun ReaderOverlay(
    state: ReaderUiState,
    onBack: () -> Unit,
    onBookmark: () -> Unit,
    onBookmarks: () -> Unit,
    onTune: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onJump: (Int) -> Unit,
) {
    val isBookmarked = state.bookmarks.any { it.page == state.currentPage }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Color.White) }
            Text(state.comic?.title.orEmpty(), Modifier.weight(1f), color = Color.White, maxLines = 1)
            IconButton(onClick = onBookmark) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                    tint = if (isBookmarked) MaterialTheme.colorScheme.tertiary else Color.White,
                )
            }
            IconButton(onClick = onBookmarks) { Icon(Icons.Outlined.Bookmarks, "Bookmarks", tint = Color.White) }
            IconButton(onClick = onTune) { Icon(Icons.Outlined.Tune, "Display", tint = Color.White) }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onPrevious) { Icon(Icons.Outlined.ChevronLeft, "Previous", tint = Color.White) }
                Text("${(state.currentPage + 1).coerceAtMost(state.pageCount)} / ${state.pageCount}", color = Color.White)
                IconButton(onClick = onNext) { Icon(Icons.Outlined.ChevronRight, "Next", tint = Color.White) }
            }
            Slider(
                value = state.currentPage.toFloat(),
                onValueChange = { onJump(it.toInt()) },
                valueRange = 0f..(state.pageCount - 1).coerceAtLeast(0).toFloat(),
                steps = (state.pageCount - 2).coerceAtLeast(0),
            )
        }
    }
}

@Composable
private fun TuneSheet(state: ReaderUiState, onPrefs: (ReaderLayoutMode, ReadingDirection, Float, Float) -> Unit) {
    var brightness by remember(state.brightness) { mutableStateOf(state.brightness) }
    var contrast by remember(state.contrast) { mutableStateOf(state.contrast) }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Reader display", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { onPrefs(state.readerMode, state.direction, brightness, contrast) }) {
                Icon(Icons.Outlined.Close, "Done")
            }
        }
        Text("Brightness")
        Slider(value = brightness, onValueChange = {
            brightness = it
            onPrefs(state.readerMode, state.direction, brightness, contrast)
        }, valueRange = -0.5f..0.5f)
        Text("Contrast")
        Slider(value = contrast, onValueChange = {
            contrast = it
            onPrefs(state.readerMode, state.direction, brightness, contrast)
        }, valueRange = 0.5f..1.8f)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReaderLayoutMode.entries.forEach { mode -> AssistChip(onClick = { onPrefs(mode, state.direction, brightness, contrast) }, label = { Text(mode.name) }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReadingDirection.entries.forEach { direction -> AssistChip(onClick = { onPrefs(state.readerMode, direction, brightness, contrast) }, label = { Text(direction.name.uppercase()) }) }
        }
        Spacer(Modifier.height(20.dp))
    }
}

private fun readerMatrix(brightness: Float, contrast: Float): ColorMatrix {
    val translate = brightness * 255f + (-0.5f * contrast + 0.5f) * 255f
    return ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f,
        )
    )
}


@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ReaderScreenPreview() {
    PanelixTheme(darkTheme = true) {
        ReaderScreen(
            state = previewReaderState(),
            onBack = {},
            onPage = {},
            onBookmark = {},
            onPrefs = { _, _, _, _ -> },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F9F8)
@Composable
private fun TuneSheetPreview() {
    PanelixTheme {
        TuneSheet(
            state = previewReaderState(),
            onPrefs = { _, _, _, _ -> },
        )
    }
}

private fun previewReaderState() = ReaderUiState(
    comic = ComicEntity(
        id = "preview-reader",
        uri = "content://panelix/preview-reader",
        folderUri = "content://panelix/library",
        title = "The Clockwork Harbor",
        format = ComicFormat.cbz,
        coverUri = null,
        pageCount = 8,
        fileSize = null,
        addedAt = 0L,
        updatedAt = 0L,
        lastOpenedAt = 0L,
        isAvailable = true,
        parserMessage = null,
        sourceModifiedAt = null,
    ),
    pages = List(8) { ReaderPage(index = it, uri = Uri.parse("content://panelix/page-$it")) },
    pageCount = 8,
    currentPage = 2,
    bookmarks = listOf(BookmarkEntity(id = 1, comicId = "preview-reader", page = 2, note = null)),
    isLoading = false,
)
