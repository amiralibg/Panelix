package com.amiralibg.panelix.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.amiralibg.panelix.data.ComicEntity
import com.amiralibg.panelix.data.ComicFormat
import com.amiralibg.panelix.parser.CbzComicParser
import com.amiralibg.panelix.parser.CbrComicParser
import com.amiralibg.panelix.parser.ComicParser
import com.amiralibg.panelix.parser.ComicSource
import com.amiralibg.panelix.parser.ImageFolderComicParser
import com.amiralibg.panelix.parser.PdfComicParser
import com.amiralibg.panelix.parser.UnsupportedComicParser
import com.amiralibg.panelix.parser.extensionLower
import com.amiralibg.panelix.parser.isImageName
import com.amiralibg.panelix.parser.naturalComparator
import java.security.MessageDigest

data class ScanProgress(
    val completed: Int,
    val total: Int,
    val currentTitle: String?,
)

class ComicScanner(
    private val context: Context,
    private val pdfParser: PdfComicParser,
    private val cbzParser: CbzComicParser,
    private val cbrParser: CbrComicParser,
    private val imageFolderParser: ImageFolderComicParser,
) {
    suspend fun scanFolder(
        folderUri: String,
        existing: Map<String, ComicEntity>,
        onProgress: (ScanProgress) -> Unit = {},
    ): List<ComicEntity> {
        val rootUri = Uri.parse(folderUri)
        val root = DocumentFile.fromTreeUri(context, rootUri)
            ?: return existing.values.map {
                it.copy(isAvailable = false, parserMessage = "Folder permission lost", updatedAt = System.currentTimeMillis())
            }
        val total = countComicCandidates(root)
        var completed = 0
        onProgress(ScanProgress(completed, total, root.name))
        val found = mutableListOf<ComicEntity>()
        walk(root, folderUri, found, existing) { source ->
            completed += 1
            onProgress(ScanProgress(completed, total, source.displayName))
        }
        val foundIds = found.mapTo(mutableSetOf()) { it.id }
        val missing = existing.values.filterNot { it.id in foundIds }
            .map { it.copy(isAvailable = false, parserMessage = "Source file is no longer available", updatedAt = System.currentTimeMillis()) }
        return found + missing
    }

    private suspend fun walk(
        directory: DocumentFile,
        folderUri: String,
        found: MutableList<ComicEntity>,
        existing: Map<String, ComicEntity>,
        onScanned: (ComicSource) -> Unit,
    ) {
        val children = directory.listFiles().sortedWith(compareBy(naturalComparator()) { it.name.orEmpty() })
        val directImages = children.filter { it.isFile && it.name?.isImageName() == true }
        if (directImages.size >= 3) {
            val source = sourceFor(directory, folderUri, ComicFormat.folder)
            found += entityFor(source, existing[source.id()])
            onScanned(source)
        }
        children.forEach { child ->
            when {
                child.isDirectory -> walk(child, folderUri, found, existing, onScanned)
                child.isFile -> {
                    val format = child.name.orEmpty().toComicFormat()
                    if (format != ComicFormat.unknown) {
                        val source = sourceFor(child, folderUri, format)
                        found += entityFor(source, existing[source.id()])
                        onScanned(source)
                    }
                }
            }
        }
    }

    private fun countComicCandidates(directory: DocumentFile): Int {
        val children = directory.listFiles()
        val imageFolder = if (children.count { it.isFile && it.name?.isImageName() == true } >= 3) 1 else 0
        val fileComics = children.count { it.isFile && it.name.orEmpty().toComicFormat() != ComicFormat.unknown }
        val childComics = children.filter { it.isDirectory }.sumOf { countComicCandidates(it) }
        return imageFolder + fileComics + childComics
    }

    private suspend fun entityFor(source: ComicSource, old: ComicEntity?): ComicEntity {
        val parser = parserFor(source.format)
        val metadata = if (old != null && old.fileSize == source.fileSize && old.sourceModifiedAt == source.modifiedAt && old.coverUri != null) {
            null
        } else {
            parser.parseForLibrary(source)
        }
        val now = System.currentTimeMillis()
        return ComicEntity(
            id = source.id(),
            uri = source.uri.toString(),
            folderUri = source.folderUri,
            title = source.displayName.substringBeforeLast('.').ifBlank { source.displayName },
            format = source.format,
            coverUri = metadata?.coverUri ?: old?.coverUri,
            pageCount = metadata?.pageCount ?: old?.pageCount,
            fileSize = source.fileSize,
            addedAt = old?.addedAt ?: now,
            updatedAt = now,
            lastOpenedAt = old?.lastOpenedAt,
            isAvailable = metadata?.parserMessage == null || source.format in setOf(ComicFormat.cbt, ComicFormat.cb7),
            parserMessage = metadata?.parserMessage,
            sourceModifiedAt = source.modifiedAt,
        )
    }

    private fun parserFor(format: ComicFormat): ComicParser = when (format) {
        ComicFormat.pdf -> pdfParser
        ComicFormat.cbz -> cbzParser
        ComicFormat.cbr -> cbrParser
        ComicFormat.folder -> imageFolderParser
        ComicFormat.cbt -> UnsupportedComicParser("CBT detected but is not supported yet")
        ComicFormat.cb7 -> UnsupportedComicParser("CB7 detected but is not supported yet")
        ComicFormat.unknown -> UnsupportedComicParser("Unsupported comic format")
    }

    fun parserForReader(format: ComicFormat): ComicParser = parserFor(format)

    private fun sourceFor(file: DocumentFile, folderUri: String, format: ComicFormat) = ComicSource(
        uri = file.uri,
        displayName = file.name ?: "Untitled",
        format = format,
        folderUri = folderUri,
        fileSize = file.length().takeIf { it > 0 },
        modifiedAt = file.lastModified().takeIf { it > 0 },
    )

    private fun String.toComicFormat(): ComicFormat = when (extensionLower()) {
        "pdf" -> ComicFormat.pdf
        "cbz", "zip" -> ComicFormat.cbz
        "cbr", "rar" -> ComicFormat.cbr
        "cbt", "tar" -> ComicFormat.cbt
        "cb7", "7z" -> ComicFormat.cb7
        else -> ComicFormat.unknown
    }

    private fun ComicSource.id(): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(uri.toString().toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
