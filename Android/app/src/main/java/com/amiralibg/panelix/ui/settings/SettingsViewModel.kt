package com.amiralibg.panelix.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amiralibg.panelix.data.AccentColor
import com.amiralibg.panelix.data.AppPreferences
import com.amiralibg.panelix.data.FolderEntity
import com.amiralibg.panelix.data.ReaderLayoutMode
import com.amiralibg.panelix.data.ReadingDirection
import com.amiralibg.panelix.data.ThemePreference
import com.amiralibg.panelix.repository.LibraryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: AppPreferences = AppPreferences(),
    val folders: List<FolderEntity> = emptyList(),
)

class SettingsViewModel(private val repository: LibraryRepository) : ViewModel() {
    val state: StateFlow<SettingsUiState> = combine(repository.appPreferences, repository.folders) { preferences, folders ->
        SettingsUiState(preferences, folders)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun addFolder(uri: Uri, flags: Int) = viewModelScope.launch { repository.addFolder(uri, flags) }
    fun removeFolder(uri: String) = viewModelScope.launch { repository.removeFolder(uri) }
    fun rescan() = viewModelScope.launch { repository.rescanAll() }
    fun setTheme(value: ThemePreference) = viewModelScope.launch { repository.setTheme(value) }
    fun setReaderMode(value: ReaderLayoutMode) = viewModelScope.launch { repository.setReaderLayoutMode(value) }
    fun setDirection(value: ReadingDirection) = viewModelScope.launch { repository.setReadingDirection(value) }
    fun setAccent(value: AccentColor) = viewModelScope.launch { repository.setAccentColor(value) }
    fun setShowProgress(value: Boolean) = viewModelScope.launch { repository.setShowProgressOnCovers(value) }
    fun setKeepAwake(value: Boolean) = viewModelScope.launch { repository.setKeepScreenAwake(value) }
}
