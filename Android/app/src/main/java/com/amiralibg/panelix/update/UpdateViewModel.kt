package com.amiralibg.panelix.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class UpdateStatus { Idle, Checking, Available, UpToDate, Downloading, ReadyToInstall, Error }

data class UpdateUiState(
    val status: UpdateStatus = UpdateStatus.Idle,
    val info: UpdateInfo? = null,
    val downloadProgress: Float = 0f,
    val message: String? = null,
)

class UpdateViewModel(private val checker: UpdateChecker) : ViewModel() {

    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    val currentVersion: String get() = checker.currentVersion

    private var downloadedFile: File? = null
    private var autoChecked = false

    /** Runs a single silent check the first time the app launches. */
    fun autoCheckOnce() {
        if (autoChecked) return
        autoChecked = true
        check(silent = true)
    }

    fun check(silent: Boolean = false) {
        val status = _state.value.status
        if (status == UpdateStatus.Checking || status == UpdateStatus.Downloading) return
        _state.value = UpdateUiState(status = UpdateStatus.Checking)
        viewModelScope.launch {
            _state.value = when (val result = checker.check()) {
                is UpdateResult.Available -> UpdateUiState(UpdateStatus.Available, info = result.info)
                is UpdateResult.UpToDate ->
                    UpdateUiState(if (silent) UpdateStatus.Idle else UpdateStatus.UpToDate)
                is UpdateResult.Error ->
                    UpdateUiState(if (silent) UpdateStatus.Idle else UpdateStatus.Error, message = result.message)
            }
        }
    }

    fun downloadAndInstall() {
        val info = _state.value.info ?: return
        _state.value = _state.value.copy(status = UpdateStatus.Downloading, downloadProgress = 0f)
        viewModelScope.launch {
            try {
                val file = checker.download(info) { progress ->
                    _state.value = _state.value.copy(downloadProgress = progress)
                }
                downloadedFile = file
                _state.value = _state.value.copy(status = UpdateStatus.ReadyToInstall)
                checker.installApk(file)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    status = UpdateStatus.Error,
                    message = e.message ?: "Download failed",
                )
            }
        }
    }

    /** Re-launches the installer for an already downloaded APK (e.g. after granting permission). */
    fun install() {
        downloadedFile?.let { checker.installApk(it) }
    }

    fun dismiss() {
        _state.value = UpdateUiState()
    }
}
