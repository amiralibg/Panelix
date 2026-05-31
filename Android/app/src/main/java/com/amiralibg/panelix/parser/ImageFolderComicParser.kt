package com.amiralibg.panelix.parser

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.amiralibg.panelix.cache.ComicCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

class ImageFolderComicParser(
    private val context: Context,
    private val cache: ComicCacheManager,
) : ComicParser {
    override suspend fun parseForLibrary(source: ComicSource): LibraryMetadata = withContext(Dispatchers.IO) {
        val folder = DocumentFile.fromTreeUri(context, source.uri)
            ?: DocumentFile.fromSingleUri(context, source.uri)
            ?: return@withContext LibraryMetadata(null, null, "Folder permission lost")
        val images = folder.collectImageFiles(recursive = false)
        if (images.size < 3) return@withContext LibraryMetadata(null, images.size, "Image folder needs at least 3 readable image files")
        val cover = images.first()
        LibraryMetadata(cover.uri.toString(), images.size, null)
    }

    override suspend fun loadForReader(source: ComicSource): ReaderDocument = withContext(Dispatchers.IO) {
        val folder = DocumentFile.fromTreeUri(context, source.uri)
            ?: DocumentFile.fromSingleUri(context, source.uri)
            ?: return@withContext ReaderDocument(0, flowOf(emptyList()), "Folder permission lost")
        val images = folder.collectImageFiles(recursive = false)
        ReaderDocument(images.size, flowOf(images.mapIndexed { index, file -> ReaderPage(index, file.uri) }), null)
    }
}
