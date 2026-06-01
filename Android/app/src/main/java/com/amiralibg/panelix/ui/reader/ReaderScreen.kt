package com.amiralibg.panelix.ui.reader

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.automirrored.outlined.ChromeReaderMode
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.amiralibg.panelix.data.AccentColor
import com.amiralibg.panelix.data.BookmarkEntity
import com.amiralibg.panelix.data.ComicEntity
import com.amiralibg.panelix.data.ComicFormat
import com.amiralibg.panelix.data.ReaderLayoutMode
import com.amiralibg.panelix.data.ReadingDirection
import com.amiralibg.panelix.parser.ReaderPage
import com.amiralibg.panelix.ui.theme.LocalPanelixPalette
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
    KeepScreenOn(enabled = state.appPreferences.keepScreenAwake)
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
            state.readerMode == ReaderLayoutMode.vertical -> VerticalReader(state, onPage) { controls = !controls }
            else -> PagedReader(state, onPage) { controls = !controls }
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
        ModalBottomSheet(
            onDismissRequest = { showBookmarks = false },
            containerColor = LocalPanelixPalette.current.surface,
        ) {
            BookmarksSheet(
                bookmarks = state.bookmarks,
                onJump = {
                    onPage(it)
                    showBookmarks = false
                },
            )
        }
    }
    if (showTune) {
        ModalBottomSheet(
            onDismissRequest = { showTune = false },
            containerColor = LocalPanelixPalette.current.surface,
        ) {
            DisplaySheet(state = state, onPrefs = onPrefs, onClose = { showTune = false })
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

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    if (LocalInspectionMode.current) return
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VerticalReader(state: ReaderUiState, onPage: (Int) -> Unit, onTap: () -> Unit) {
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
            state.pages.getOrNull(page)?.let { PageImage(it, state, Modifier.fillMaxSize(), onTap) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PagedReader(state: ReaderUiState, onPage: (Int) -> Unit, onTap: () -> Unit) {
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
                    state.pages.getOrNull(page)?.let { PageImage(it, state, Modifier.weight(1f), onTap) }
                    state.pages.getOrNull(page + 1)?.let { PageImage(it, state, Modifier.weight(1f), onTap) }
                }
            } else {
                state.pages.getOrNull(page)?.let { PageImage(it, state, Modifier.fillMaxSize(), onTap) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageImage(page: ReaderPage, state: ReaderUiState, modifier: Modifier, onTap: () -> Unit = {}) {
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
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
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
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
        ) {
            ReaderTopPill(state, onBack, onBookmark, onBookmarks, onTune)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        ) {
            ReaderBottomBar(state, onPrevious, onNext, onJump)
        }
    }
}

@Composable
private fun ReaderTopPill(
    state: ReaderUiState,
    onBack: () -> Unit,
    onBookmark: () -> Unit,
    onBookmarks: () -> Unit,
    onTune: () -> Unit,
) {
    val palette = LocalPanelixPalette.current
    val isBookmarked = state.bookmarks.any { it.page == state.currentPage }
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x8C12121A))
            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GlassButton(icon = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", onClick = onBack)
        Column(Modifier.weight(1f)) {
            Text(
                state.comic?.title.orEmpty(),
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                state.comic?.let {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(palette.accent)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text(
                            it.format.name.uppercase(),
                            color = palette.onAccent,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
                val current = (state.currentPage + 1).coerceAtMost(state.pageCount).coerceAtLeast(1)
                Text(
                    "Page $current of ${state.pageCount}",
                    color = Color(0xCCFFFFFF),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        GlassButton(
            icon = if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
            iconTint = if (isBookmarked) palette.accent else Color.White,
            onClick = onBookmark,
        )
        GlassButton(icon = Icons.Outlined.Bookmarks, contentDescription = "Bookmarks", onClick = onBookmarks)
        GlassButton(icon = Icons.Outlined.Tune, contentDescription = "Display", onClick = onTune)
    }
}

@Composable
private fun GlassButton(
    icon: ImageVector,
    contentDescription: String,
    iconTint: Color = Color.White,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0x12FFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = iconTint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ReaderBottomBar(
    state: ReaderUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onJump: (Int) -> Unit,
) {
    val palette = LocalPanelixPalette.current
    val safePageCount = state.pageCount.coerceAtLeast(1)
    val displayPage = (state.currentPage + 1).coerceAtMost(safePageCount).coerceAtLeast(1)
    val pct = displayPage.toFloat() / safePageCount.toFloat()
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0x8C12121A))
            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(pct.coerceIn(0.04f, 1f))
                    .height(18.dp),
                contentAlignment = Alignment.TopEnd,
            ) {
                Box(
                    Modifier
                        .wrapContentWidth(Alignment.End, unbounded = true)
                        .clip(RoundedCornerShape(7.dp))
                        .background(palette.accent)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        displayPage.toString(),
                        color = palette.onAccent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Slider(
            value = (state.currentPage).toFloat(),
            onValueChange = { onJump(it.toInt()) },
            valueRange = 0f..(state.pageCount - 1).coerceAtLeast(0).toFloat(),
            steps = (state.pageCount - 2).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = Color(0x29FFFFFF),
            ),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            NavButton(icon = Icons.Outlined.ChevronLeft, contentDescription = "Previous", filled = false, accent = palette.accent, onClick = onPrevious)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        displayPage.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        " / ${state.pageCount}",
                        color = Color(0x66FFFFFF),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                Spacer(Modifier.height(1.dp))
                Text(
                    "${(pct * 100).toInt()}% READ",
                    color = palette.accent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            NavButton(icon = Icons.Outlined.ChevronRight, contentDescription = "Next", filled = true, accent = palette.accent, onClick = onNext)
        }
    }
}

@Composable
private fun NavButton(
    icon: ImageVector,
    contentDescription: String,
    filled: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (filled) accent else Color(0x12FFFFFF))
            .border(1.dp, if (filled) accent else Color(0x1FFFFFFF), RoundedCornerShape(15.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription,
            tint = if (filled) LocalPanelixPalette.current.onAccent else Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun BookmarksSheet(
    bookmarks: List<BookmarkEntity>,
    onJump: (Int) -> Unit,
) {
    val palette = LocalPanelixPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Bookmarks",
            style = MaterialTheme.typography.titleLarge,
            color = palette.text,
        )
        if (bookmarks.isEmpty()) {
            Text("No bookmarks yet", color = palette.muted, style = MaterialTheme.typography.bodyMedium)
        }
        bookmarks.forEach { bm ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surface2)
                    .clickable { onJump(bm.page) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Page ${bm.page + 1}", color = palette.text, style = MaterialTheme.typography.bodyLarge)
                Icon(Icons.Outlined.ChevronRight, null, tint = palette.muted, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DisplaySheet(
    state: ReaderUiState,
    onPrefs: (ReaderLayoutMode, ReadingDirection, Float, Float) -> Unit,
    onClose: () -> Unit,
) {
    val palette = LocalPanelixPalette.current
    var brightness by remember(state.brightness) { mutableStateOf(state.brightness) }
    var contrast by remember(state.contrast) { mutableStateOf(state.contrast) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 0.dp, bottom = 26.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Display", style = MaterialTheme.typography.titleLarge, color = palette.text)
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.surface2)
                    .border(1.dp, palette.line, RoundedCornerShape(10.dp))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Close, "Close", tint = palette.sub, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(22.dp))
        SheetLabel("Brightness")
        Spacer(Modifier.height(12.dp))
        IconSlider(
            value = brightness,
            range = -0.5f..0.5f,
            left = Icons.Outlined.DarkMode,
            right = Icons.Outlined.LightMode,
            onValueChange = {
                brightness = it
                onPrefs(state.readerMode, state.direction, brightness, contrast)
            },
        )
        Spacer(Modifier.height(20.dp))
        SheetLabel("Contrast")
        Spacer(Modifier.height(12.dp))
        IconSlider(
            value = contrast,
            range = 0.5f..1.8f,
            left = Icons.Outlined.Contrast,
            right = Icons.Outlined.Contrast,
            onValueChange = {
                contrast = it
                onPrefs(state.readerMode, state.direction, brightness, contrast)
            },
        )
        Spacer(Modifier.height(24.dp))
        SheetLabel("Page layout")
        Spacer(Modifier.height(12.dp))
        SheetSegmented(
            value = state.readerMode,
            options = listOf(
                SheetSegment(ReaderLayoutMode.vertical, "Vertical", Icons.Outlined.UnfoldMore),
                SheetSegment(ReaderLayoutMode.horizontal, "Horizontal", Icons.Outlined.SwapHoriz),
                SheetSegment(ReaderLayoutMode.spread, "Spread", Icons.AutoMirrored.Outlined.ChromeReaderMode),
            ),
            onChange = { onPrefs(it, state.direction, brightness, contrast) },
        )
        Spacer(Modifier.height(20.dp))
        SheetLabel("Reading direction")
        Spacer(Modifier.height(12.dp))
        SheetSegmented(
            value = state.direction,
            options = listOf(
                SheetSegment(ReadingDirection.ltr, "Left → Right", Icons.AutoMirrored.Outlined.ArrowForward),
                SheetSegment(ReadingDirection.rtl, "Right → Left", Icons.AutoMirrored.Outlined.ArrowBack),
            ),
            onChange = { onPrefs(state.readerMode, it, brightness, contrast) },
        )
    }
}

@Composable
private fun SheetLabel(text: String) {
    val palette = LocalPanelixPalette.current
    Text(text, color = palette.sub, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun IconSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    left: ImageVector,
    right: ImageVector,
    onValueChange: (Float) -> Unit,
) {
    val palette = LocalPanelixPalette.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(left, null, tint = palette.muted, modifier = Modifier.size(18.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = palette.surfaceHi,
            ),
            modifier = Modifier.weight(1f),
        )
        Icon(right, null, tint = palette.muted, modifier = Modifier.size(18.dp))
    }
}

private data class SheetSegment<T>(val key: T, val label: String, val icon: ImageVector)

@Composable
private fun <T> SheetSegmented(
    value: T,
    options: List<SheetSegment<T>>,
    onChange: (T) -> Unit,
) {
    val palette = LocalPanelixPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(palette.surface2)
            .border(1.dp, palette.line, RoundedCornerShape(13.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { opt ->
            val sel = opt.key == value
            Row(
                Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (sel) palette.accent else Color.Transparent)
                    .clickable { onChange(opt.key) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    opt.icon,
                    null,
                    tint = if (sel) palette.onAccent else palette.sub,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    opt.label,
                    color = if (sel) palette.onAccent else palette.sub,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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
        ),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ReaderScreenPreview() {
    PanelixTheme(darkTheme = true, accent = AccentColor.coral) {
        ReaderScreen(
            state = previewReaderState(),
            onBack = {},
            onPage = {},
            onBookmark = {},
            onPrefs = { _, _, _, _ -> },
        )
    }
}

private fun previewReaderState() = ReaderUiState(
    comic = ComicEntity(
        id = "preview-reader",
        uri = "content://panelix/preview-reader",
        folderUri = "content://panelix/library",
        title = "Batman: Damned 2",
        format = ComicFormat.cbr,
        coverUri = null,
        pageCount = 52,
        fileSize = null,
        addedAt = 0L,
        updatedAt = 0L,
        lastOpenedAt = 0L,
        isAvailable = true,
        parserMessage = null,
        sourceModifiedAt = null,
    ),
    pages = List(8) { ReaderPage(index = it, uri = Uri.parse("content://panelix/page-$it")) },
    pageCount = 52,
    currentPage = 1,
    bookmarks = listOf(BookmarkEntity(id = 1, comicId = "preview-reader", page = 2, note = null)),
    isLoading = false,
)
