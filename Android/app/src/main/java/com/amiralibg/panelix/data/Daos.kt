package com.amiralibg.panelix.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY createdAt DESC")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY createdAt DESC")
    suspend fun getFolders(): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)
}

@Dao
interface ComicDao {
    @Query("SELECT * FROM comics ORDER BY addedAt DESC")
    fun observeComics(): Flow<List<ComicEntity>>

    @Query("SELECT * FROM comics WHERE id = :id")
    fun observeComic(id: String): Flow<ComicEntity?>

    @Query("SELECT * FROM comics WHERE id = :id")
    suspend fun getComic(id: String): ComicEntity?

    @Query("SELECT * FROM comics WHERE folderUri = :folderUri")
    suspend fun getByFolder(folderUri: String): List<ComicEntity>

    @Upsert
    suspend fun upsert(comic: ComicEntity)

    @Upsert
    suspend fun upsertAll(comics: List<ComicEntity>)

    @Query("UPDATE comics SET isAvailable = 0, parserMessage = :message, updatedAt = :updatedAt WHERE folderUri = :folderUri")
    suspend fun markFolderUnavailable(folderUri: String, message: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE comics SET lastOpenedAt = :openedAt WHERE id = :id")
    suspend fun markOpened(id: String, openedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM comics WHERE folderUri = :folderUri")
    suspend fun deleteByFolder(folderUri: String)

    @Transaction
    @Query(
        "SELECT * FROM comics WHERE lastOpenedAt IS NOT NULL " +
            "ORDER BY lastOpenedAt DESC LIMIT 12"
    )
    fun observeContinueReading(): Flow<List<ComicEntity>>
}

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress WHERE comicId = :comicId")
    fun observe(comicId: String): Flow<ReadingProgressEntity?>

    @Query("SELECT * FROM reading_progress WHERE comicId = :comicId")
    suspend fun get(comicId: String): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress")
    fun observeAll(): Flow<List<ReadingProgressEntity>>

    @Upsert
    suspend fun upsert(progress: ReadingProgressEntity)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE comicId = :comicId ORDER BY page ASC")
    fun observeForComic(comicId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE comicId = :comicId AND page = :page LIMIT 1")
    suspend fun getForPage(comicId: String, page: Int): BookmarkEntity?

    @Upsert
    suspend fun upsert(bookmark: BookmarkEntity)

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)
}

@Dao
interface ComicReaderPreferencesDao {
    @Query("SELECT * FROM comic_reader_preferences WHERE comicId = :comicId")
    fun observe(comicId: String): Flow<ComicReaderPreferencesEntity?>

    @Query("SELECT * FROM comic_reader_preferences WHERE comicId = :comicId")
    suspend fun get(comicId: String): ComicReaderPreferencesEntity?

    @Upsert
    suspend fun upsert(preferences: ComicReaderPreferencesEntity)
}
