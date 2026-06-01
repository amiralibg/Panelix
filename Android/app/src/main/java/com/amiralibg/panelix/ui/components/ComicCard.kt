package com.amiralibg.panelix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.amiralibg.panelix.data.AccentColor
import com.amiralibg.panelix.data.ComicEntity
import com.amiralibg.panelix.data.ComicFormat
import com.amiralibg.panelix.ui.theme.LocalPanelixPalette
import com.amiralibg.panelix.ui.theme.PanelixTheme

@Composable
fun ComicCard(
    comic: ComicEntity,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    showProgressBar: Boolean = true,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    val palette = LocalPanelixPalette.current
    if (compact) {
        Row(
            modifier
                .fillMaxWidth()
                .clickable(enabled = comic.isAvailable, onClick = onClick)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArt(
                comic = comic,
                progress = progress,
                showProgressBar = false,
                modifier = Modifier.size(width = 52.dp, height = 74.dp),
                radius = 9.dp,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    comic.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.text,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    secondaryLine(comic),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showProgressBar && progress > 0f) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .width(180.dp)
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
                }
            }
        }
    } else {
        Column(
            modifier
                .fillMaxWidth()
                .clickable(enabled = comic.isAvailable, onClick = onClick),
        ) {
            CoverArt(
                comic = comic,
                progress = progress,
                showProgressBar = showProgressBar,
                modifier = Modifier.fillMaxWidth().aspectRatio(0.69f),
                radius = 14.dp,
            )
            Spacer(Modifier.height(9.dp))
            Text(
                comic.title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.text,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                metaLine(comic, progress),
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun CoverArt(
    comic: ComicEntity,
    progress: Float,
    showProgressBar: Boolean,
    modifier: Modifier,
    radius: androidx.compose.ui.unit.Dp,
) {
    val palette = LocalPanelixPalette.current
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(palette.surface2),
    ) {
        if (comic.coverUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(comic.coverUri)
                    .allowHardware(true)
                    .crossfade(false)
                    .build(),
                contentDescription = comic.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        listOf(palette.surface2, palette.surfaceHi),
                    ),
                ),
            )
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                Text(
                    comic.format.name.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.muted,
                )
            }
        }
        Box(Modifier.matchParentSize().background(
            Brush.verticalGradient(
                0.55f to Color.Transparent,
                1f to Color(0x80000000),
            ),
        ))
        Box(
            Modifier
                .padding(top = 8.dp, end = 8.dp)
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xCC000000))
                .padding(horizontal = 7.dp, vertical = 3.dp),
        ) {
            Text(
                comic.format.name.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        if (showProgressBar && progress > 0f) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomStart)
                    .background(Color(0x66000000)),
            ) {
                Box(
                    Modifier
                        .fillMaxSize(progress.coerceIn(0f, 1f))
                        .background(palette.accent),
                )
            }
        }
    }
}

private fun secondaryLine(comic: ComicEntity): String {
    val parts = mutableListOf(comic.format.name.uppercase())
    comic.pageCount?.let { parts += "$it pages" }
    return parts.joinToString(" · ")
}

private fun metaLine(comic: ComicEntity, progress: Float): String {
    val pages = comic.pageCount ?: 0
    return if (progress > 0f && pages > 0) "${(progress * 100).toInt()}% · $pages pages"
    else if (pages > 0) "$pages pages"
    else comic.format.name.uppercase()
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0E10)
@Composable
private fun ComicCardPreview() {
    PanelixTheme(darkTheme = true, accent = AccentColor.coral) {
        ComicCard(
            comic = previewComic(),
            modifier = Modifier.padding(16.dp).width(170.dp),
            progress = 0.34f,
            onClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0E10, widthDp = 420)
@Composable
private fun ComicCardCompactPreview() {
    PanelixTheme(darkTheme = true, accent = AccentColor.coral) {
        ComicCard(
            comic = previewComic(),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            compact = true,
            progress = 0.62f,
            onClick = {},
        )
    }
}

private fun previewComic() = ComicEntity(
    id = "preview-comic",
    uri = "content://panelix/preview",
    folderUri = "content://panelix/library",
    title = "Batman: Hush",
    format = ComicFormat.cbr,
    coverUri = null,
    pageCount = 291,
    fileSize = 48_000_000,
    addedAt = 0L,
    updatedAt = 0L,
    lastOpenedAt = 0L,
    isAvailable = true,
    parserMessage = null,
    sourceModifiedAt = null,
)
