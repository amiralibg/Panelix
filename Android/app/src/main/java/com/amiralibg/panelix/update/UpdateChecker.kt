package com.amiralibg.panelix.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Metadata about a newer release found on GitHub. */
data class UpdateInfo(
    val versionName: String,
    val tagName: String,
    val releaseNotes: String,
    val apkUrl: String,
    val apkSize: Long,
)

sealed interface UpdateResult {
    data class Available(val info: UpdateInfo) : UpdateResult
    data object UpToDate : UpdateResult
    data class Error(val message: String) : UpdateResult
}

/**
 * Checks GitHub Releases for a newer build, downloads the signed APK and hands it
 * to the system package installer. The downloaded APK must be signed with the same
 * key as the installed app, otherwise Android rejects the update.
 */
class UpdateChecker(private val context: Context) {

    private val latestReleaseApi =
        "https://api.github.com/repos/$REPO/releases/latest"

    val currentVersion: String
        get() = runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "0.0.0"

    suspend fun check(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val body = httpGet(latestReleaseApi)
            val release = JSONObject(body)
            val tag = release.getString("tag_name")
            val remoteVersion = tag.trimStart('v', 'V')
            val notes = release.optString("body", "")

            val assets = release.getJSONArray("assets")
            var apkUrl: String? = null
            var apkSize = 0L
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.getString("browser_download_url")
                    apkSize = asset.optLong("size", 0L)
                    break
                }
            }

            when {
                apkUrl == null -> UpdateResult.Error("Latest release has no APK asset")
                isNewer(remoteVersion, currentVersion) ->
                    UpdateResult.Available(UpdateInfo(remoteVersion, tag, notes, apkUrl, apkSize))
                else -> UpdateResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "Update check failed")
        }
    }

    /** Downloads the APK into the cache, reporting progress in the range 0f..1f. */
    suspend fun download(info: UpdateInfo, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val out = File(dir, "Panelix-${info.tagName}.apk")

            val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15_000
                readTimeout = 30_000
            }
            try {
                val total = if (info.apkSize > 0) info.apkSize else conn.contentLengthLong
                conn.inputStream.use { input ->
                    out.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var downloaded = 0L
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) onProgress((downloaded.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            } finally {
                conn.disconnect()
            }
            out
        }

    /**
     * Launches the system installer for [file]. On Android O+ the app must hold the
     * "install unknown apps" grant first; if missing, this opens that settings screen
     * instead and returns false so the caller can ask the user to retry.
     */
    fun installApk(file: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settings = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settings)
            return false
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return true
    }

    private fun httpGet(urlStr: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            if (conn.responseCode !in 200..299) {
                throw java.io.IOException("HTTP ${conn.responseCode}")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    /** Compares dotted version strings numerically, ignoring any "-suffix". */
    private fun isNewer(remote: String, current: String): Boolean {
        val r = parseVersion(remote)
        val c = parseVersion(current)
        for (i in 0 until maxOf(r.size, c.size)) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv != cv) return rv > cv
        }
        return false
    }

    private fun parseVersion(v: String): List<Int> =
        v.substringBefore('-').split('.').mapNotNull { it.trim().toIntOrNull() }

    private companion object {
        const val REPO = "amiralibg/Panelix"
    }
}
