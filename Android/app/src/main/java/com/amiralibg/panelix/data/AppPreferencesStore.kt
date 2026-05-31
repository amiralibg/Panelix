package com.amiralibg.panelix.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("panelix_app_preferences")

class AppPreferencesStore(private val context: Context) {
    private object Keys {
        val Theme = stringPreferencesKey("theme")
        val ViewMode = stringPreferencesKey("library_view_mode")
        val Sort = stringPreferencesKey("sort")
        val ReaderMode = stringPreferencesKey("reader_mode")
        val Direction = stringPreferencesKey("direction")
        val Onboarding = booleanPreferencesKey("has_completed_onboarding")
    }

    val preferences = context.dataStore.data.map { prefs ->
        AppPreferences(
            themePreference = prefs[Keys.Theme]?.let(ThemePreference::valueOf) ?: ThemePreference.system,
            libraryViewMode = prefs[Keys.ViewMode]?.let(LibraryViewMode::valueOf) ?: LibraryViewMode.grid,
            sortOption = prefs[Keys.Sort]?.let(SortOption::valueOf) ?: SortOption.recentlyAdded,
            readerLayoutMode = prefs[Keys.ReaderMode]?.let(ReaderLayoutMode::valueOf) ?: ReaderLayoutMode.horizontal,
            readingDirection = prefs[Keys.Direction]?.let(ReadingDirection::valueOf) ?: ReadingDirection.ltr,
            hasCompletedOnboarding = prefs[Keys.Onboarding] ?: false,
        )
    }

    suspend fun setTheme(value: ThemePreference) = context.dataStore.edit { it[Keys.Theme] = value.name }
    suspend fun setLibraryViewMode(value: LibraryViewMode) = context.dataStore.edit { it[Keys.ViewMode] = value.name }
    suspend fun setSortOption(value: SortOption) = context.dataStore.edit { it[Keys.Sort] = value.name }
    suspend fun setReaderLayoutMode(value: ReaderLayoutMode) = context.dataStore.edit { it[Keys.ReaderMode] = value.name }
    suspend fun setReadingDirection(value: ReadingDirection) = context.dataStore.edit { it[Keys.Direction] = value.name }
    suspend fun setHasCompletedOnboarding(value: Boolean) = context.dataStore.edit { it[Keys.Onboarding] = value }
}
