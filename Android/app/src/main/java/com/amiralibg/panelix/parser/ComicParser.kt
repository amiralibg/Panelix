package com.amiralibg.panelix.parser

import android.net.Uri
import com.amiralibg.panelix.data.ComicFormat
import kotlinx.coroutines.flow.Flow

data class ComicSource(
    val uri: Uri,
    val displayName: String,
    val format: ComicFormat,
    val folderUri: String,
    val fileSize: Long?,
    val modifiedAt: Long?,
    val knownPageCount: Int? = null,
)

data class LibraryMetadata(
    val coverUri: String?,
    val pageCount: Int?,
    val parserMessage: String?,
)

data class ReaderPage(
    val index: Int,
    val uri: Uri,
)

data class ReaderDocument(
    val pageCount: Int,
    val pages: Flow<List<ReaderPage>>,
    val parserMessage: String?,
)

interface ComicParser {
    suspend fun parseForLibrary(source: ComicSource): LibraryMetadata
    suspend fun loadForReader(source: ComicSource): ReaderDocument
}

class UnsupportedComicParser(private val message: String) : ComicParser {
    override suspend fun parseForLibrary(source: ComicSource) = LibraryMetadata(null, null, message)
    override suspend fun loadForReader(source: ComicSource) = ReaderDocument(0, kotlinx.coroutines.flow.flowOf(emptyList()), message)
}
