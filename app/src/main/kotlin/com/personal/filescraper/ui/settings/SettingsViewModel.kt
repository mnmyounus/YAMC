package com.personal.filescraper.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.filescraper.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    val retentionHours: StateFlow<Int> = settingsRepository.retentionHours
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_RETENTION_HOURS)

    fun setRetentionHours(hours: Int) {
        viewModelScope.launch { settingsRepository.setRetentionHours(hours) }
    }
}
