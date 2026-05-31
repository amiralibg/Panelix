package com.amiralibg.panelix.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.amiralibg.panelix.repository.LibraryRepository
import com.amiralibg.panelix.ui.library.LibraryViewModel
import com.amiralibg.panelix.ui.reader.ReaderViewModel
import com.amiralibg.panelix.ui.settings.SettingsViewModel

class LibraryViewModelFactory(private val repository: LibraryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            return modelClass.cast(LibraryViewModel(repository))
                ?: throw IllegalArgumentException("Unable to create ${modelClass.name}")
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class SettingsViewModelFactory(private val repository: LibraryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return modelClass.cast(SettingsViewModel(repository))
                ?: throw IllegalArgumentException("Unable to create ${modelClass.name}")
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class ReaderViewModelFactory(
    private val comicId: String,
    private val repository: LibraryRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
            return modelClass.cast(ReaderViewModel(comicId, repository))
                ?: throw IllegalArgumentException("Unable to create ${modelClass.name}")
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
