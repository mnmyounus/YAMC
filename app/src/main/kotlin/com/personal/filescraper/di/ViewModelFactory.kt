package com.personal.filescraper.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.personal.filescraper.ui.folders.FolderPickerViewModel
import com.personal.filescraper.ui.gallery.GalleryViewModel
import com.personal.filescraper.ui.home.HomeViewModel
import com.personal.filescraper.ui.settings.SettingsViewModel

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        HomeViewModel::class.java ->
            HomeViewModel(container.archiveRepository, container.settingsRepository) as T
        FolderPickerViewModel::class.java ->
            FolderPickerViewModel(container.archiveRepository, container.settingsRepository) as T
        GalleryViewModel::class.java ->
            GalleryViewModel(container.archiveRepository, container.settingsRepository) as T
        SettingsViewModel::class.java ->
            SettingsViewModel(container.settingsRepository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
