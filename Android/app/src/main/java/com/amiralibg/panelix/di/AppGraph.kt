package com.amiralibg.panelix.di

import android.content.Context
import androidx.room.Room
import com.amiralibg.panelix.cache.ComicCacheManager
import com.amiralibg.panelix.data.AppDatabase
import com.amiralibg.panelix.data.AppPreferencesStore
import com.amiralibg.panelix.parser.CbzComicParser
import com.amiralibg.panelix.parser.CbrComicParser
import com.amiralibg.panelix.parser.ImageFolderComicParser
import com.amiralibg.panelix.parser.PdfComicParser
import com.amiralibg.panelix.repository.LibraryRepository
import com.amiralibg.panelix.scanner.ComicScanner
import com.amiralibg.panelix.update.UpdateChecker

class AppGraph(context: Context) {
    private val appContext = context.applicationContext
    val updateChecker = UpdateChecker(appContext)
    private val cache = ComicCacheManager(appContext)
        .also { it.trimCopiedSources(maxBytes = 512L * 1024L * 1024L) }
    private val database = Room.databaseBuilder(appContext, AppDatabase::class.java, "panelix.db").build()
    private val pdfParser = PdfComicParser(appContext.contentResolver, cache)
    private val cbzParser = CbzComicParser(appContext.contentResolver, cache)
    private val cbrParser = CbrComicParser(appContext.contentResolver, cache)
    private val imageFolderParser = ImageFolderComicParser(appContext, cache)
    private val scanner = ComicScanner(appContext, pdfParser, cbzParser, cbrParser, imageFolderParser)

    val repository = LibraryRepository(
        context = appContext,
        folderDao = database.folderDao(),
        comicDao = database.comicDao(),
        progressDao = database.progressDao(),
        bookmarkDao = database.bookmarkDao(),
        readerPrefsDao = database.readerPreferencesDao(),
        preferencesStore = AppPreferencesStore(appContext),
        scanner = scanner,
    )
}
