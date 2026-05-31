package com.amiralibg.panelix.cache

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.security.MessageDigest

class ComicCacheManager(private val context: Context) {
    private val root = File(context.cacheDir, "comics").apply { mkdirs() }
    private val covers = File(root, "covers").apply { mkdirs() }
    private val sources = File(root, "sources").apply { mkdirs() }
    private val pages = File(root, "pages").apply { mkdirs() }

    fun cacheKey(uri: Uri, fileSize: Long?, modifiedAt: Long?): String {
        return sha256("${uri}|${fileSize ?: -1}|${modifiedAt ?: -1}")
    }

    fun coverFile(key: String): File = File(covers, "$key.jpg")
    fun sourceFile(key: String, extension: String): File = File(sources, "$key.$extension")
    fun pageDir(key: String): File = File(pages, key).apply { mkdirs() }

    fun clearPages(key: String) {
        File(pages, key).deleteRecursively()
    }

    fun clearCopiedSources() {
        sources.deleteRecursively()
        sources.mkdirs()
    }

    fun trimCopiedSources(maxBytes: Long) {
        if (sources.walkBottomUp().filter { it.isFile }.sumOf { it.length() } <= maxBytes) return
        clearCopiedSources()
    }

    fun saveCover(key: String, bitmap: Bitmap): Uri {
        val file = coverFile(key)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 86, it) }
        return Uri.fromFile(file)
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
