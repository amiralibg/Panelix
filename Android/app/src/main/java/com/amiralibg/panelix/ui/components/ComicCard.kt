package com.amiralibg.panelix.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.amiralibg.panelix.data.ComicEntity
import com.amiralibg.panelix.data.ComicFormat
import com.amiralibg.panelix.ui.theme.PanelixTheme

@Composable
fun ComicCard(
    comic: ComicEntity,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(enabled = comic.isAvailable, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        if (compact) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Cover(comic, Modifier.size(width = 68.dp, height = 98.dp))
                ComicText(comic, Modifier.weight(1f))
            }
        } else {
            Column(Modifier.padding(16.dp)) {
                Cover(comic, Modifier.fillMaxWidth().aspectRatio(0.68f))
                Spacer(Modifier.height(10.dp))
                ComicText(comic)
            }
        }
    }
}

@Composable
private fun Cover(comic: ComicEntity, modifier: Modifier) {
    val context = LocalContext.current
    Box(
        modifier = modifier.clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
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
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.secondaryContainer,
                        )
                    )
                )
            )
            Text(comic.format.name.uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0.68f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.28f),
                )
            )
        )
    }
}

@Composable
private fun ComicText(comic: ComicEntity, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(comic.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        Text(
            listOfNotNull(comic.format.name.uppercase(), comic.pageCount?.let { "$it pages" }).joinToString(" • "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        if (comic.parserMessage != null) {
            AssistChip(
                onClick = {},
                label = { Text(comic.parserMessage, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Outlined.Warning, null, Modifier.size(16.dp)) },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F9F8)
@Composable
private fun ComicCardPreview() {
    PanelixTheme {
        ComicCard(
            comic = previewComic(),
            modifier = Modifier.padding(16.dp),
            onClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F9F8, widthDp = 420)
@Composable
private fun ComicCardCompactPreview() {
    PanelixTheme {
        ComicCard(
            comic = previewComic(parserMessage = "Missing cover, using fallback"),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            compact = true,
            onClick = {},
        )
    }
}

private fun previewComic(parserMessage: String? = null) = ComicEntity(
    id = "preview-comic",
    uri = "content://panelix/preview",
    folderUri = "content://panelix/library",
    title = "The Clockwork Harbor",
    format = ComicFormat.cbz,
    coverUri = null,
    pageCount = 124,
    fileSize = 48_000_000,
    addedAt = 0L,
    updatedAt = 0L,
    lastOpenedAt = 0L,
    isAvailable = true,
    parserMessage = parserMessage,
    sourceModifiedAt = null,
)
