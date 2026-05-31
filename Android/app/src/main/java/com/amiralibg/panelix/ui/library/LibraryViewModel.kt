package com.amiralibg.panelix.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amiralibg.panelix.data.AppPreferences
import com.amiralibg.panelix.data.ComicEntity
import com.amiralibg.panelix.data.ComicFormat
import com.amiralibg.panelix.data.LibraryViewMode
import com.amiralibg.panelix.data.SortOption
import com.amiralibg.panelix.repository.LibraryRepository
import com.amiralibg.panelix.scanner.ScanProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val comics: List<ComicEntity> = emptyList(),
    val continueReading: List<ComicEntity> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
    val query: String = "",
    val filter: ComicFormat? = null,
    val isScanning: Boolean = false,
    val scanProgress: ScanUiProgress? = null,
    val error: String? = null,
) {
    val visibleComics: List<ComicEntity>
        get() = comics.asSequence()
            .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }
            .filter { filter == null || it.format == filter }
            .let { seq ->
                when (preferences.sortOption) {
                    SortOption.title -> seq.sortedBy { it.title.lowercase() }
                    SortOption.recentlyAdded -> seq.sortedByDescending { it.addedAt }
                    SortOption.recentlyOpened -> seq.sortedByDescending { it.lastOpenedAt ?: 0L }
                }
            }.toList()
}

data class ScanUiProgress(
    val completed: Int,
    val total: Int,
    val currentTitle: String?,
    val estimatedRemainingMillis: Long?,
) {
    val fraction: Float
        get() = if (total <= 0) 0f else (completed.toFloat() / total).coerceIn(0f, 1f)
}

class LibraryViewModel(private val repository: LibraryRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<ComicFormat?>(null)
    private val scanning = MutableStateFlow(false)
    private val scanProgress = MutableStateFlow<ScanUiProgress?>(null)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<LibraryUiState> = combine(
        repository.comics,
        repository.continueReading,
        repository.appPreferences,
        query,
        filter,
        scanning,
        scanProgress,
        error,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        LibraryUiState(
            comics = values[0] as List<ComicEntity>,
            continueReading = values[1] as List<ComicEntity>,
            preferences = values[2] as AppPreferences,
            query = values[3] as String,
            filter = values[4] as ComicFormat?,
            isScanning = values[5] as Boolean,
            scanProgress = values[6] as ScanUiProgress?,
            error = values[7] as String?,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun setQuery(value: String) { query.value = value }
    fun setFilter(value: ComicFormat?) { filter.value = value }
    fun setSort(value: SortOption) = viewModelScope.launch { repository.setSortOption(value) }
    fun setViewMode(value: LibraryViewMode) = viewModelScope.launch { repository.setLibraryViewMode(value) }

    fun addFolder(uri: Uri, flags: Int) = viewModelScope.launch {
        runWithScanState { progress -> repository.addFolder(uri, flags, progress) }
    }

    fun rescan() = viewModelScope.launch {
        runWithScanState { progress -> repository.rescanAll(progress) }
    }

    private suspend fun runWithScanState(block: suspend ((ScanProgress) -> Unit) -> Unit) {
        scanning.value = true
        scanProgress.value = ScanUiProgress(0, 0, "Counting comics", null)
        error.value = null
        val startedAt = System.currentTimeMillis()
        val progressCallback: (ScanProgress) -> Unit = { progress ->
            val elapsed = System.currentTimeMillis() - startedAt
            val eta = if (progress.completed > 0 && progress.total > progress.completed) {
                ((elapsed.toDouble() / progress.completed) * (progress.total - progress.completed)).toLong()
            } else {
                null
            }
            scanProgress.value = ScanUiProgress(
                completed = progress.completed,
                total = progress.total,
                currentTitle = progress.currentTitle,
                estimatedRemainingMillis = eta,
            )
        }
        runCatching { block(progressCallback) }.onFailure { error.value = it.message ?: "Scan failed" }
        scanning.value = false
        scanProgress.value = null
    }
}
