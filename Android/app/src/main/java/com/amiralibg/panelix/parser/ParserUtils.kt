package com.amiralibg.panelix.parser

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.amiralibg.panelix.cache.ComicCacheManager
import java.io.File
import java.text.Collator
import java.util.Locale

private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif")

fun String.extensionLower(): String = substringAfterLast('.', "").lowercase(Locale.US)
fun String.isImageName(): Boolean = extensionLower() in imageExtensions

fun naturalComparator(): Comparator<String> {
    val collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }
    val regex = Regex("(\\d+)|(\\D+)")
    fun chunks(s: String) = regex.findAll(s).map { it.value }.toList()
    return Comparator { a, b ->
        val ac = chunks(a)
        val bc = chunks(b)
        for (i in 0 until minOf(ac.size, bc.size)) {
            val ai = ac[i].toLongOrNull()
            val bi = bc[i].toLongOrNull()
            val cmp = if (ai != null && bi != null) ai.compareTo(bi) else collator.compare(ac[i], bc[i])
            if (cmp != 0) return@Comparator cmp
        }
        ac.size.compareTo(bc.size)
    }
}

suspend fun ContentResolver.copyUriToCache(source: ComicSource, cache: ComicCacheManager): File {
    val ext = source.displayName.extensionLower().ifBlank { source.format.name }
    val target = cache.sourceFile(cache.cacheKey(source.uri, source.fileSize, source.modifiedAt), ext)
    if (target.exists() && source.fileSize != null && target.length() == source.fileSize) return target
    openInputStream(source.uri)?.use { input ->
        target.outputStream().buffered().use { output -> input.copyTo(output, DEFAULT_BUFFER_SIZE) }
    } ?: error("Folder permission lost or source could not be opened")
    return target
}

fun decodeSampledBitmap(file: File, maxSide: Int = 1200): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    var sample = 1
    while (bounds.outWidth / sample > maxSide || bounds.outHeight / sample > maxSide) sample *= 2
    return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
}

fun DocumentFile.collectImageFiles(recursive: Boolean = false): List<DocumentFile> {
    val files = mutableListOf<DocumentFile>()
    listFiles().forEach { child ->
        when {
            child.isFile && child.name?.isImageName() == true -> files += child
            recursive && child.isDirectory -> files += child.collectImageFiles(true)
        }
    }
    return files.sortedWith(compareBy(naturalComparator()) { it.name.orEmpty() })
}
