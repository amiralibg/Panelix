package com.amiralibg.panelix.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class PanelixConverters {
    @TypeConverter fun toComicFormat(value: String) = ComicFormat.valueOf(value)
    @TypeConverter fun fromComicFormat(value: ComicFormat) = value.name
    @TypeConverter fun toReaderLayoutMode(value: String) = ReaderLayoutMode.valueOf(value)
    @TypeConverter fun fromReaderLayoutMode(value: ReaderLayoutMode) = value.name
    @TypeConverter fun toReadingDirection(value: String) = ReadingDirection.valueOf(value)
    @TypeConverter fun fromReadingDirection(value: ReadingDirection) = value.name
}

@Database(
    entities = [
        FolderEntity::class,
        ComicEntity::class,
        ReadingProgressEntity::class,
        BookmarkEntity::class,
        ComicReaderPreferencesEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(PanelixConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun comicDao(): ComicDao
    abstract fun progressDao(): ReadingProgressDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun readerPreferencesDao(): ComicReaderPreferencesDao
}
