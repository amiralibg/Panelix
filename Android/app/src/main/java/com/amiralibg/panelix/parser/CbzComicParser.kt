package com.amiralibg.panelix.parser

import android.content.ContentResolver
import android.net.Uri
import com.amiralibg.panelix.cache.ComicCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

class CbzComicParser(
    private val resolver: ContentResolver,
    private val cache: ComicCacheManager,
) : ComicParser {
    override suspend fun parseForLibrary(source: ComicSource): LibraryMetadata = withContext(Dispatchers.IO) {
        runCatching {
            var count = 0
            var coverName: String? = null
            var coverBytes: ByteArray? = null
            resolver.openInputStream(source.uri)?.buffered()?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name.isImageName()) {
                            count += 1
                            if (coverName == null || naturalComparator().compare(entry.name, coverName) < 0) {
                                coverName = entry.name
                                coverBytes = ByteArrayOutputStream().use { bytes ->
                                    zip.copyTo(bytes)
                                    bytes.toByteArray()
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: error("Folder permission lost or source could not be opened")
            if (count == 0) return@withContext LibraryMetadata(null, null, "Archive has no readable image pages")
            val cover = cache.coverFile(cache.cacheKey(source.uri, source.fileSize, source.modifiedAt))
            coverBytes?.let { cover.writeBytes(it) }
            LibraryMetadata(Uri.fromFile(cover).toString(), count, null)
        }.getOrElse { LibraryMetadata(null, null, "CBZ could not be read: ${it.message ?: "unknown error"}") }
    }

    override suspend fun loadForReader(source: ComicSource): ReaderDocument {
        val pageCount = source.knownPageCount ?: 1
        return ReaderDocument(pageCount, flow {
            val file = resolver.copyUriToCache(source, cache)
            val key = cache.cacheKey(source.uri, source.fileSize, source.modifiedAt)
            val pageDir = cache.pageDir(key)
            val names = ZipFile(file).use { zip ->
                zip.entries().asSequence().filter { !it.isDirectory && it.name.isImageName() }
                    .map { it.name }.sortedWith(naturalComparator()).toList()
            }
            if (names.isEmpty()) {
                emit(emptyList())
                return@flow
            }
            val pages = mutableListOf<ReaderPage>()
            ZipFile(file).use { zip ->
                names.forEachIndexed { index, name ->
                    val ext = name.extensionLower().ifBlank { "jpg" }
                    val out = File(pageDir, "%05d.$ext".format(index))
                    if (!out.exists()) {
                        zip.getInputStream(zip.getEntry(name)).use { input ->
                            out.outputStream().buffered().use { output -> input.copyTo(output) }
                        }
                    }
                    pages += ReaderPage(index, Uri.fromFile(out))
                    emit(pages.toList())
                }
            }
        }.flowOn(Dispatchers.IO), null)
    }
}
