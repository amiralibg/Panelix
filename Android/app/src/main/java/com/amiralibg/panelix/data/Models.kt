package com.amiralibg.panelix.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ComicFormat { pdf, cbz, cbr, cbt, cb7, folder, unknown }
enum class ReaderLayoutMode { vertical, horizontal, spread }
enum class ReadingDirection { ltr, rtl }
enum class ThemePreference { system, light, dark }
enum class LibraryViewMode { grid, list, compact }
enum class SortOption { title, recentlyAdded, recentlyOpened }
enum class AccentColor { coral, teal, violet, amber }

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "comics")
data class ComicEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val folderUri: String,
    val title: String,
    val format: ComicFormat,
    val coverUri: String?,
    val pageCount: Int?,
    val fileSize: Long?,
    val addedAt: Long,
    val updatedAt: Long,
    val lastOpenedAt: Long?,
    val isAvailable: Boolean,
    val parserMessage: String?,
    val sourceModifiedAt: Long?,
)

@Entity(tableName = "reading_progress", primaryKeys = ["comicId"])
data class ReadingProgressEntity(
    val comicId: String,
    val currentPage: Int,
    val totalPages: Int,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val comicId: String,
    val page: Int,
    val note: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "comic_reader_preferences", primaryKeys = ["comicId"])
data class ComicReaderPreferencesEntity(
    val comicId: String,
    val readerLayoutMode: ReaderLayoutMode,
    val readingDirection: ReadingDirection,
    val brightness: Float,
    val contrast: Float,
    val updatedAt: Long = System.currentTimeMillis(),
)

data class AppPreferences(
    val themePreference: ThemePreference = ThemePreference.system,
    val libraryViewMode: LibraryViewMode = LibraryViewMode.grid,
    val sortOption: SortOption = SortOption.recentlyAdded,
    val readerLayoutMode: ReaderLayoutMode = ReaderLayoutMode.horizontal,
    val readingDirection: ReadingDirection = ReadingDirection.ltr,
    val accentColor: AccentColor = AccentColor.coral,
    val showProgressOnCovers: Boolean = true,
    val keepScreenAwake: Boolean = true,
    val hasCompletedOnboarding: Boolean = false,
)

data class ComicWithProgress(
    val comic: ComicEntity,
    val progress: ReadingProgressEntity?,
)
