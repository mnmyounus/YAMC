package com.personal.filescraper.ui.home

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.filescraper.data.repository.ArchiveRepository
import com.personal.filescraper.data.repository.SettingsRepository
import com.personal.filescraper.monitor.FileMonitorService
import com.personal.filescraper.worker.WorkerScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: ArchiveRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val monitoringEnabled: StateFlow<Boolean> = settingsRepository.monitoringEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val watchedFolderCount: StateFlow<Int> = repository.observeWatchedFolders()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setMonitoring(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            settingsRepository.setMonitoringEnabled(enabled)
            if (enabled) {
                ContextCompat.startForegroundService(context, Intent(context, FileMonitorService::class.java))
                WorkerScheduler.scheduleSync(context)
            } else {
                context.startService(
                    Intent(context, FileMonitorService::class.java).apply { action = FileMonitorService.ACTION_STOP }
                )
                WorkerScheduler.cancelSync(context)
            }
        }
    }
}
