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
        val Accent = stringPreferencesKey("accent_color")
        val ShowProgress = booleanPreferencesKey("show_progress_on_covers")
        val KeepAwake = booleanPreferencesKey("keep_screen_awake")
        val Onboarding = booleanPreferencesKey("has_completed_onboarding")
    }

    val preferences = context.dataStore.data.map { prefs ->
        AppPreferences(
            themePreference = prefs[Keys.Theme]?.let(ThemePreference::valueOf) ?: ThemePreference.system,
            libraryViewMode = prefs[Keys.ViewMode]?.let { runCatching { LibraryViewMode.valueOf(it) }.getOrNull() } ?: LibraryViewMode.grid,
            sortOption = prefs[Keys.Sort]?.let(SortOption::valueOf) ?: SortOption.recentlyAdded,
            readerLayoutMode = prefs[Keys.ReaderMode]?.let(ReaderLayoutMode::valueOf) ?: ReaderLayoutMode.horizontal,
            readingDirection = prefs[Keys.Direction]?.let(ReadingDirection::valueOf) ?: ReadingDirection.ltr,
            accentColor = prefs[Keys.Accent]?.let { runCatching { AccentColor.valueOf(it) }.getOrNull() } ?: AccentColor.coral,
            showProgressOnCovers = prefs[Keys.ShowProgress] ?: true,
            keepScreenAwake = prefs[Keys.KeepAwake] ?: true,
            hasCompletedOnboarding = prefs[Keys.Onboarding] ?: false,
        )
    }

    suspend fun setTheme(value: ThemePreference) = context.dataStore.edit { it[Keys.Theme] = value.name }
    suspend fun setLibraryViewMode(value: LibraryViewMode) = context.dataStore.edit { it[Keys.ViewMode] = value.name }
    suspend fun setSortOption(value: SortOption) = context.dataStore.edit { it[Keys.Sort] = value.name }
    suspend fun setReaderLayoutMode(value: ReaderLayoutMode) = context.dataStore.edit { it[Keys.ReaderMode] = value.name }
    suspend fun setReadingDirection(value: ReadingDirection) = context.dataStore.edit { it[Keys.Direction] = value.name }
    suspend fun setAccentColor(value: AccentColor) = context.dataStore.edit { it[Keys.Accent] = value.name }
    suspend fun setShowProgressOnCovers(value: Boolean) = context.dataStore.edit { it[Keys.ShowProgress] = value }
    suspend fun setKeepScreenAwake(value: Boolean) = context.dataStore.edit { it[Keys.KeepAwake] = value }
    suspend fun setHasCompletedOnboarding(value: Boolean) = context.dataStore.edit { it[Keys.Onboarding] = value }
}
