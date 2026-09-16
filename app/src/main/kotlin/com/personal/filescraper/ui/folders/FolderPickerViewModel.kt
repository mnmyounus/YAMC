package com.personal.filescraper.ui.folders

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.filescraper.data.db.WatchedFolderEntity
import com.personal.filescraper.data.repository.ArchiveRepository
import com.personal.filescraper.data.repository.SettingsRepository
import com.personal.filescraper.monitor.FileMonitorService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class FolderPickerViewModel(
    private val repository: ArchiveRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val folders: StateFlow<List<WatchedFolderEntity>> = repository.observeWatchedFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addFolder(path: String, context: Context) {
        viewModelScope.launch {
            repository.addWatchedFolder(path, File(path).name, isDefault = false)
            refreshServiceIfActive(context)
        }
    }

    // isDefault is passed through so a removed default folder gets marked "dismissed"
    // and won't be silently re-added by the next default-folder sync.
    fun removeFolder(path: String, isDefault: Boolean, context: Context) {
        viewModelScope.launch {
            repository.removeWatchedFolder(path)
            if (isDefault) {
                repository.dismissDefaultFolder(path)
            }
            refreshServiceIfActive(context)
        }
    }

    fun renameFolder(path: String, newDisplayName: String) {
        viewModelScope.launch {
            repository.renameWatchedFolder(path, newDisplayName)
        }
    }

    private suspend fun refreshServiceIfActive(context: Context) {
        if (settingsRepository.monitoringEnabled.first()) {
            context.startService(
                Intent(context, FileMonitorService::class.java).apply {
                    action = FileMonitorService.ACTION_REFRESH
                }
            )
        }
    }
}
