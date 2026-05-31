package com.amiralibg.panelix.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amiralibg.panelix.data.AppPreferences
import com.amiralibg.panelix.data.BookmarkEntity
import com.amiralibg.panelix.data.ComicEntity
import com.amiralibg.panelix.data.ComicReaderPreferencesEntity
import com.amiralibg.panelix.data.ReaderLayoutMode
import com.amiralibg.panelix.data.ReadingDirection
import com.amiralibg.panelix.data.ReadingProgressEntity
import com.amiralibg.panelix.parser.ReaderPage
import com.amiralibg.panelix.repository.LibraryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReaderUiState(
    val comic: ComicEntity? = null,
    val pages: List<ReaderPage> = emptyList(),
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val progress: ReadingProgressEntity? = null,
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val appPreferences: AppPreferences = AppPreferences(),
    val comicPreferences: ComicReaderPreferencesEntity? = null,
    val isLoading: Boolean = true,
    val parserMessage: String? = null,
) {
    val readerMode: ReaderLayoutMode get() = comicPreferences?.readerLayoutMode ?: appPreferences.readerLayoutMode
    val direction: ReadingDirection get() = comicPreferences?.readingDirection ?: appPreferences.readingDirection
    val brightness: Float get() = comicPreferences?.brightness ?: 0f
    val contrast: Float get() = comicPreferences?.contrast ?: 1f
}

class ReaderViewModel(
    private val comicId: String,
    private val repository: LibraryRepository,
) : ViewModel() {
    private val pages = MutableStateFlow<List<ReaderPage>>(emptyList())
    private val pageCount = MutableStateFlow(0)
    private val currentPage = MutableStateFlow(0)
    private val loading = MutableStateFlow(true)
    private val message = MutableStateFlow<String?>(null)
    private var loader: Job? = null

    val state: StateFlow<ReaderUiState> = combine(
        repository.observeComic(comicId),
        pages,
        pageCount,
        currentPage,
        repository.observeProgress(comicId),
        repository.observeBookmarks(comicId),
        repository.appPreferences,
        repository.observeReaderPreferences(comicId),
        loading,
        message,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        ReaderUiState(
            comic = values[0] as ComicEntity?,
            pages = values[1] as List<ReaderPage>,
            pageCount = values[2] as Int,
            currentPage = values[3] as Int,
            progress = values[4] as ReadingProgressEntity?,
            bookmarks = values[5] as List<BookmarkEntity>,
            appPreferences = values[6] as AppPreferences,
            comicPreferences = values[7] as ComicReaderPreferencesEntity?,
            isLoading = values[8] as Boolean,
            parserMessage = values[9] as String?,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState())

    init {
        loader = viewModelScope.launch {
            repository.observeComic(comicId).collect { comic ->
                if (comic != null && pages.value.isEmpty()) load(comic)
            }
        }
    }

    fun setCurrentPage(page: Int) {
        currentPage.value = page.coerceIn(0, (pageCount.value - 1).coerceAtLeast(0))
        viewModelScope.launch { repository.saveProgress(comicId, currentPage.value, pageCount.value) }
    }

    fun toggleBookmark() = viewModelScope.launch { repository.toggleBookmark(comicId, currentPage.value) }

    fun saveReaderPrefs(mode: ReaderLayoutMode, direction: ReadingDirection, brightness: Float, contrast: Float) {
        viewModelScope.launch {
            repository.saveReaderPreferences(
                ComicReaderPreferencesEntity(comicId, mode, direction, brightness, contrast)
            )
        }
    }

    private suspend fun load(comic: ComicEntity) {
        loading.value = true
        val document = repository.loadReaderDocument(comic)
        pageCount.value = document.pageCount
        message.value = document.parserMessage
        currentPage.value = repository.getProgress(comicId)?.currentPage ?: 0
        loading.value = false
        document.pages
            .onCompletion {
                if (pages.value.isEmpty() && message.value == null) message.value = "Comic could not be prepared"
            }
            .collect {
                pages.value = it
                if (it.size > pageCount.value) pageCount.value = it.size
            }
    }
}
