package com.personal.filescraper.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.personal.filescraper.FileScraperApp
import com.personal.filescraper.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val container = (applicationContext as FileScraperApp).container
        val repo = container.archiveRepository

        // Keeps the default-folder list current even if the app process never
        // restarts (e.g. it's just sitting alive in the background the whole time).
        repo.syncDefaultFolders()

        val monitoringEnabled = container.settingsRepository.monitoringEnabled.first()
        if (!monitoringEnabled) return@withContext Result.success()

        val folders = repo.getWatchedFoldersOnce()
        var newCount = 0

        folders.forEach { folder ->
            val root = File(folder.path)
            if (root.exists()) {
                root.walkTopDown()
                    .maxDepth(SCAN_MAX_DEPTH)
                    .filter {
                        it.isFile &&
                            it.lastModified() >= folder.addedAtMillis &&
                            FileUtils.isSupportedType(it) &&
                            !FileUtils.isLikelyTemporary(it)
                    }
                    .forEach { file ->
                        if (!repo.isAlreadyArchived(file.absolutePath)) {
                            if (repo.archiveFile(file, folder.path) != null) newCount++
                        }
                    }
            }
        }

        if (newCount > 0) {
            val message = if (newCount == 1) "1 file archived" else "$newCount files archived"
            container.notificationHelper.showFileArchivedNotification(message)
        }
        Result.success()
    }

    companion object {
        private const val SCAN_MAX_DEPTH = 12
    }
}
