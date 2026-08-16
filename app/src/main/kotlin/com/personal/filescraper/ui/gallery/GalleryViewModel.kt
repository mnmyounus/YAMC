package com.personal.filescraper.ui.gallery

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.filescraper.data.model.FileType
import com.personal.filescraper.data.repository.ArchiveRepository
import com.personal.filescraper.data.repository.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class ArchivedFileUi(
    val id: Long,
    val fileName: String,
    val archivedPath: String,
    val fileType: FileType,
    val timeRemainingMillis: Long
)

class GalleryViewModel(
    private val repository: ArchiveRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    // Re-emits every 30s purely to keep the "time remaining" countdown fresh.
    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(TICK_MILLIS)
        }
    }

    val uiState: StateFlow<List<ArchivedFileUi>> = combine(
        repository.observeArchivedFiles(),
        settingsRepository.retentionHours,
        ticker
    ) { files, retentionHours, now ->
        val retentionMillis = retentionHours * 60L * 60L * 1000L
        files.map { entity ->
            val expiresAt = entity.archivedAtMillis + retentionMillis
            ArchivedFileUi(
                id = entity.id,
                fileName = entity.fileName,
                archivedPath = entity.archivedPath,
                fileType = FileType.valueOf(entity.fileType),
                timeRemainingMillis = (expiresAt - now).coerceAtLeast(0)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun exportAll(context: Context, destinationUri: Uri): Int =
        repository.exportAll(context, destinationUri)

    companion object {
        private const val TICK_MILLIS = 30_000L
    }
}
