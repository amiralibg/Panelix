package com.amiralibg.panelix.parser

import android.content.ContentResolver
import android.net.Uri
import com.amiralibg.panelix.cache.ComicCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.sf.sevenzipjbinding.ExtractAskMode
import net.sf.sevenzipjbinding.ExtractOperationResult
import net.sf.sevenzipjbinding.IArchiveExtractCallback
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.ISequentialOutStream
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.SevenZipException
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import java.io.File
import java.io.RandomAccessFile
import java.util.Locale

class CbrComicParser(
    private val resolver: ContentResolver,
    private val cache: ComicCacheManager,
) : ComicParser {
    override suspend fun parseForLibrary(source: ComicSource): LibraryMetadata = withContext(Dispatchers.IO) {
        val key = cache.cacheKey(source.uri, source.fileSize, source.modifiedAt)
        runCatching {
            val images = listImages(source)
            if (images.isEmpty()) return@withContext LibraryMetadata(null, null, "Archive has no readable image pages")
            val cover = extractCover(source, images.first())
            LibraryMetadata(
                coverUri = Uri.fromFile(cover).toString(),
                pageCount = images.size,
                parserMessage = null,
            )
        }.getOrElse { LibraryMetadata(null, null, rarMessage(it)) }
            .also { cache.sourceFile(key, source.displayName.extensionLower().ifBlank { "cbr" }).delete() }
    }

    override suspend fun loadForReader(source: ComicSource): ReaderDocument {
        val pageCount = source.knownPageCount ?: 1
        return ReaderDocument(pageCount, flow {
            val images = runCatching { listImages(source) }.getOrElse {
                emit(emptyList())
                return@flow
            }
            if (images.isEmpty()) {
                emit(emptyList())
                return@flow
            }
            emitAll(extractPages(source, images))
        }.flowOn(Dispatchers.IO), null)
    }

    private suspend fun listImages(source: ComicSource): List<SevenZipImage> {
        val file = resolver.copyUriToCache(source, cache)
        return openArchive(file) { archive ->
            (0 until archive.numberOfItems)
                .mapNotNull { index ->
                    val path = archive.getStringProperty(index, PropID.PATH).orEmpty()
                    val isFolder = archive.getProperty(index, PropID.IS_FOLDER) as? Boolean ?: false
                    if (!isFolder && path.isImageName()) SevenZipImage(index, path) else null
                }
                .sortedWith(compareBy(naturalComparator()) { it.path })
        }
    }

    private fun extractPages(source: ComicSource, images: List<SevenZipImage>): Flow<List<ReaderPage>> = callbackFlow {
        val file = resolver.copyUriToCache(source, cache)
        val dir = cache.pageDir(cache.cacheKey(source.uri, source.fileSize, source.modifiedAt))
        val pages = MutableList<ReaderPage?>(images.size) { index ->
            val existing = pageFile(dir, index, images[index].path)
            if (existing.exists() && existing.length() > 0L) ReaderPage(index, Uri.fromFile(existing)) else null
        }
        fun readyPages() = pages.takeWhile { it != null }.filterNotNull()
        val knownPages = readyPages()
        if (knownPages.isNotEmpty()) trySend(knownPages)
        runCatching {
            openArchive(file) { archive ->
                val byArchiveIndex = images.withIndex().associateBy { it.value.archiveIndex }
                val missingArchiveIndexes = images
                    .filterIndexed { pageIndex, _ -> pages[pageIndex] == null }
                    .map { it.archiveIndex }
                    .toIntArray()
                if (missingArchiveIndexes.isEmpty()) return@openArchive
                archive.extract(missingArchiveIndexes, false, object : IArchiveExtractCallback {
                    private var output: java.io.OutputStream? = null
                    private var currentOut: File? = null
                    private var currentPageIndex: Int? = null

                    override fun getStream(index: Int, askExtractMode: ExtractAskMode): ISequentialOutStream? {
                        if (askExtractMode != ExtractAskMode.EXTRACT) return null
                        val image = byArchiveIndex[index] ?: return null
                        currentPageIndex = image.index
                        currentOut = pageFile(dir, image.index, image.value.path)
                        output = currentOut!!.outputStream().buffered()
                        return ISequentialOutStream { data ->
                            output?.write(data)
                            data.size
                        }
                    }

                    override fun prepareOperation(askExtractMode: ExtractAskMode) = Unit

                    override fun setOperationResult(extractOperationResult: ExtractOperationResult) {
                        output?.close()
                        val pageIndex = currentPageIndex
                        val out = currentOut
                        output = null
                        currentOut = null
                        currentPageIndex = null
                        if (extractOperationResult == ExtractOperationResult.OK && pageIndex != null && out != null) {
                            pages[pageIndex] = ReaderPage(pageIndex, Uri.fromFile(out))
                            trySend(readyPages())
                        } else {
                            out?.delete()
                        }
                    }

                    override fun setCompleted(complete: Long) = Unit
                    override fun setTotal(total: Long) = Unit
                })
            }
        }.onFailure {
            trySend(readyPages())
        }
        trySend(readyPages())
        close()
        awaitClose { }
    }

    private suspend fun extractCover(source: ComicSource, image: SevenZipImage): File {
        val file = resolver.copyUriToCache(source, cache)
        val cover = cache.coverFile(cache.cacheKey(source.uri, source.fileSize, source.modifiedAt))
        if (cover.exists() && cover.length() > 0) return cover
        openArchive(file) { archive ->
            cover.outputStream().buffered().use { output ->
                val result = archive.simpleInterface.archiveItems[image.archiveIndex]
                    .extractSlow(ISequentialOutStream { data ->
                        output.write(data)
                        data.size
                    })
                if (result != ExtractOperationResult.OK) {
                    cover.delete()
                    error("CBR cover extraction failed: $result")
                }
            }
        }
        return cover
    }

    private fun pageFile(dir: File, pageIndex: Int, path: String): File {
        val ext = path.extensionLower().ifBlank { "jpg" }.lowercase(Locale.US)
        return File(dir, "%05d.%s".format(pageIndex, ext))
    }

    private fun rarMessage(error: Throwable): String {
        val text = error.message.orEmpty()
        return when {
            text.contains("password", ignoreCase = true) || text.contains("encrypted", ignoreCase = true) -> "Password-protected CBR unsupported"
            text.contains("unsupported", ignoreCase = true) -> "Unsupported RAR variant"
            text.contains("CRC", ignoreCase = true) || text.contains("damaged", ignoreCase = true) -> "CBR archive appears to be damaged"
            else -> "CBR could not be read: ${error.message ?: "unsupported archive"}"
        }
    }

    private fun <T> openArchive(file: File, block: (IInArchive) -> T): T {
        initializeSevenZip()
        RandomAccessFile(file, "r").use { randomAccessFile ->
            val archive = SevenZip.openInArchive(null, RandomAccessFileInStream(randomAccessFile))
            try {
                return block(archive)
            } finally {
                archive.close()
            }
        }
    }

    private fun initializeSevenZip() {
        runCatching { SevenZip.initSevenZipFromPlatformJAR() }
            .recoverCatching {
                // Android AAR builds may load native libraries through the package loader.
                SevenZip.getSevenZipVersion()
            }
            .getOrElse { throw SevenZipException("Native RAR extractor could not initialize", it) }
    }

    private data class SevenZipImage(val archiveIndex: Int, val path: String)
}
