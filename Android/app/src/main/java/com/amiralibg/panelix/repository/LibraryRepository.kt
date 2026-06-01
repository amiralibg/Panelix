package com.amiralibg.panelix.repository

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.amiralibg.panelix.data.AccentColor
import com.amiralibg.panelix.data.AppPreferencesStore
import com.amiralibg.panelix.data.BookmarkDao
import com.amiralibg.panelix.data.BookmarkEntity
import com.amiralibg.panelix.data.ComicDao
import com.amiralibg.panelix.data.ComicEntity
import com.amiralibg.panelix.data.ComicReaderPreferencesDao
import com.amiralibg.panelix.data.ComicReaderPreferencesEntity
import com.amiralibg.panelix.data.FolderDao
import com.amiralibg.panelix.data.FolderEntity
import com.amiralibg.panelix.data.LibraryViewMode
import com.amiralibg.panelix.data.ReaderLayoutMode
import com.amiralibg.panelix.data.ReadingDirection
import com.amiralibg.panelix.data.ReadingProgressDao
import com.amiralibg.panelix.data.ReadingProgressEntity
import com.amiralibg.panelix.data.SortOption
import com.amiralibg.panelix.data.ThemePreference
import com.amiralibg.panelix.parser.ComicSource
import com.amiralibg.panelix.parser.ReaderDocument
import com.amiralibg.panelix.scanner.ComicScanner
import com.amiralibg.panelix.scanner.ScanProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LibraryRepository(
    private val context: Context,
    private val folderDao: FolderDao,
    private val comicDao: ComicDao,
    private val progressDao: ReadingProgressDao,
    private val bookmarkDao: BookmarkDao,
    private val readerPrefsDao: ComicReaderPreferencesDao,
    private val preferencesStore: AppPreferencesStore,
    private val scanner: ComicScanner,
) {
    val folders = folderDao.observeFolders()
    val comics = comicDao.observeComics()
    val continueReading = comicDao.observeContinueReading()
    val readingProgress = progressDao.observeAll()
    val appPreferences = preferencesStore.preferences

    suspend fun addFolder(
        uri: Uri,
        flags: Int,
        onScanProgress: (ScanProgress) -> Unit = {},
    ) = withContext(Dispatchers.IO) {
        val readFlag = flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(
            uri,
            if (readFlag != 0) readFlag else Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        val name = DocumentFile.fromTreeUri(context, uri)?.name ?: uri.lastPathSegment ?: "Folder"
        folderDao.insert(FolderEntity(uri = uri.toString(), name = name))
        preferencesStore.setHasCompletedOnboarding(true)
        rescanFolder(uri.toString(), onScanProgress)
    }

    suspend fun rescanAll(onScanProgress: (ScanProgress) -> Unit = {}) = withContext(Dispatchers.IO) {
        folderDao.getFolders().forEach { rescanFolder(it.uri, onScanProgress) }
    }

    suspend fun rescanFolder(
        folderUri: String,
        onScanProgress: (ScanProgress) -> Unit = {},
    ) = withContext(Dispatchers.IO) {
        if (!hasPermission(folderUri)) {
            comicDao.markFolderUnavailable(folderUri, "Folder permission lost")
            return@withContext
        }
        val existing = comicDao.getByFolder(folderUri).associateBy { it.id }
        comicDao.upsertAll(scanner.scanFolder(folderUri, existing, onScanProgress))
    }

    suspend fun removeFolder(uri: String) = withContext(Dispatchers.IO) {
        folderDao.deleteByUri(uri)
        comicDao.deleteByFolder(uri)
        runCatching { context.contentResolver.releasePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    fun observeComic(id: String): Flow<ComicEntity?> = comicDao.observeComic(id)
    fun observeProgress(comicId: String) = progressDao.observe(comicId)
    suspend fun getProgress(comicId: String) = progressDao.get(comicId)
    fun observeBookmarks(comicId: String) = bookmarkDao.observeForComic(comicId)
    fun observeReaderPreferences(comicId: String) = readerPrefsDao.observe(comicId)

    suspend fun loadReaderDocument(comic: ComicEntity): ReaderDocument = withContext(Dispatchers.IO) {
        comicDao.markOpened(comic.id)
        val source = ComicSource(
            uri = Uri.parse(comic.uri),
            displayName = comic.title,
            format = comic.format,
            folderUri = comic.folderUri,
            fileSize = comic.fileSize,
            modifiedAt = comic.sourceModifiedAt,
            knownPageCount = comic.pageCount,
        )
        scanner.parserForReader(comic.format).loadForReader(source)
    }

    suspend fun saveProgress(comicId: String, page: Int, totalPages: Int) {
        progressDao.upsert(ReadingProgressEntity(comicId, page, totalPages))
    }

    suspend fun toggleBookmark(comicId: String, page: Int) {
        val existing = bookmarkDao.getForPage(comicId, page)
        if (existing != null) bookmarkDao.delete(existing)
        else bookmarkDao.upsert(BookmarkEntity(comicId = comicId, page = page, note = null))
    }

    suspend fun saveReaderPreferences(preferences: ComicReaderPreferencesEntity) = readerPrefsDao.upsert(preferences)

    suspend fun setTheme(value: ThemePreference) = preferencesStore.setTheme(value)
    suspend fun setLibraryViewMode(value: LibraryViewMode) = preferencesStore.setLibraryViewMode(value)
    suspend fun setSortOption(value: SortOption) = preferencesStore.setSortOption(value)
    suspend fun setReaderLayoutMode(value: ReaderLayoutMode) = preferencesStore.setReaderLayoutMode(value)
    suspend fun setReadingDirection(value: ReadingDirection) = preferencesStore.setReadingDirection(value)
    suspend fun setAccentColor(value: AccentColor) = preferencesStore.setAccentColor(value)
    suspend fun setShowProgressOnCovers(value: Boolean) = preferencesStore.setShowProgressOnCovers(value)
    suspend fun setKeepScreenAwake(value: Boolean) = preferencesStore.setKeepScreenAwake(value)

    private fun hasPermission(folderUri: String): Boolean {
        val uri = Uri.parse(folderUri)
        return context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
    }
}
