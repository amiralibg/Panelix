package com.amiralibg.panelix.parser

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.amiralibg.panelix.cache.ComicCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class PdfComicParser(
    private val resolver: ContentResolver,
    private val cache: ComicCacheManager,
) : ComicParser {
    override suspend fun parseForLibrary(source: ComicSource): LibraryMetadata = withContext(Dispatchers.IO) {
        runCatching {
            openRenderer(source.uri).use { renderer ->
                val cover = renderer.openPage(0).use { page ->
                    val width = 600
                    val height = (width * page.height / page.width.toFloat()).toInt().coerceAtLeast(1)
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
                LibraryMetadata(cache.saveCover(cache.cacheKey(source.uri, source.fileSize, source.modifiedAt), cover).toString(), renderer.pageCount, null)
            }
        }.getOrElse { LibraryMetadata(null, null, "PDF could not render: ${it.message ?: "unknown error"}") }
    }

    override suspend fun loadForReader(source: ComicSource): ReaderDocument {
        val pageCount = source.knownPageCount ?: 1
        return ReaderDocument(
            pageCount = pageCount,
            pages = flow {
                val key = cache.cacheKey(source.uri, source.fileSize, source.modifiedAt)
                val file = resolver.copyUriToCache(source, cache)
                val pageDir = cache.pageDir(key)
                val rendered = mutableListOf<ReaderPage>()
                openRenderer(file).use { renderer ->
                    for (i in 0 until renderer.pageCount) {
                        val out = File(pageDir, "%05d.jpg".format(i))
                        if (!out.exists()) {
                            renderer.openPage(i).use { page ->
                                val width = 1200
                                val height = (width * page.height / page.width.toFloat()).toInt().coerceAtLeast(1)
                                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                                bitmap.recycle()
                            }
                        }
                        rendered += ReaderPage(i, Uri.fromFile(out))
                        emit(rendered.toList())
                    }
                }
            }.flowOn(Dispatchers.IO),
            parserMessage = null,
        )
    }

    private fun openRenderer(file: File): PdfRenderer {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return PdfRenderer(descriptor)
    }

    private fun openRenderer(uri: Uri): PdfRenderer {
        val descriptor = resolver.openFileDescriptor(uri, "r")
            ?: error("Folder permission lost or source could not be opened")
        return PdfRenderer(descriptor)
    }
}
