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

/**
 * Safety-net reconciliation. FileObserver only reports events while the monitoring
 * service's process is alive, and Android can kill that process - Doze, OEM battery
 * managers, and (on Android 14) the background time limit on dataSync foreground
 * services all do this. This worker periodically re-scans watched folders and
 * archives anything that was missed, so monitoring stays eventually-consistent even
 * after the service itself has been killed.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val container = (applicationContext as FileScraperApp).container
        val monitoringEnabled = container.settingsRepository.monitoringEnabled.first()
        if (!monitoringEnabled) return@withContext Result.success()

        val repo = container.archiveRepository
        val folders = repo.getWatchedFoldersOnce()
        var newCount = 0

        folders.forEach { folder ->
            val root = File(folder.path)
            if (root.exists()) {
                root.walkTopDown()
                    .maxDepth(SCAN_MAX_DEPTH)
                    .filter { it.isFile && FileUtils.isSupportedType(it) && !FileUtils.isLikelyTemporary(it) }
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
